---
layout: documentation
menu: configuration
pill: connectRetriesInterval
subtitle: migratedb.connectRetriesInterval
---

# Connect Retries Interval

## Description

The maximum time between retries when attempting to connect to the database in seconds. This will cap the interval
between connect retries to the value provided.

## Default

120

## Usage

### Command line

```powershell
./migratedb -connectRetriesInterval=60 info
```

### Configuration File

```properties
migratedb.connectRetriesInterval=60
```

### Environment Variable

```properties
MIGRATEDB_CONNECT_RETRIES_INTERVAL=60
```

### API

```java
MigrateDB.configure()
    .connectRetriesInterval(60)
    .load()
```
