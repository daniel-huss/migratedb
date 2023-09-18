---
layout: documentation
menu: configuration
pill: loggers
subtitle: migratedb.loggers
---

# Loggers

## Description

The `loggers` configuration parameter allows you to override MigrateDB's logging auto-detection and specify an exact
logger, or comma-separated list of loggers, you wish to use.
This can be useful when a dependency comes with a logger you do not wish to use.

### Valid Options

* `auto` - Auto detect the logger (default behavior)
* `console` - Use stdout/stderr (_only available when using CLI_)
* `slf4j2` - Use the slf4j2 logger
* `apache-commons` - Use the Apache Commons logger

Alternatively you can provide the fully qualified class name for any other logger to use that.

### Default

`auto`

## Usage

### Command line

```powershell
./migratedb -loggers=auto
```

### Configuration File

```properties
migratedb.loggers=auto
```

### Environment Variable

```properties
MIGRATEDB_LOGGERS=auto
```

### API

```java
MigrateDb.configure()
    .loggers("auto")
    .load()
```
