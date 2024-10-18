---
layout: documentation
menu: configuration
pill: encoding
subtitle: migratedb.encoding
redirect_from: /documentation/configuration/encoding/
---

# Encoding

## Description

The encoding of SQL migrations.

The encodings that MigrateDB supports are:

- `US-ASCII`
- `ISO-8859-1`
- `UTF-8`
- `UTF-16BE`
- `UTF-16LE`
- `UTF-16`

We recommend using a consistent file encoding (UTF-8!) across all of your scripts to minimize the issues you encounter.
See [Troubleshooting](/migratedb/documentation/configuration/parameters/encoding#troubleshooting) for known problems and
solutions.

## Default

UTF-8

## Usage

### Configuration File

```properties
migratedb.encoding=UTF-16
```

### I'm getting a MalformedInputException

This exception is due to inconsistent encoding configurations. `ISO-8859-1` is the most compatible supported encoding,
so using this encoding could fix your configuration. However, we recommend that all of your scripts have the same
encoding.
