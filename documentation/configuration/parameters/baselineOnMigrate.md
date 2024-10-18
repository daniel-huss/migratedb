---
layout: documentation
menu: configuration
pill: baselineOnMigrate
subtitle: migratedb.baselineOnMigrate
redirect_from: /documentation/configuration/baselineOnMigrate/
---

# Baseline On Migrate

## Description

Whether to automatically call [baseline](/migratedb/documentation/command/baseline) when [migrate](/migratedb/documentation/command/migrate)
is executed against a non-empty schema with no metadata table. This schema will then be baselined with
the `baselineVersion` before executing the migrations. Only migrations above `baselineVersion` will then be applied.

This is useful for initial MigrateDB production deployments on projects with an existing DB.

Be careful when enabling this as it removes the safety net that ensures MigrateDB does not migrate the wrong database in
case of a configuration mistake!

## Default

false

## Usage

### Configuration File

```properties
migratedb.baselineOnMigrate=true
```

### API

```java
MigrateDb.configure()
    .baselineOnMigrate(true)
    .load()
```
