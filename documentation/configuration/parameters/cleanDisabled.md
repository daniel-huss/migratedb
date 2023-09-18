---
layout: documentation
menu: configuration
pill: cleanDisabled
subtitle: migratedb.cleanDisabled
redirect_from: /documentation/configuration/cleanDisabled/
---

# Clean Disabled

## Description

Whether to disable clean. This is especially useful for production environments where running clean can be quite a
career limiting move.

## Default

true

## Usage

### Command line

```powershell
./migratedb -cleanDisabled="true" clean
```

### Configuration File

```properties
migratedb.cleanDisabled=true
```

### Environment Variable

```properties
MIGRATEDB_CLEAN_DISABLED=true
```

### API

```java
MigrateDb.configure()
    .cleanDisabled(true)
    .load()
```
