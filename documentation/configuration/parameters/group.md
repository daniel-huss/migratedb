---
layout: documentation
menu: configuration
pill: group
subtitle: migratedb.group
redirect_from: /documentation/configuration/group/
---

# Group

## Description

Whether to group all pending migrations together in the same transaction when applying them (only recommended for
databases with support for DDL transactions)

## Default

false

## Usage

### Configuration File

```properties
migratedb.group=true
```

### API

```java
MigrateDb.configure()
    .group(true)
    .load()
```

