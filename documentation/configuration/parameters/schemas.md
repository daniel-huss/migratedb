---
layout: documentation
menu: configuration
pill: schemas
subtitle: migratedb.schemas
redirect_from: /documentation/configuration/schemas/
---

# Schemas

## Description

Comma-separated, case-sensitive list of schemas managed by MigrateDB.

MigrateDB will attempt to create these schemas if they do not already exist, and will clean them in the order of this
list.
If MigrateDB created them, then the schemas themselves will be dropped when cleaning.

If [defaultSchema](/migratedb/documentation/configuration/parameters/defaultSchema) is not specified, the first schema in this
list also acts as the default schema, which is where the schema history table will be placed.

## Usage

### API

```java
MigrateDb.configure()
    .schemas("schema1","schema2")
    .load()
```
