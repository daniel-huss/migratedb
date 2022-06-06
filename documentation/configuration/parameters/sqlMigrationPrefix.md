---
layout: documentation
menu: configuration
pill: sqlMigrationPrefix
subtitle: migratedb.sqlMigrationPrefix
redirect_from: /documentation/configuration/sqlMigrationPrefix/
---

# SQL Migration Prefix

## Description

The file name prefix for versioned SQL migrations.

Versioned SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using
the defaults translates to V1.1__My_description.sql

## Default

V

## Usage

### Command line

```powershell
./migratedb -sqlMigrationPrefix="M" info
```

### Configuration File

```properties
migratedb.sqlMigrationPrefix=M
```

### Environment Variable

```properties
MIGRATEDB_SQL_MIGRATION_PREFIX=M
```

### API

```java
MigrateDB.configure()
    .sqlMigrationPrefix("M")
    .load()
```
