---
layout: documentation
menu: configuration
pill: validateOnMigrate
subtitle: migratedb.validateOnMigrate
redirect_from: /documentation/configuration/validateOnMigrate/
---

# Validate On Migrate

## Description

Whether to automatically call [validate](/documentation/command/validate) or not when running migrate.

## Default

true

## Usage

### Command line

```powershell
./migratedb -validateOnMigrate="false" migrate
```

### Configuration File

```properties
migratedb.validateOnMigrate=false
```

### Environment Variable

```properties
MIGRATEDB_VALIDATE_ON_MIGRATE=false
```

### API

```java
MigrateDB.configure()
    .validateOnMigrate(false)
    .load()
```
