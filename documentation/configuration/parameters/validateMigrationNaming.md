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

### API

```java
MigrateDb.configure()
    .validateMigrationNaming(true)
    .load()
```
