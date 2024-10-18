---
layout: documentation
menu: configuration
pill: target
subtitle: migratedb.target
redirect_from: /documentation/configuration/target/
---

# Target

## Description

The target version up to which MigrateDB should consider migrations. If set to a value other than `current` or `latest`,
this must be a valid migration version (e.g. `2.1`).

When migrating forwards, MigrateDB will apply all migrations up to and including the target version. Migrations with a
higher version number will be ignored. If the target is `current`, then no versioned migrations will be
applied but repeatable migrations will be, together with any callbacks.

Special values:

- `current`: designates the current version of the schema
- `latest`: the latest version of the schema, as defined by the migration with the highest version
- `next`: the next version of the schema, as defined by the first pending migration
- `<version>?`:  Instructs MigrateDB to not fail if the target version doesn't exist. In this case, MigrateDB will go up
  to but not beyond the specified target (default: fail if the target version doesn't exist) (e.g.) `target=2.1?`

## Default

`latest` for versioned migrations.

## Usage

### API

```java
MigrateDb.configure()
    .target("2.0")
    .load()
```
