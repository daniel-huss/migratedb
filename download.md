---                   
layout: documentation
menu: overview
title: Download
---
# Downloads                     

<ul>
    <li><a href="https://mvnrepository.com/artifact/de.unentscheidbar/migratedb-core">Core Library</a></li>
    <li><a href="https://mvnrepository.com/artifact/de.unentscheidbar/migratedb-commandline">Command Line Interface</a></li>

</ul>

## Maven Plugin
````xml
<plugin>
    <groupId>de.unentscheidbar</groupId>
    <artifactId>migratedb-maven-plugin</artifactId>
    <version>{{ site.migratedbReleaseVersion }}</version>
</plugin>
````

## Gradle Plugin
````groovy
plugins {
    id 'de.unentscheidbar.migratedb' version '{{ site.migratedbReleaseVersion }}'
}
````
