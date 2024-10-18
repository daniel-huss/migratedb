---
layout: documentation
menu: configuration
pill: tablespace
subtitle: migratedb.tablespace
redirect_from: /documentation/configuration/tablespace/
---

# Tablespace

## Description

The tablespace where to create the schema history table that will be used by MigrateDB.

This setting is only relevant for databases that do support the notion of tablespaces. Its value is simply ignored for
all others.

## Usage

### API

```java
MigrateDb.configure()
    .tablespace("xyz")
    .load()
```
