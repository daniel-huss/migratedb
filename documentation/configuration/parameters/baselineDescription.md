---
layout: documentation
menu: configuration
pill: baselineDescription
subtitle: migratedb.baselineDescription
redirect_from: /documentation/configuration/baselineDescription/
---

# Baseline Description

## Description

The Description to tag an existing schema with when executing [baseline](/documentation/command/baseline).

## Default

<nobr>&lt;&lt; MigrateDB Baseline &gt;&gt;</nobr>

## Usage

### Command line

```powershell
./migratedb -baselineDescription="Baseline" baseline
```

### Configuration File

```properties
migratedb.baselineDescription=Baseline
```

### Environment Variable

```properties
MIGRATEDB_BASELINE_DESCRIPTION=Baseline
```

### API

```java
MigrateDB.configure()
    .baselineDescription("Baseline")
    .load()
```
