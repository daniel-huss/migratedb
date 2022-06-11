---
layout: documentation
menu: configuration
pill: skipExecutingMigrations
subtitle: migratedb.skipExecutingMigrations
redirect_from: /documentation/configuration/skipExecutingMigrations/
---

# Skip Executing Migrations

## Description

Whether MigrateDB should skip migration execution. The remainder of the operation will run as normal - including
updating the schema history table, callbacks, and so on.

`skipExecutingMigrations` essentially allows you to mimic a migration being executed, because the schema history table
is still updated as normal.

`skipExecutingMigrations` can be used to bring an out-of-process change into MigrateDB's change control process. For
instance, a script run against the database outside of MigrateDB (like a hotfix) can be turned into a migration. The
hotfix migration can be deployed with MigrateDB with `skipExecutingMigrations=true`. The schema history table will be
updated with the new migration, but the script itself won't be executed again.

`skipExecutingMigrations` can be used with with [cherryPick](/migratedb/documentation/configuration/parameters/cherryPick) to skip
specific migrations.

## Default

false

## Usage

### Command line

```powershell
./migratedb -skipExecutingMigrations="true" migrate
```

### Configuration File

```properties
migratedb.skipExecutingMigrations=true
```

### Environment Variable

```properties
MIGRATEDB_SKIP_EXECUTING_MIGRATIONS=true
```

### API

```java
MigrateDB.configure()
    .skipExecutingMigrations(true)
    .load()
```