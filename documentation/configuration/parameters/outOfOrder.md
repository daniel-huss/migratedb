---
layout: documentation
menu: configuration
pill: outOfOrder
subtitle: migratedb.outOfOrder
redirect_from: /documentation/configuration/outOfOrder/
---

# Out Of Order

## Description

Allows migrations to be run "out of order".

If you already have versions `1.0` and `3.0` applied, and now a version `2.0` is found, it will be applied too instead
of being ignored.

## Default

false

## Usage

### API

```java
MigrateDb.configure()
    .outOfOrder(true)
    .load()
```
