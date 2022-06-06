---
layout: documentation
menu: configuration
pill: sqlMigrationSeparator
subtitle: migratedb.sqlMigrationSeparator
redirect_from: /documentation/configuration/sqlMigrationSeparator/
---

# SQL Migration Separator

## Description

The file name separator for Sql migrations.

Versioned SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using
the defaults translates to V1.1__My_description.sql

## Default

__

## Usage

### Command line

```powershell
./migratedb -sqlMigrationSeparator="-" info
```

### Configuration File

```properties
migratedb.sqlMigrationSeparator=-
```

### Environment Variable

```properties
MIGRATEDB_SQL_MIGRATION_SEPARATOR=-
```

### API

```java
MigrateDB.configure()
    .sqlMigrationSeparator("-")
    .load()
```
