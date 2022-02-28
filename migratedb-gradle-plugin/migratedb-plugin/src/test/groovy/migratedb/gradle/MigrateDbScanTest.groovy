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
package migratedb.gradle

import groovy.transform.Canonical
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource

import java.util.stream.Stream

import static migratedb.core.api.Location.ClassPathLocation.CLASS_LIST_RESOURCE_NAME
import static migratedb.core.api.Location.ClassPathLocation.RESOURCE_LIST_RESOURCE_NAME
import static org.gradle.testkit.runner.TaskOutcome.FAILED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class MigrateDbScanTest {

    @ParameterizedTest
    @ArgumentsSource(Params)
    void "Scans project build output by default"(P p) {
        // given
        plugins << p.compilingPlugin
        resources['src/main/resources/db/migration/V000__Foo.sql'] = ''
        resources['src/main/java/db/migration/V001__Bar.java'] = """
            package db.migration;
            public final class V001__Bar {}
        """

        // when
        def result = buildProject(gradleVersion: p.gradleVersion)

        // then
        assert result.task(':migratedbScan')?.outcome == SUCCESS
        assert linesOf("build/resources/main/db/migration/$CLASS_LIST_RESOURCE_NAME") == ['db.migration.V001__Bar']
        assert linesOf("build/resources/main/db/migration/$RESOURCE_LIST_RESOURCE_NAME") == ['db/migration/V000__Foo.sql']
    }

    @ParameterizedTest
    @ArgumentsSource(Params)
    void "Can include entire runtime class path"(P p) {
        // given
        plugins << p.compilingPlugin
        dependencies << "implementation 'org.apache.commons:commons-lang3:3.12.0'"
        resources['src/main/resources/foo/migration/V000__Foo.sql'] = ''
        resources['src/main/java/foo/migration/V001__Bar.java'] = """
            package foo.migration;
            public final class V001__Bar {}
        """
        pluginConfig = """
            migratedb {
                scan.scope = configurations.runtimeClasspath + sourceSets.main.output
                scan.includes = ['foo', 'org/apache']
            }
        """

        // when
        def result = buildProject(gradleVersion: p.gradleVersion)

        // then
        assert result.task(':migratedbScan')?.outcome == SUCCESS
        linesOf("build/resources/main/db/migration/$CLASS_LIST_RESOURCE_NAME").with {
            assert it.contains('foo.migration.V001__Bar')
            assert it.contains('org.apache.commons.lang3.mutable.MutableObject')
        }
        assert linesOf("build/resources/main/db/migration/$RESOURCE_LIST_RESOURCE_NAME") == ['foo/migration/V000__Foo.sql']
    }

    @ParameterizedTest
    @ArgumentsSource(Params)
    void "Unprocessable file fails build by default"(P p) {
        // given
        plugins << p.compilingPlugin
        resources['unprocessable.war'] = ''
        pluginConfig = """
            migratedb {
                scan.scope = files('unprocessable.war')
            }
        """

        // when
        def result = buildProject(gradleVersion: p.gradleVersion, expectFailure: true)

        // then
        assert result.task(':migratedbScan')?.outcome == FAILED: result.output
    }

    @BeforeEach
    void setup() {
        settingsFile = new File(testProjectDir, 'settings.gradle')
        settingsFile << """
            rootProject.name = 'MigrateDB Gradle Plugin Test'    
        """
        buildFile = new File(testProjectDir, 'build.gradle')
    }

    @TempDir
    File testProjectDir
    File buildFile
    File settingsFile
    List repositories = ['mavenLocal()', 'mavenCentral()']
    List plugins = ["id 'de.unentscheidbar.migratedb'"]
    List dependencies = []
    String pluginConfig = ''
    Map<String, Object> resources = [:]

    @Canonical
    static class P {
        String compilingPlugin
        String gradleVersion

        @Override
        String toString() {
            return "$gradleVersion / $compilingPlugin"
        }
    }

    static class Params implements ArgumentsProvider {
        @Override
        Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            def gradleVersions = ['7.4', '7.3', '7.2', '7.1', '7.0']
            def compilingPlugins = [
                    "id 'java'",
                    "id 'java-library'",
                    "id 'application'",
                    "id 'org.jetbrains.kotlin.jvm' version '1.6.10'"
            ]
            return gradleVersions.stream()
                    .flatMap { v ->
                        compilingPlugins.stream()
                                .map { p -> new P(p, v) }
                    }
                    .map(Arguments::arguments)
        }
    }

    private def createResource(Map.Entry entry) {
        def path = entry.key.toString()
        def content = entry.value?.toString() ?: ''
        def file = new File(testProjectDir, path.replace('/', File.separator))
        file.parentFile.mkdirs()
        file.write(content, 'UTF-8', false)
    }

    def linesOf(String path) {
        return new File(testProjectDir, path.replace('/', File.separator))?.readLines('UTF-8')
    }

    def buildProject(Map args = [:]) {
        def nextLine = '\n                '
        buildFile.text = """
            plugins {
                ${plugins.join(nextLine)}
            }
            repositories {
                ${repositories.join(nextLine)}
            }                               
            dependencies {
                ${dependencies.join(nextLine)}
            }                 
                
            $pluginConfig            

            tasks.findByPath(':startScripts')?.configure {
                mainClassName = 'Main'
            }
        """
        resources.each { createResource(it) }

        def runner = GradleRunner.create()
                .withGradleVersion(args['gradleVersion']?.toString() ?: '7.4')
                .withDebug(true)
                .withProjectDir(testProjectDir)
                .withArguments(args['task']?.toString() ?: 'build')
                .withPluginClasspath()
        return args['expectFailure'] ? runner.buildAndFail() : runner.build()
    }
}
