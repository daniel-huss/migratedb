---
layout: documentation
menu: configuration
pill: ignoreMigrationPatterns
subtitle: migratedb.ignoreMigrationPatterns
---

# Ignore Migration Patterns

## Description

Ignore migrations during `validate` and `repair` according to a given list
of [patterns](/migratedb/documentation/configuration/parameters/ignoreMigrationPatterns#patterns).

Only `Missing` migrations are ignored during `repair`,

### Patterns

Patterns are of the form `type`:`status` with `*` matching `type` or `status`.

`type` must be one of (*case insensitive*):

* `repeatable`
* `versioned`
* `*` *(will match any of the above)*

`status` must be one of (*case insensitive*):

* `Missing`
* `Pending`
* `Ignored`
* `Future`
* `*` *(will match any of the above)*

For example, the pattern to ignore missing repeatables is:

```
repeatable:missing
```

Patterns are comma seperated. For example, to ignore missing repeatables and pending versioned migrations:

```
repeatable:missing,versioned:pending
```

The `*` wild card is also supported, thus:

```
*:missing
```

will ignore missing migrations no matter their type and:

```
repeatable:*
```

will ignore repeatables regardless of their state.

## Default

empty

## Usage

### Configuration File

```properties
migratedb.ignoreMigrationPatterns="repeatable:missing"
```

### API

```java
MigrateDb.configure()
    .ignoreMigrationPatterns("repeatable:missing")
    .load()
```
