---
layout: documentation
menu: configuration
pill: placeholderSeparator
subtitle: migratedb.placeholderSeparator
redirect_from: /documentation/configuration/placeholderSeparator/
---

# Placeholder Separator

## Description

The separator of default [placeholders](/documentation/configuration/placeholder)

## Default

:

## Usage

### Command line

```powershell
./migratedb -placeholderSeparator="_" info
```

### Configuration File

```properties
migratedb.placeholderSeparator=_
```

### Environment Variable

```properties
MIGRATEDB_PLACEHOLDER_SEPARATOR=_
```

### API

```java
MigrateDB.configure()
    .placeholderSeparator("_")
    .load()
```
