---
layout: documentation
menu: configuration
pill: table
subtitle: migratedb.table
redirect_from: /documentation/configuration/table/
---

# Table

## Description

The name of MigrateDB's [schema history table](/migratedb/documentation/concepts/migrations#schema-history-table).

By default (single-schema mode) the schema history table is placed in the default schema for the connection provided by
the datasource.

When the [defaultSchema](/migratedb/documentation/configuration/parameters/defaultSchema)
or [schemas](/migratedb/documentation/configuration/parameters/schemas) property is set (multi-schema mode), the schema history
table is placed in the specified default schema.

## Default

migratedb_state

## Usage

### API

```java
MigrateDb.configure()
    .table("my_schema_history_table")
    .load()
```
