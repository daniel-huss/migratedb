---
layout: documentation
menu: configuration
pill: connectRetries
subtitle: migratedb.connectRetries
redirect_from: /documentation/configuration/connectRetries/
---

# Connect Retries

## Description

The maximum number of retries when attempting to connect to the database. After each failed attempt, MigrateDB will wait
1 second before attempting to connect again, up to the maximum number of times specified by connectRetries. The interval
between retries doubles with each subsequent attempt.

## Default

0

## Usage

### Command line

```powershell
./migratedb -connectRetries=10 info
```

### Configuration File

```properties
migratedb.connectRetries=10
```

### Environment Variable

```properties
MIGRATEDB_CONNECT_RETRIES=10
```

### API

```java
MigrateDb.configure()
    .connectRetries(10)
    .load()
```
