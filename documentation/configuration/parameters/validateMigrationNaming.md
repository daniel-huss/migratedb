---
layout: documentation
menu: configuration
pill: validateMigrationNaming
subtitle: migratedb.validateMigrationNaming
redirect_from: /documentation/configuration/validateMigrationNaming/
---

# Validate Migration Naming

## Description

Whether to ignore migration files whose names do not match the naming conventions.

If `false`, files with invalid names are ignored and MigrateDB continues normally. If `true`, MigrateDB fails fast and
lists the offending files.

## Default

false

## Usage

### Command line

```powershell
./migratedb -validateMigrationNaming="true" info
```

### Configuration File

```properties
migratedb.validateMigrationNaming=true
```

### Environment Variable

```properties
MIGRATEDB_VALIDATE_MIGRATION_NAMING=true
```

### API

```java
MigrateDB.configure()
    .validateMigrationNaming(true)
    .load()
```