---
layout: documentation
menu: configuration
pill: locations
subtitle: migratedb.locations
redirect_from: /documentation/configuration/locations/
---

# Locations

## Description

Comma-separated list of locations to scan recursively for migrations. The location type is determined by its prefix.

### Classpath

Unprefixed locations or locations starting with <code>classpath:</code> point to a package on the classpath and may
contain both SQL and Java-based migrations.

### Filesystem

Locations starting with <code>filesystem:</code> point to a directory on the filesystem, may only contain SQL migrations
and are only scanned recursively down non-hidden directories.

## Default

classpath:db/migration

## Usage

### Configuration File

```properties
migratedb.locations=filesystem:./sql
```

### API

```java
MigrateDb.configure()
    .locations("filesystem:./sql")
    .load()
```
