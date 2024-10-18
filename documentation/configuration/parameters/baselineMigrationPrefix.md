---
layout: documentation
menu: configuration
pill: baselineMigrationPrefix
subtitle: migratedb.baselineMigrationPrefix
---

# Baseline Migration Prefix

## Description

The file name prefix for baseline migrations.

Baseline migrations represent all migrations with `version <= current baseline migration version` while keeping older
migrations if needed for upgrading older deployments.

They have the following file name structure: prefixVERSIONseparatorDESCRIPTIONsuffix, which using the defaults
translates to B1.1__My_description.sql

## Default

B

## Usage


### Configuration File

```properties
migratedb.baselineMigrationPrefix=IB
```

### API

```java
MigrateDb.configure()
    .baselineMigrationPrefix("IB")
    .load()
```
