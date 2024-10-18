---
layout: documentation
menu: configuration
pill: createSchemas
subtitle: migratedb.createSchemas
redirect_from: /documentation/configuration/createSchemas/
---

# Create Schemas

## Description

Whether MigrateDB should attempt to create the schemas specified in the schemas
property. [See this page for more details](/migratedb/documentation/concepts/migrations#the-createschemas-option-and-the-schema-history-table)

## Default

true

## Usage

### Configuration File

```properties
migratedb.createSchemas=false
```

### API

```java
MigrateDb.configure()
    .createSchemas(false)
    .load()
```
