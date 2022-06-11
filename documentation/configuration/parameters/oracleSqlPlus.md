---
layout: documentation
menu: configuration
pill: oracleSqlPlus
subtitle: migratedb.oracleSqlPlus
redirect_from: /documentation/configuration/oracleSqlPlus/
---

# Oracle SQL*Plus

## Description

Enable MigrateDB's support for [Oracle SQL*Plus commands](/migratedb/documentation/database/oracle#sqlplus-commands).

## Default

false

## Usage

### Command line

```powershell
./migratedb -oracle.sqlplus="true" info
```

### Configuration File

```properties
migratedb.oracle.sqlplus=true
```

### Environment Variable

```properties
MIGRATEDB_ORACLE_SQLPLUS=true
```

### API

```java
MigrateDB.configure()
    .oracleSqlplus(true)
    .load()
```

## Use Cases

### Configuring consistent sessions for your migrations

See our list of [supported SQL\*Plus commands](/migratedb/documentation/database/oracle#sqlplus-commands) and how you can utilize
them with [site and user profiles](/migratedb/documentation/database/oracle#site-profiles-gloginsql--user-profiles-loginsql) once
SQL\*Plus is enable to achieved this.
