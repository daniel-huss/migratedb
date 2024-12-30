---
layout: documentation
menu: configuration
pill: placeholderSuffix
subtitle: migratedb.placeholderSuffix
redirect_from: /documentation/configuration/placeholderSuffix/
---

# Placeholder Suffix

## Description

The suffix of every [placeholder](/migratedb/documentation/configuration/placeholder)

## Default

}

## Usage

### API

```java
MigrateDb.configure()
    .placeholderSuffix("$$")
    .load()
```