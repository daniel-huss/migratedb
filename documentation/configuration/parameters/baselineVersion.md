---
layout: documentation
menu: configuration
pill: baselineVersion
subtitle: migratedb.baselineVersion
redirect_from: /documentation/configuration/baselineVersion/
---

# Baseline Version

## Description

The version to tag an existing schema with when executing [baseline](/migratedb/documentation/command/baseline).

## Default

1

## Usage

### Command line

```powershell
./migratedb -baselineVersion="0.0" baseline
```

### Configuration File

```properties
migratedb.baselineVersion=0.0
```

### Environment Variable

```properties
MIGRATEDB_BASELINE_VERSION=0.0
```

### API

```java
MigrateDB.configure()
    .baselineVersion("0.0")
    .load()
```
