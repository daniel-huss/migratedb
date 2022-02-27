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

package migratedb.gradle;

import static migratedb.gradle.MigrateDbScanConfig.Defaults.DEFAULTS;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.Unit;
import migratedb.scanner.PathTarget;
import migratedb.scanner.Scanner;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

/**
 * Scans the build path for relevant MigrateDB resources and classes for auto-discovery at runtime.
 */
public abstract class MigrateDbScan extends DefaultTask implements MigrateDbScanConfig {
    @TaskAction
    public void perform() throws Exception {
        var scannerConfig = getScannerConfig();
        var unprocessablePaths = new ArrayList<Path>();
        var scanResult = new Scanner(it -> {
            unprocessablePaths.add(it);
            return Unit.INSTANCE;
        }).scan(scannerConfig);
        getLogger().log(LogLevel.INFO, "Found " + scanResult.getFoundClasses().size() + " classes, " +
                                       scanResult.getFoundResources().size() + " resources");
        var outputRelativePath = getOutputSubPath().getOrElse(DEFAULTS.outputSubPath);
        var outputDirectory = getOutputDirectory();
        scanResult.writeTo(new PathTarget(outputDirectory.toPath(), true));
        handleUnprocessable(unprocessablePaths);
    }

    private File getOutputDirectory() {
        var baseDir = getOutputDir().getOrNull();
        if (baseDir == null) {
            var sourceSets = getProject().getExtensions().getByType(SourceSetContainer.class);
            var mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            baseDir = mainSourceSet.getOutput().getResourcesDir();
        }
        var relativePath = getOutputSubPath().getOrElse(DEFAULTS.outputSubPath);

        return new File(baseDir, relativePath.replace('/', File.separatorChar));
    }

    private void handleUnprocessable(List<Path> unprocessablePaths) {
        if (!unprocessablePaths.isEmpty()) {
            var message = new StringBuilder("The scope contains elements that couldn't be scanned:\n");
            unprocessablePaths.forEach(it -> message.append("    ").append(it).append("\n"));
            var shouldThrow = getFailBuildOnUnprocessablePath().getOrElse(DEFAULTS.failBuildOnUnprocessablePath);
            if (shouldThrow) {
                throw new IllegalStateException(message.toString());
            } else {
                getLogger().log(LogLevel.WARN, message.toString());
            }
        }
    }

    private Scanner.Config getScannerConfig() {
        Collection<File> scope = getScope().map(FileCollection::getFiles).getOrNull();
        if (scope == null) {
            scope = DEFAULTS.scope(getProject());
        }
        var includes = getIncludes().getOrNull();
        if (includes == null || includes.isEmpty()) {
            includes = DEFAULTS.includes;
        }
        var followSymlinks = getFollowSymlinks().getOrElse(DEFAULTS.followSymlinks);

        var pathsInScope = scope.stream().map(File::toPath).filter(Files::exists).collect(Collectors.toSet());
        return new Scanner.Config(pathsInScope,
                                  new HashSet<>(includes),
                                  it -> true,
                                  followSymlinks);
    }
}
