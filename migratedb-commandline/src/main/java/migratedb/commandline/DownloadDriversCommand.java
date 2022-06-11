/*
 * Copyright 2022 The MigrateDB contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package migratedb.commandline;

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.logging.Log;
import migratedb.core.api.output.OperationResult;
import migratedb.core.internal.info.BuildInfo;
import migratedb.core.internal.util.StringUtils;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class DownloadDriversCommand {
    private final DriverDefinitions driverDefinitions;
    private final Set<String> driversToDownload;
    private final Path targetDir;
    private final HttpClient httpClient;

    DownloadDriversCommand(DriverDefinitions driverDefinitions, Path targetDir, Collection<String> driversToDownload) {
        this.driverDefinitions = driverDefinitions;
        this.driversToDownload = Set.copyOf(driversToDownload);
        this.targetDir = targetDir;
        this.httpClient = HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_1_1)
                                    .connectTimeout(Duration.ofSeconds(10))
                                    .build();
    }

    Result run() {
        resolvePlaceholders();
        var downloadedDrivers = driversToDownload.isEmpty() ? allSupportedDriverAliases() : driversToDownload;
        var driverDefinitions = downloadedDrivers.stream()
                                                 .map(this::findDriverDefinition)
                                                 .collect(Collectors.toList());
        for (var driverDefinition : driverDefinitions) {
            LOG.info("Processing artifacts for '" + driverDefinition.name + "'");
            downloadArtifacts(this.driverDefinitions.repo, driverDefinition);
        }
        LOG.info("Successfully downloaded " + downloadedDrivers.size() + " drivers");
        return new Result(downloadedDrivers);
    }

    private void downloadArtifacts(String repo, DriverDefinition driverDefinition) {
        for (var artifact : driverDefinition.artifacts) {
            var coordinates = new JarCoordinates(artifact.m2);
            var targetFile = targetDir.resolve(coordinates.targetFileName());
            if (Files.isRegularFile(targetFile)) {
                LOG.info("Already exists: '" + targetFile.getFileName() + "'");
                try {
                    validate(targetFile, artifact.sha256);
                } catch (RuntimeException e) {
                    LOG.info("Please delete '" + targetFile.getFileName() + "' and try again");
                    throw e;
                }
            } else {
                downloadFromRemoteRepository(repo, artifact, coordinates, targetFile);
            }
        }
    }

    private void downloadFromRemoteRepository(String repo,
                                              ArtifactDefinition artifact,
                                              JarCoordinates coordinates,
                                              Path targetFile) {
        var tempFile = targetFile.resolveSibling(targetFile.getFileName() + ".tmp");
        try {
            Files.deleteIfExists(tempFile);
            httpGet(coordinates.toUrl(repo), tempFile, artifact.sha256);
            try {
                Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (URISyntaxException e) {
            throw new MigrateDbException("Invalid download URL for artifact '" + artifact.m2 + "'", e);
        } catch (IOException e) {
            throw new MigrateDbException("Unable to fetch driver artifact '" + artifact.m2 + "'", e);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                LOG.warn("Failed to remove temporary file '" + tempFile + "'");
            }
        }
    }

    private NavigableSet<String> allSupportedDriverAliases() {
        return driverDefinitions.drivers.stream()
                                        .map(it -> it.alias)
                                        .collect(Collectors.toCollection(TreeSet::new));
    }

    private void httpGet(URI url, Path toFile, String sha256) throws IOException {
        LOG.info("Downloading '" + url + "'");
        try {
            var request = HttpRequest.newBuilder(url)
                                     .GET()
                                     .timeout(Duration.ofMinutes(10))
                                     .build();
            if (toFile.getParent() != null) {
                Files.createDirectories(toFile.getParent());
            }
            var response = httpClient.send(request, BodyHandlers.ofFile(toFile));
            if (response.statusCode() != 200) {
                throw new MigrateDbException("GET request to '" + url +
                                             "' failed with status code " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MigrateDbException("Interrupted");
        }
        validate(toFile, sha256);
    }

    private void validate(Path targetFile, String sha256) {
        LOG.info("Validating '" + targetFile.getFileName() + "'");
        var digest = newSha256();
        var buf = new byte[4096];
        try (var stream = new DigestInputStream(Files.newInputStream(targetFile), digest)) {
            while (stream.read(buf) > 0) {
                // keep updating the digest
            }
        } catch (IOException e) {
            throw new MigrateDbException("Failed to validate '" + targetFile + "'", e);
        }
        try {
            var actual = digest.digest();
            var expected = Hex.decodeHex(sha256);
            if (!Arrays.equals(expected, actual)) {
                throw new MigrateDbException(
                    "Hash mismatch for file '" + targetFile + "'\n" +
                    "   Actual:   '" + Hex.encodeHexString(actual) + "'\n" +
                    "   Expected: '" + sha256 + "'\n" +
                    "Please try again. If the error persists, the remote driver file may have been corrupted.");
            }
        } catch (DecoderException e) {
            throw new MigrateDbException("Not a hexadecimal value: '" + sha256 + "'");
        }
    }

    private DriverDefinition findDriverDefinition(String alias) {
        return driverDefinitions.drivers.stream()
                                        .filter(it -> it.alias.equalsIgnoreCase(alias))
                                        .findFirst()
                                        .orElseThrow(() -> new MigrateDbException(
                                            "No such driver '" + alias + "'. The available drivers are: "
                                            + allSupportedDriverAliases()));
    }

    private MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(); // Unreachable - every JVM must support SHA-256
        }
    }

    private void resolvePlaceholders() {
        var subst = new StringSubstitutor(driverDefinitions.properties, "${", "}");
        subst.setEnableUndefinedVariableException(true);
        driverDefinitions.repo = subst.replace(driverDefinitions.repo);
        for (var driver : driverDefinitions.drivers) {
            driver.name = subst.replace(driver.name);
            for (var artifact : driver.artifacts) {
                artifact.m2 = subst.replace(artifact.m2);
            }
        }
    }

    public static final class Result extends OperationResult {
        public Set<String> downloadedDrivers;

        Result(Set<String> downloadedDrivers) {
            this.downloadedDrivers = Set.copyOf(downloadedDrivers);
            this.migratedbVersion = BuildInfo.VERSION;
            this.operation = "download-drivers";
        }
    }

    public static class DriverDefinitions {
        public Map<String, String> properties;
        public List<DriverDefinition> drivers;
        public String repo;
    }

    public static class ArtifactDefinition {
        public String m2;
        public String sha256;
    }

    public static class DriverDefinition {
        public String name;
        public String alias;
        public List<ArtifactDefinition> artifacts;
    }

    static class JarCoordinates {
        public final String groupId;
        public final String artifactId;
        public final String version;
        public final String extension = "jar";

        JarCoordinates(String coordinates) {
            var groupArtifactVersion = StringUtils.tokenizeToStringArray(coordinates, ":");
            if (groupArtifactVersion.length != 3) {
                throw new MigrateDbException("Unsupported artifact coordinates: '" + coordinates + "'");
            }
            groupId = groupArtifactVersion[0];
            artifactId = groupArtifactVersion[1];
            version = groupArtifactVersion[2];
        }

        String targetFileName() {
            return groupId + "." + artifactId + "-" + version + "." + extension;
        }

        URI toUrl(String repo) throws URISyntaxException {
            var repoUrl = new URI(repo);

            String newPath = repoUrl.getPath() +
                             (repoUrl.getPath().endsWith("/") ? "" : "/") +
                             groupId.replace('.', '/') +
                             "/" + artifactId +
                             "/" + version +
                             "/" + artifactId + "-" + version + "." + extension;
            return new URI(repoUrl.getScheme(),
                           repoUrl.getUserInfo(),
                           repoUrl.getHost(),
                           repoUrl.getPort(),
                           newPath,
                           null,
                           null);
        }
    }

    private static final Log LOG = Log.getLog(DownloadDriversCommand.class);
}
