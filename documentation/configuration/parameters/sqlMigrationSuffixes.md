---
layout: documentation
menu: configuration
pill: sqlMigrationSuffixes
subtitle: migratedb.sqlMigrationSuffixes
redirect_from: /documentation/configuration/sqlMigrationSuffixes/
---

# SQL Migration Suffixes

## Description

Comma-separated list of file name suffixes for SQL migrations.

SQL migrations have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults
translates to V1_1__My_description.sql

Multiple suffixes (like .sql,.pkg,.pkb) can be specified for easier compatibility with other tools such as editors with
specific file associations.

## Default

.sql

## Usage

### API

```java
MigrateDb.configure()
    .sqlMigrationSuffixes(".sql,.pkg,.pkb")
    .load()
```
