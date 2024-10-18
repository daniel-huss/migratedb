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

### Configuration File

```properties
migratedb.failOnMissingLocations=true
```

### API

```java
MigrateDb.configure()
    .failOnMissingLocations(true)
    .load()
```
