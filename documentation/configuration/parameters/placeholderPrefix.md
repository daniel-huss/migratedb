---
layout: documentation
menu: configuration
pill: placeholderPrefix
subtitle: migratedb.placeholderPrefix
redirect_from: /documentation/configuration/placeholderPrefix/
---

# Placeholder Prefix

## Description

The prefix of every [placeholder](/migratedb/documentation/configuration/placeholder)

## Default

${

## Usage

### API

```java
MigrateDb.configure()
    .placeholderPrefix("$$")
    .load()
```
