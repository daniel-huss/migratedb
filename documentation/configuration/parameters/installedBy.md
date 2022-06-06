---
layout: documentation
menu: configuration
pill: installedBy
subtitle: migratedb.installedBy
redirect_from: /documentation/configuration/installedBy/
---

# Installed By

## Description

The username that will be recorded in the schema history table as having applied the migration.

## Default

<i>Current database user</i>

## Usage

### Command line

```powershell
./migratedb -installedBy="ci-pipeline" clean
```

### Configuration File

```properties
migratedb.installedBy=ci-pipeline
```

### Environment Variable

```properties
MIGRATEDB_INSTALLED_BY=ci-pipeline
```

### API

```java
MigrateDB.configure()
    .installedBy("ci-pipeline")
    .load()
```
