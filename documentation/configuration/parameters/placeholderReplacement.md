---
layout: documentation
menu: configuration
pill: placeholderReplacement
subtitle: migratedb.placeholderReplacement
redirect_from: /documentation/configuration/placeholderReplacement/
---

# Placeholder Replacement

## Description

Whether [placeholders](/documentation/configuration/placeholder) should be replaced

## Default

true

## Usage

### Command line

```powershell
./migratedb -placeholderReplacement="false" info
```

### Configuration File

```properties
migratedb.placeholderReplacement=false
```

### Environment Variable

```properties
MIGRATEDB_PLACEHOLDER_REPLACEMENT=false
```

### API

```java
MigrateDB.configure()
    .placeholderReplacement(false)
    .load()
```
