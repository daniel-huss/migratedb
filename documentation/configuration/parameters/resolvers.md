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

You must ensure that the resolver is available on the classpath (
see [Adding to the classpath](/migratedb/documentation/adding-to-the-class-path)).

## Usage

### Command line

```powershell
./migratedb -resolvers="my.resolver.MigrationResolver" info
```

### Configuration File

```properties
migratedb.resolvers=my.resolver.MigrationResolver
```

### Environment Variable

```properties
MIGRATEDB_RESOLVERS=my.resolver.MigrationResolver
```

### API

```java
MigrateDB.configure()
    .resolvers("my.resolver.MigrationResolver")
    .load()
```
