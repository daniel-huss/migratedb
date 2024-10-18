---
layout: documentation
menu: configuration
pill: installedBy
subtitle: migratedb.installedBy
redirect_from: /documentation/configuration/installedBy/
---

# Installed By

## Description

The username that will be recorded in the schema history table as having applied the migration.

## Default

<i>Current database user</i>

## Usage

### Configuration File

```properties
migratedb.installedBy=ci-pipeline
```

### API

```java
MigrateDb.configure()
    .installedBy("ci-pipeline")
    .load()
```
