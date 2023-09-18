---
layout: documentation
menu: configuration
pill: callbacks
subtitle: migratedb.callbacks
redirect_from: /documentation/configuration/callbacks/
---

# Callbacks

## Description

Comma-separated list of fully qualified class names
of [Callback](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/callback/Callback) implementations to use to
hook into the MigrateDB lifecycle, or packages to scan for these classes. Ensure the class or package is available on
the classpath (see [Adding to the classpath](/migratedb/documentation/adding-to-the-class-path)).

Note: SQL callbacks matching the correct name pattern are loaded from locations (
see [Callbacks](/migratedb/documentation/concepts/callbacks)). This configuration parameter is only used for loading java
callbacks. To disable loading sql callbacks,
see [skipDefaultCallbacks](/migratedb/documentation/configuration/parameters/skipDefaultCallbacks).

## Default

db/callback

## Usage

### Command line

```powershell
./migratedb -callbacks="my.callback.MigrateDBCallback" info
```

### Configuration File

```properties
migratedb.callbacks=my.callback.MigrateDBCallback
```

### Environment Variable

```properties
MIGRATEDB_CALLBACKS=my.callback.MigrateDBCallback
```

### API

```java
MigrateDb.configure()
    .callbacks("my.callback.MigrateDBCallback")
    .load()
```
