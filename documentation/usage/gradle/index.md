---
layout: gradle
pill: gradle_overview
subtitle: Gradle Plugin
redirect_from: /documentation/gradle/
---

# Gradle Plugin

The MigrateDB Gradle plugin exists to scan your class path during build time. To _execute_ MigrateDB commands during the
build, just use Gradle's scripting capabilities. After
all, MigrateDB is just another library.

The MigrateDB Gradle plugin supports **Gradle 5 or later** running on **Java 11 or later**.

## Installation

Add the plugin to your `plugins` section:

```groovy
plugins {
    id 'de.unentscheidbar.migratedb' version '{{ migratedbVersion }}'
}
```

Gradle will auto-download the plugin from Gradle's official plugin repository.

## Tasks

### `migratedb.gradle.MigrateDbScan`

The `migratedbScan` task will auto-trigger if your build includes the `classes` task, which should be the case for any
kind of project that targets the JVM or generates a JAR file.

By default, the  `migratedbScan` task will:

* ... scan classes and resources of the main source set
* ... not follow symlinks
* ... include the directory `db/migration`
* ... fail the build if some class path element cannot be processed.

This can be changed via the `migratedb` config object:

```groovy
migratedb {
    scan.includes = ['my/cool/migrations/package']
    scan.scope = configurations.runtimeClasspath + sourceSets.main.output
    scan.followSymlinks = true
    scan.failBuildOnUnprocessablePath = false
}
```

## Example

```groovy
plugins {
    id 'de.unentscheidbar.migratedb'
    id 'java-library'
}

group 'com.foo'

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

dependencies {
    implementation 'com.foo:db-migrations:1.0'
}

migratedb {
    scan.includes = ['db/migration', 'com/foo']
    scan.scope = configurations.runtimeClasspath + sourceSets.main.output
}
```
