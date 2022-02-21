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

package migratedb.maven;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.Unit;
import migratedb.scanner.PathTarget;
import migratedb.scanner.Scanner;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Scans the output of this project (and optionally its dependencies) for database migration resources, e.g. scripts, so
 * they can be found as a 'classpath:' location.
 * <p>
 * MigrateDB looks for the scan results in <code>db/migration</code> unless you tweak its configuration.
 * </p>
 */
@Mojo(name = "scan",
      defaultPhase = LifecyclePhase.PROCESS_CLASSES,
      threadSafe = true,
      requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
@SuppressWarnings("HtmlTagCanBeJavadocTag") // {@code } tags confuse the maven-plugin-plugin
public class ScanMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true)
    public MavenProject project;

    /**
     * Resource name prefixes to include in the scan. To include package <code>com.foo.bar</code> you specify
     * <code>com/foo/bar</code> here.
     * The separator is always a forward slash and does not depend on the file system implementation.
     * <p>
     * The default value corresponds to the default migration directory/package: <code>"db/migration"</code></p>
     */
    @Parameter
    public @Nullable String[] includes = { "db/migration" };

    /**
     * Whether the dependencies of this project should be included in the scan. Otherwise only the direct output is
     * scanned.
     */
    @Parameter(defaultValue = "false")
    public boolean includeDependencies = false;

    /**
     * Whether the build will fail if the class path contains an unsupported element (such as .war or .ear files). When
     * set to <code>false</code> a warning is logged instead.
     */
    @Parameter(defaultValue = "true")
    public boolean failBuildOnUnprocessableClassPathElement = true;

    /**
     * Whether the scan will follow symlinks, which can lead to an exception when symlinks create a cycle, e.g., by
     * pointing to one of their parent directories.
     */
    @Parameter(defaultValue = "false")
    public boolean followSymlinks = false;

    /**
     * The directory where scan results are placed. The default value corresponds to the default
     * <code>"migratedb.location"</code> configuration (<code>"db/migration"</code>).
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/db/migration")
    public File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        var classPath = getProjectClassPath();
        var unprocessableClassPathElements = new ArrayList<Path>();
        var scanner = new Scanner(unprocessable -> {
            unprocessableClassPathElements.add(unprocessable);
            return Unit.INSTANCE;
        });
        var scanResult = scanner.scan(crateScannerConfig(classPath));
        getLog().info("Found " + scanResult.getFoundClasses().size() + " classes, " +
                      scanResult.getFoundResources().size() + " resources");
        var target = new PathTarget(outputDirectory.toPath(), true);
        try {
            scanResult.writeTo(target);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to write scan result to " + target, e);
        }
        handle(unprocessableClassPathElements);
    }

    private Scanner.Config crateScannerConfig(List<Path> classPath) {
        return new Scanner.Config(
            classPath,
            Arrays.asList(includes),
            it -> true,
            followSymlinks
        );
    }

    private void handle(List<Path> unprocessableClassPathElements) throws MojoFailureException {
        if (!unprocessableClassPathElements.isEmpty()) {
            var message = new StringBuilder("The project class path contains elements that couldn't be scanned:\n");
            unprocessableClassPathElements.forEach(it -> message.append(it).append("\n"));
            if (failBuildOnUnprocessableClassPathElement) {
                throw new MojoFailureException(message.toString());
            } else {
                getLog().warn(message);
            }
        }
    }

    private List<Path> getProjectClassPath() throws MojoExecutionException {
        try {
            return (includeDependencies ? project.getRuntimeClasspathElements()
                                        : List.of(project.getBuild().getOutputDirectory()))
                .stream()
                .map(Paths::get)
                .collect(Collectors.toList());
        } catch (InvalidPathException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Failed to get project class path", e);
        }
    }
}
