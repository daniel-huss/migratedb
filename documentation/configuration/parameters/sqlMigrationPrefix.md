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

### API

```java
MigrateDb.configure()
    .sqlMigrationPrefix("M")
    .load()
```
