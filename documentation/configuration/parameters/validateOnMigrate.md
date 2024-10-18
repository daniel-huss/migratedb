---
layout: documentation
menu: configuration
pill: validateOnMigrate
subtitle: migratedb.validateOnMigrate
redirect_from: /documentation/configuration/validateOnMigrate/
---

# Validate On Migrate

## Description

Whether to automatically call [validate](/migratedb/documentation/command/validate) or not when running migrate.

## Default

true

## Usage

### API

```java
MigrateDb.configure()
    .validateOnMigrate(false)
    .load()
```
