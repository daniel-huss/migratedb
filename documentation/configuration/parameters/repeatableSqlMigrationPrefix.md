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

### API

```java
MigrateDb.configure()
    .repeatableSqlMigrationPrefix("A")
    .load()
```
