---
layout: documentation
menu: configuration
pill: oracleSqlPlusWarn
subtitle: migratedb.oracleSqlPlusWarn
redirect_from: /documentation/configuration/oracleSqlPlusWarn/
---

# Oracle SQL*Plus Warn

## Description

Whether MigrateDB should issue a warning instead of an error whenever it encounters
an [Oracle SQL*Plus statement](/migratedb/documentation/database/oracle#sqlplus-commands) it doesn't yet support.

## Default

false

## Usage

### Command line

```powershell
./migratedb -oracle.sqlplusWarn="true" info
```

### Configuration File

```properties
migratedb.oracle.sqlplusWarn=true
```

### Environment Variable

```properties
MIGRATEDB_ORACLE_SQLPLUS_WARN=true
```

### API

```java
MigrateDB.configure()
    .oracleSqlplusWarn(true)
    .load()
```
