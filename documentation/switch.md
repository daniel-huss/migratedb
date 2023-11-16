---
layout: documentation
pill: switching-to-migratedb
subtitle: Switching to MigrateDB
---

# Flyway
        
Although MigrateDB is a fork of Flyway, it's not a drop-in replacement due to several breaking API changes.

However, there's not much to do if you want to make the switch, and you won't have to make any changes to your existing SQL migrations.

## Source code and configuration changes

1. Replace `org.flywaydb` with `migratedb.v{{ site.migratedbApiMajorVersion }}`
2. Replace `Flyway` with `MigrateDb`
3. Replace `flyway` with `migratedb`
4. Replace `MigrationVersion` with either `Version` or `TargetVersion`, depending on how it's used. If its value could be 'current' or 'latest', use TargetVersion, otherwise Version.
                    
This should cover most changes. The full list of breaking changes can be found in the [change log](https://github.com/daniel-huss/migratedb/blob/master/CHANGELOG.adoc).

## Conversion of schema history table

Your existing schema history needs to be converted into the format used by MigrateDB. This will, by default, happen automatically the next time you [migrate](/migratedb/documentation/usage/commandline/migrate). If your schema history does not use the default name `flyway_schema_history`, set the configuration parameter `oldTable`to the custom table name.

Alternatively, you can execute the `liberate` command manually. You only have to run it _once_ per schema history table:

### Java API                        
```java
MigrateDb.configure()
         .dataSource(myDataSource)
         .oldTable("table_if_different_from_default")
         .load()
         .liberate()
```

### Command line

```powershell
./migratedb -oldTable=table_if_different_from_default liberate
```

## Build tool changes

MigrateDB does class path scanning during build-time, not at runtime. This approach is much more maintainable and works in any runtime environment.

There are scanner plugins for Maven and Gradle users and a scanner library for every other build tool.

### Maven

See [Maven plugin usage](/migratedb/documentation/usage/maven).

### Gradle 

See [Gradle plugin usage](/migratedb/documentation/usage/gradle).

### Other build tools

Other build tools may use the scanner API directly.

````xml
<!--
    Assuming your tool supports pulling jars from
    Maven Central, add the equivalent of this dependency.
-->
<dependency>
    <groupId>de.unentscheidbar</groupId>
    <artifactId>migratedb-scanner</artifactId>
    <version>{{ site.migratedbReleaseVersion }}</version>
</dependency>
````
Then execute during build:
````java
new Scanner(unprocessable -> Unit.INSTANCE)
    .scan(new Scanner.Config(
            classPath, // Some set of directories and jar files
            Set.of("db/migration"), // if your migrations live in the db.migration package (default)
            fileName -> true, // accept all file names
            false // don't follow symlinks))
    .writeTo(new PathTarget(outputDirectory.resolve("db").resolve("migration"), true));
````
with `outputDirectory` being the directory where your build tool places class path resources.
