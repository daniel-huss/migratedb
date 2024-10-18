---                   
layout: documentation
menu: overview
title: Download
---
# Downloads                     

<ul>
    <li><a href="https://mvnrepository.com/artifact/de.unentscheidbar/migratedb-core">Core Library</a></li>
</ul>

````xml
<plugin>
    <groupId>de.unentscheidbar</groupId>
    <artifactId>migratedb-core</artifactId>
    <version>{{ site.migratedbReleaseVersion }}</version>
</plugin>
````

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
