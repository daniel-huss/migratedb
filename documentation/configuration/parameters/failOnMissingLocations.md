---
layout: documentation
menu: configuration
pill: failOnMissingLocations
subtitle: migratedb.failOnMissingLocations
---

# Fail On Missing Locations

## Description

Whether to fail if a location specified in the [`locations`](/migratedb/documentation/configuration/parameters/locations) option
doesn't exist.

## Default

false

## Usage

### Command line

```powershell
./migratedb -failOnMissingLocations="true" migrate
```

### Configuration File

```properties
migratedb.failOnMissingLocations=true
```

### Environment Variable

```properties
MIGRATEDB_FAIL_ON_MISSING_LOCATIONS=true
```

### API

```java
MigrateDB.configure()
    .failOnMissingLocations(true)
    .load()
```
