---
layout: documentation
menu: configuration
pill: cleanOnValidationError
subtitle: migratedb.cleanOnValidationError
redirect_from: /documentation/configuration/cleanOnValidationError/
---

# Clean On Validation Error

## Description

Whether to automatically call clean or not when a validation error occurs.

This is exclusively intended as a convenience for development. Even though we strongly recommend not to change migration
scripts once they have been checked into SCM and run, this provides a way of dealing with this case in a smooth manner.
The database will be wiped clean automatically, ensuring that the next migration will bring you back to the state
checked into SCM.

<strong>Warning ! Do not enable in production !</strong>

## Default

false

## Usage

### Command line

```powershell
./migratedb -cleanOnValidationError="true" validate
```

### Configuration File

```properties
migratedb.cleanOnValidationError=true
```

### Environment Variable

```properties
MIGRATEDB_CLEAN_ON_VALIDATION_ERROR=true
```

### API

```java
MigrateDb.configure()
    .cleanOnValidationError(true)
    .load()
```
