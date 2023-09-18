---
layout: documentation
menu: configuration
pill: skipDefaultCallbacks
subtitle: migratedb.skipDefaultCallbacks
redirect_from: /documentation/configuration/skipDefaultCallbacks/
---

# Skip Default Callbacks

## Description

Whether default built-in callbacks (sql) should be skipped. If true,
only [custom callbacks](/migratedb/documentation/configuration/parameters/callbacks) are used.

## Default

false

## Usage

### Command line

```powershell
./migratedb -skipDefaultCallbacks="true" info
```

### Configuration File

```properties
migratedb.skipDefaultCallbacks=true
```

### Environment Variable

```properties
MIGRATEDB_SKIP_DEFAULT_CALLBACKS=true
```

### API

```java
MigrateDb.configure()
    .skipDefaultCallbacks(true)
    .load()
```
