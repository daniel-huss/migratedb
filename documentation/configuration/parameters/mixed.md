---
layout: documentation
menu: configuration
pill: mixed
subtitle: migratedb.mixed
redirect_from: /documentation/configuration/mixed/
---

# Mixed

## Description

Whether to allow mixing transactional and non-transactional statements within the same migration. Enabling this
automatically causes the entire affected migration to be run without a transaction.

Note that this is only applicable for PostgreSQL, Aurora PostgreSQL, SQL Server and SQLite which all have statements
that do not run at all within a transaction.

This is not to be confused with implicit transaction, as they occur in MySQL or Oracle, where even though a DDL
statement was run within within a transaction, the database will issue an implicit commit before and after its
execution.

## Default

false

## Usage

### Command line

```powershell
./migratedb -mixed="true" info
```

### Configuration File

```properties
migratedb.mixed=true
```

### Environment Variable

```properties
MIGRATEDB_MIXED=true
```

### API

```java
MigrateDB.configure()
    .mixed(true)
    .load()
```
