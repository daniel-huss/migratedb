---
layout: documentation
menu: configuration
pill: skipDefaultResolvers
subtitle: migratedb.skipDefaultResolvers
redirect_from: /documentation/configuration/skipDefaultResolvers/
---

# skipDefaultResolvers

## Description

Whether default built-in resolvers (sql and jdbc) should be skipped. If `true`,
only [custom resolvers](/migratedb/documentation/configuration/parameters/resolvers) are used.

## Default

false

## Usage

### Command line

```powershell
./migratedb -skipDefaultResolvers="true" info
```

### Configuration File

```properties
migratedb.skipDefaultResolvers=true
```

### Environment Variable

```properties
MIGRATEDB_SKIP_DEFAULT_RESOLVERS=true
```

### API

```java
MigrateDB.configure()
    .skipDefaultResolvers(true)
    .load()
```
