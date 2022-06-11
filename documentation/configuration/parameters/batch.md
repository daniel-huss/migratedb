---
layout: documentation
menu: configuration
pill: batch
subtitle: migratedb.batch
redirect_from: /documentation/configuration/batch/
---

{% include not-implemented.html %}

# Batch

## Description

Whether to batch SQL statements when executing them. Batching can save up to 99 percent of network roundtrips by sending
up to 100 statements at once over the network to the database, instead of sending each statement individually.

This is particularly useful for very large SQL migrations composed of multiple MB or even GB of reference data, as this
can dramatically reduce the network overhead.

This is supported for INSERT, UPDATE, DELETE, MERGE, and UPSERT statements. All other statements are automatically
executed without batching.

## Default

false

## Usage

### Command line

```powershell
./migratedb -batch="true" info
```

### Configuration File

```properties
migratedb.batch=true
```

### Environment Variable

```properties
MIGRATEDB_BATCH=true
```

### API

```java
MigrateDB.configure()
    .batch(true)
    .load()
```
