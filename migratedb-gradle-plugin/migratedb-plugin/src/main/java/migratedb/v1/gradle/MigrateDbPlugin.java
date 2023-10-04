/*
 * Copyright 2022-2023 The MigrateDB contributors
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

package migratedb.v1.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public final class MigrateDbPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        var config = project.getExtensions().create("migratedb", MigrateDbConfig.class);
        var scanTask = project.getTasks().register("migratedbScan", MigrateDbScan.class, task -> {
            var taskConfig = config.getScan();
            task.getFailBuildOnUnprocessablePath().set(taskConfig.getFailBuildOnUnprocessablePath());
            task.getFollowSymlinks().set(taskConfig.getFollowSymlinks());
            task.getIncludes().set(taskConfig.getIncludes());
            task.getOutputDir().set(taskConfig.getOutputDir());
            task.getOutputSubPath().set(taskConfig.getOutputSubPath());
            task.getScope().set(taskConfig.getScope());
        });
        project.afterEvaluate(it -> {
            var classesTask = project.getTasks().findByPath(":classes");
            if (classesTask != null) {
                classesTask.finalizedBy(scanTask.get());
            }
        });
    }
}
