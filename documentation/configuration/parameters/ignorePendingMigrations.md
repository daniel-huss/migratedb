---
layout: documentation
menu: configuration
pill: ignorePendingMigrations
subtitle: migratedb.ignorePendingMigrations
redirect_from: /documentation/configuration/ignorePendingMigrations/
---

# Ignore Pending Migrations

## Description

Ignore pending migrations when reading
the [schema history table](/migratedb/documentation/concepts/migrations#schema-history-table). These are migrations that are
available but have not yet been applied.

This can be useful for verifying that in-development migration changes don't contain any validation-breaking changes of
migrations that have already been applied to a production environment, e.g. as part of a CI/CD process, without failing
because of the existence of new migration versions.

## Default

false

## Usage

### Command line

```powershell
./migratedb -ignorePendingMigrations="true" validate
```

### Configuration File

```properties
migratedb.ignorePendingMigrations=true
```

### Environment Variable

```properties
MIGRATEDB_IGNORE_PENDING_MIGRATIONS=true
```

### API

```java
MigrateDb.configure()
    .ignorePendingMigrations(true)
    .load()
```
