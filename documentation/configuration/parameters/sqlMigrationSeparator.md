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

### API

```java
MigrateDb.configure()
    .sqlMigrationSeparator("-")
    .load()
```
