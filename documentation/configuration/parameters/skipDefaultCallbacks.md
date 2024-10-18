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

### API

```java
MigrateDb.configure()
    .skipDefaultCallbacks(true)
    .load()
```
