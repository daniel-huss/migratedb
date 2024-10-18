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

### API

```java
MigrateDb.configure()
    .skipDefaultResolvers(true)
    .load()
```
