---
layout: documentation
menu: configuration
pill: initSql
subtitle: migratedb.initSql
redirect_from: /documentation/configuration/initSql/
---

# Init SQL

## Description

The SQL statements to run to initialize a new database connection immediately after opening it.

This is mainly used for setting some state on the connection that needs to be shared across all scripts, or should not
be included into any one script.

## Usage

### Command line

```powershell
./migratedb -initSql="ALTER SESSION SET NLS_LANGUAGE='ENGLISH';" info
```

### Configuration File

```properties
migratedb.initSql=ALTER SESSION SET NLS_LANGUAGE='ENGLISH';
```

### Environment Variable

```properties
MIGRATEDB_INIT_SQL=ALTER SESSION SET NLS_LANGUAGE='ENGLISH';
```

### API

```java
MigrateDB.configure()
    .initSql("ALTER SESSION SET NLS_LANGUAGE='ENGLISH';")
    .load()
```
