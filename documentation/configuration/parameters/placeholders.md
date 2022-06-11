---
layout: documentation
menu: configuration
pill: placeholders
subtitle: migratedb.placeholders
redirect_from: /documentation/configuration/placeholders/
---

# Placeholders

## Description

[Placeholders](/migratedb/documentation/configuration/placeholder) to replace in SQL migrations.

For example to replace a placeholder named `key1` with the value `value1`, you can
set `migratedb.placeholders.key1=value1`.
MigrateDB will take the `key1` part, and long with
the [placeholder prefix](/migratedb/documentation/configuration/parameters/placeholderPrefix) and
the [placeholder suffix](/migratedb/documentation/configuration/parameters/placeholderSuffix) construct a placeholder replacement,
which by default would look like `${key}`. Then in your SQL migrations and instances of this will be replaced
with `value1`.

Placeholder matching is case insensitive, so a placeholder of `migratedb.placeholders.key1` will match `${key1}`
and `${KEY1}`.

## Usage

### Command line

```powershell
./migratedb -placeholders.key1=value1 -placeholders.key2=value2 info
```

### Configuration File

```properties
migratedb.placeholders.key1=value1
migratedb.placeholders.key2=value2
```

### Environment Variable

```properties
MIGRATEDB_PLACEHOLDERS_KEY1=value1
MIGRATEDB_PLACEHOLDERS_KEY2=value2
```

### API

```java
Map<String, String> placeholders=new HashMap<>();
    placeholders.put("key1","value1");
    placeholders.put("key2","value2");

    MigrateDB.configure()
    .placeholders(placeholders)
    .load()
```
