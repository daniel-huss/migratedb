---                   
layout: documentation
menu: overview
title: Download
---
# Downloads                     

MigrateDB is available via Maven Central!           

## Core Library
https://search.maven.org/artifact/de.unentscheidbar/migratedb-core/{{ site.migratedbVersion }}/jar

## Command Line Interface
https://search.maven.org/artifact/de.unentscheidbar/migratedb-commandline/{{ site.migratedbVersion }}/tar.gz

## Maven Plugin
````xml
<plugin>
    <groupId>de.unentscheidbar</groupId>
    <artifactId>migratedb-maven-plugin</artifactId>
    <version>{{ site.migratedbVersion }}</version>
</plugin>
````

## Gradle Plugin
````groovy
plugins {
    id 'de.unentscheidbar.migratedb' version '{{ site.migratedbVersion }}'
}
````
