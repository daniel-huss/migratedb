---
layout: documentation
menu: configuration
pill: placeholderPrefix
subtitle: migratedb.placeholderPrefix
redirect_from: /documentation/configuration/placeholderPrefix/
---

# Placeholder Prefix

## Description

The prefix of every [placeholder](/documentation/configuration/placeholder)

## Default

${

## Usage

### Command line

```powershell
./migratedb -placeholderPrefix="$$" info
```

### Configuration File

```properties
migratedb.placeholderPrefix=$$
```

### Environment Variable

```properties
MIGRATEDB_PLACEHOLDER_PREFIX=$$
```

### API

```java
MigrateDB.configure()
    .placeholderPrefix("$$")
    .load()
```
