---
layout: documentation
menu: configuration
pill: lockRetryCount
subtitle: migratedb.lockRetryCount
redirect_from: /documentation/configuration/lockRetryCount
---

# Lock Retry Count

## Description

At the start of a migration, MigrateDB will attempt to take a lock to prevent competing instances executing in parallel.
If this lock cannot be obtained straight away, MigrateDB will retry at 1s intervals, until this count is reached, at
which
point it will abandon the migration. A value of -1 indicates that MigrateDB should keep retrying indefinitely.

## Default

<i>Retry 50 times then fail</i>

## Usage

### Command line

```powershell
./migratedb -lockRetryCount=10 migrate
```

### Configuration File

```properties
migratedb.lockRetryCount=10
```

### Environment Variable

```properties
MIGRATEDB_LOCK_RETRY_COUNT=10
```

### API

```java
MigrateDb.configure()
    .lockRetryCount(10)
    .load()
```
