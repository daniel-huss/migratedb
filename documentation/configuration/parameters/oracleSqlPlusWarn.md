---
layout: documentation
menu: configuration
pill: oracleSqlPlusWarn
subtitle: migratedb.oracleSqlPlusWarn
redirect_from: /documentation/configuration/oracleSqlPlusWarn/
---
{% include not-implemented.html %}

# Oracle SQL*Plus Warn

## Description

Whether MigrateDB should issue a warning instead of an error whenever it encounters
an [Oracle SQL*Plus statement](/migratedb/documentation/database/oracle#sqlplus-commands) it doesn't yet support.

## Default

false

## Usage

### API

```java
MigrateDb.configure()
    .extensionConfig(OracleConfig.class, new OracleConfig()
        .sqlplusWarn(true))
    .load()
```
