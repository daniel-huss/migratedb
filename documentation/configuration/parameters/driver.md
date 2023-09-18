---
layout: documentation
menu: configuration
pill: driver
subtitle: migratedb.driver
redirect_from: /documentation/configuration/driver/
---

# Driver

## Description

The fully qualified classname of the jdbc driver to use to connect to the database.

This must match the driver for the database type in the [url](/migratedb/documentation/configuration/parameters/url) you are
using.

If you use a driver class that is not shipped with MigrateDB, you must ensure that it is available on the classpath (
see [Adding to the classpath](/migratedb/documentation/adding-to-the-class-path)).

## Default

Auto-detected based on the url

## Usage

### Command line

```powershell
./migratedb -driver=com.microsoft.sqlserver.jdbc.SQLServerDriver info
```

### Configuration File

```properties
migratedb.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

### Environment Variable

```properties
MIGRATEDB_DRIVER=com.microsoft.sqlserver.jdbc.SQLServerDriver
```

### API

```java
MigrateDb.configure()
    .driver("com.microsoft.sqlserver.jdbc.SQLServerDriver")
    .load()
```
