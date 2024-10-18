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

### Configuration File

```properties
migratedb.connectRetriesInterval=60
```

### API

```java
MigrateDb.configure()
    .connectRetriesInterval(60)
    .load()
```
