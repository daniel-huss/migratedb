---                   
layout: documentation
menu: overview
title: Download
---
# Downloads                     

<ul>
    <li><a href="https://search.maven.org/artifact/de.unentscheidbar/migratedb-core/{{ site.migratedbVersion }}/jar">Core Library</a></li>
    <li><a href="https://search.maven.org/artifact/de.unentscheidbar/migratedb-commandline/{{ site.migratedbVersion }}/tar.gz">Command Line Interface</a></li>

</ul>

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
