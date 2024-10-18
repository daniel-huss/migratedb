---
layout: documentation
menu: configuration
pill: baselineDescription
subtitle: migratedb.baselineDescription
redirect_from: /documentation/configuration/baselineDescription/
---

# Baseline Description

## Description

The Description to tag an existing schema with when executing [baseline](/migratedb/documentation/command/baseline).

## Default

<nobr>&lt;&lt; MigrateDB Baseline &gt;&gt;</nobr>

## Usage

### Configuration File

```properties
migratedb.baselineDescription=Baseline
```

### API

```java
MigrateDb.configure()
    .baselineDescription("Baseline")
    .load()
```
