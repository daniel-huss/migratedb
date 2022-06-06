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

### Command line

```powershell
./migratedb -group="true" info
```

### Configuration File

```properties
migratedb.group=true
```

### Environment Variable

```properties
MIGRATEDB_GROUP=true
```

### API

```java
MigrateDB.configure()
    .group(true)
    .load()
```

