---
layout: documentation
menu: configuration
pill: defaultSchema
subtitle: migratedb.defaultSchema
redirect_from: /documentation/configuration/defaultSchema/
---

# Default Schema

## Description

The default schema managed by MigrateDB. This schema will be the one containing
the [schema history table](/migratedb/documentation/concepts/migrations#schema-history-table).
If not specified in [schemas](/migratedb/documentation/configuration/parameters/schemas), MigrateDB will automatically attempt to
create and clean this schema first.

This schema will also be the default for the database connection (provided the database supports this concept).

## Default

If [schemas](/migratedb/documentation/configuration/parameters/schemas) is specified, the first schema in that list. Otherwise,
the database's default schema.

## Usage

### Command line

```powershell
./migratedb -defaultSchema="schema2" info
```

### Configuration File

```properties
migratedb.defaultSchema=schema2
```

### Environment Variable

```properties
MIGRATEDB_DEFAULT_SCHEMA=schema2
```

### API

```java
MigrateDB.configure()
    .defaultSchema("schema2")
    .load()
```
