---
layout: documentation
menu: configuration
pill: placeholderSuffix
subtitle: migratedb.placeholderSuffix
redirect_from: /documentation/configuration/placeholderSuffix/
---

# Placeholder Suffix

## Description

The suffix of every [placeholder](/migratedb/documentation/configuration/placeholder)

## Default

}

## Usage

### Command line

```powershell
./migratedb -placeholderSuffix="$$" info
```

### Configuration File

```properties
migratedb.placeholderSuffix=$$
```

### Environment Variable

```properties
MIGRATEDB_PLACEHOLDER_SUFFIX=$$
```

### API

```java
MigrateDB.configure()
    .placeholderSuffix("$$")
    .load()
```
