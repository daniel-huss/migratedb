---
layout: documentation
menu: configuration
pill: url
subtitle: migratedb.url
redirect_from: /documentation/configuration/url/
---

# URL

## Description

The jdbc url to use to connect to the database.

Note: Only certain JDBC drivers are packaged with migratedb. If your driver is not packaged, then you need to ensure it
is available on the classpath (see [Adding to the classpath](/migratedb/documentation/adding-to-the-class-path)).

## Usage

This configuration parameter will only be used in the command line version of MigrateDB.

### Command line

```powershell
./migratedb -url=jdbc:h2:mem:migratedb_db info
```

### Configuration File

```properties
migratedb.url=jdbc:h2:mem:migratedb_db
```

### Environment Variable

```properties
MIGRATEDB_URL=jdbc:h2:mem:migratedb_db
```
