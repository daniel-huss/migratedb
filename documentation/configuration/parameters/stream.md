---
layout: documentation
menu: configuration
pill: stream
subtitle: migratedb.stream
redirect_from: /documentation/configuration/stream/
---

# Stream

## Description

Whether to stream SQL migrations when executing them. Streaming doesn't load the entire migration in memory at once.
Instead each statement is loaded individually.

This is particularly useful for very large SQL migrations composed of multiple MB or even GB of reference data, as this
dramatically reduces MigrateDB's memory consumption.

## Default

false

## Usage

### Command line

```powershell
./migratedb -stream="true" info
```

### Configuration File

```properties
migratedb.stream=true
```

### Environment Variable

```properties
MIGRATEDB_STREAM=true
```

### API

```java
MigrateDB.configure()
    .stream(true)
    .load()
```
