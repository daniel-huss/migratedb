---
layout: documentation
menu: configuration
pill: password
subtitle: migratedb.password
redirect_from: /documentation/configuration/password/
---

# Password

## Description

The password to use to connect to the database

This can be omitted if the password is baked into the [url](/migratedb/documentation/configuration/parameters/url) (
See [Sql Server](/migratedb/documentation/database/sqlserver#windows-authentication) for an example), or if password is provided
through another means.

## Usage

This configuration parameter will only be used in the command line version of MigrateDB.

### Command line

```powershell
./migratedb -password=mysecretpassword info
```

### Configuration File

```properties
migratedb.password=mysecretpassword
```

### Environment Variable

```properties
MIGRATEDB_PASSWORD=mysecretpassword
```
