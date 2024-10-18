---
layout: documentation
menu: configuration
pill: resolvers
subtitle: migratedb.resolvers
redirect_from: /documentation/configuration/resolvers/
---

# Resolver

## Description

Comma-separated list of fully qualified class names of
custom [MigrationResolver](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/resolver/MigrationResolver)
implementations to be used in addition to the built-in ones for resolving Migrations to apply.

## Usage

### API

```java
MigrateDb.configure()
    .resolvers("my.resolver.MigrationResolver")
    .load()
```
