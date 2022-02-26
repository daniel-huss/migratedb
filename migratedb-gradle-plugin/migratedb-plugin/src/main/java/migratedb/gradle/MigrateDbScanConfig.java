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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public interface MigrateDbScanConfig {
    enum Defaults {
        DEFAULTS;

        public final Set<String> includes = Set.of("db/migration");
        public final boolean failBuildOnUnprocessablePath = true;
        public final boolean followSymlinks = false;
        public final String outputSubPath = "db/migration";

        public List<File> scope(Project project) {
            var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
            var mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
            List<File> scope = new ArrayList<>(mainSourceSet.getOutput().getClassesDirs().getFiles());
            var resourcesDir = mainSourceSet.getOutput().getResourcesDir();
            if (resourcesDir != null) {
                scope.add(resourcesDir);
            }
            return scope;
        }
    }

    @Optional
    @InputFiles Property<FileCollection> getScope();

    @Optional
    @Input SetProperty<String> getIncludes();

    @Optional
    @Input Property<Boolean> getFailBuildOnUnprocessablePath();

    @Optional
    @Input Property<Boolean> getFollowSymlinks();

    @Optional
    @OutputDirectory Property<File> getOutputDir();

    @Optional
    @Input Property<String> getOutputSubPath();
}
