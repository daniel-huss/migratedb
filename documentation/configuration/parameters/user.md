---
layout: documentation
menu: configuration
pill: user
subtitle: migratedb.user
redirect_from: /documentation/configuration/user/
---

# User

## Description

The user to use to connect to the database.

This can be omitted if the user is baked into the [url](/documentation/configuration/parameters/url) (
See [Sql Server](/documentation/database/sqlserver#windows-authentication) for an example).

## Usage

### Command line

```powershell
./migratedb -user=myuser info
```

### Configuration File

```properties
migratedb.user=myuser
```

### Environment Variable

```properties
MIGRATEDB_USER=myuser
```

### API

```java
MigrateDB.configure()
    .user("myuser")
    .load()
```
