---
layout: documentation
menu: configuration
pill: repeatableSqlMigrationPrefix
subtitle: migratedb.repeatableSqlMigrationPrefix
redirect_from: /documentation/configuration/repeatableSqlMigrationPrefix/
---

# Repeatable SQL Migration Prefix

## Description

The file name prefix for repeatable SQL migrations.

Repeatable SQL migrations have the following file name structure: prefixSeparatorDESCRIPTIONsuffix, which using the
defaults translates to R__My_description.sql

## Default

R

## Usage

### Command line

```powershell
./migratedb -repeatableSqlMigrationPrefix="A" info
```

### Configuration File

```properties
migratedb.repeatableSqlMigrationPrefix=A
```

### Environment Variable

```properties
MIGRATEDB_REPEATABLE_SQL_MIGRATION_PREFIX=A
```

### API

```java
MigrateDb.configure()
    .repeatableSqlMigrationPrefix("A")
    .load()
```
