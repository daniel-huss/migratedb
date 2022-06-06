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

### Command line

```powershell
./migratedb -baselineMigrationPrefix="IB" info
```

### Configuration File

```properties
migratedb.baselineMigrationPrefix=IB
```

### Environment Variable

```properties
MIGRATEDB_BASELINE_MIGRATION_PREFIX=IB
```

### API

```java
MigrateDB.configure()
    .baselineMigrationPrefix("IB")
    .load()
```
