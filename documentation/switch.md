---
layout: documentation
pill: switching-to-migratedb
subtitle: Switching to MigrateDB
---

# Flyway
        
Although MigrateDB is a fork of Flyway, it's not a drop-in replacement due to several breaking changes.

However, there's not much to do if you want to make the switch, and you won't have to make any changes to your existing SQL migrations.

## Source code changes
* Replace `org.flywaydb` with `migratedb`
* Replace `Flyway` with `MigrateDb`
* Replace `flyway` with `migratedb`
* Replace `MigrationVersion` with either `Version` or `TargetVersion`, depending on how it's used. If its value could be 'current' or 'latest', use TargetVersion, otherwise MigrationVersion.
                    
This should cover most changes, but we recommend checking the full list of breaking changes in the [change log](https://github.com/daniel-huss/migratedb/blob/master/CHANGELOG.adoc).

## Conversion of schema history table

Your existing schema history must be converted into the format used by MigrateDB. You only have to run this _once_ per schema history table

### Java API
```java
MigrateDb.configure()
         .dataSource(myDataSource)
         .oldTable("flyway_schema_history")
         .load()
         .liberate()
```

### Command line

```powershell
./migratedb -oldTable=flyway_schema_history liberate
```

## Build tool changes

MigrateDB performs class path scanning during build-time, not at runtime. This approach is much more maintainable and works in any runtime environment.

There are scanner plugins for Maven and Gradle users and a scanner library for every other build tool.

### Maven

See [Maven plugin usage](/migratedb/documentation/usage/maven).

### Gradle 

See [Gradle plugin usage](/migratedb/documentation/usage/gradle).

### Other build tools

If you're not particularly fond of Maven or Gradle, you can use the scanner API directly in your build script.

````xml
<!--
    Assuming your tool supports pulling jars from
    Maven Central, add the equivalent of this dependency.
-->
<dependency>
    <groupId>de.unentscheidbar</groupId>
    <artifactId>migratedb-scanner</artifactId>
    <version>{{ site.migratedbVersion }}</version>
</dependency>
````
Then execute during build:
````java
new Scanner(unprocessable -> Unit.INSTANCE)
    .scan(new Scanner.Config(
            classPath, // Some set of directories and jar files
            Set.of("db/migration"), // if your migrations live in the db.migration package (default)
            it -> true, // accept all file names
            false // don't follow symlinks))
    .writeTo(new PathTarget(outputDirectory.resolve("db").resolve("migration"), true));
````
with `outputDirectory` being the directory where your build tool places class path resources.
