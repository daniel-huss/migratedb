---
layout: documentation
menu: configuration
pill: ignoreIgnoredMigrations
subtitle: migratedb.ignoreIgnoredMigrations
redirect_from: /documentation/configuration/ignoreIgnoredMigrations/
---

# Ignore Ignored Migrations

## Description

Ignore ignored migrations when reading
the [schema history table](/migratedb/documentation/concepts/migrations#schema-history-table).

These are migrations that were added in between already migrated migrations in this version.

For example: we have migrations available on the classpath with versions from `1.0` to `3.0`. The schema history table
indicates that version `1` was finished on `1.0.15`, and the next one was `2.0.0`. But with the next release a new
migration was added to version `1`: `1.0.16`. Such scenario is ignored by migrate command, but by default is rejected by
validate.

When `ignoreIgnoredMigrations` is enabled, such case will not be reported by validate command. This is useful for
situations where one must be able to deliver complete set of migrations in a delivery package for multiple versions of
the product, and allows for further development of older versions.

## Default

false

## Usage

### Command line

```powershell
./migratedb -ignoreIgnoredMigrations="true" validate
```

### Configuration File

```properties
migratedb.ignoreIgnoredMigrations=true
```

### Environment Variable

```properties
MIGRATEDB_IGNORE_IGNORED_MIGRATIONS=true
```

### API

```java
MigrateDb.configure()
    .ignoreIgnoredMigrations(true)
    .load()
```
