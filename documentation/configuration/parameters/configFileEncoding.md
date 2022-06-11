---
layout: documentation
menu: configuration
pill: configFileEncoding
subtitle: migratedb.configFileEncoding
redirect_from: /documentation/configuration/configFileEncoding/
---

# Config File Encoding

## Description

The file encoding to use when loading [MigrateDB configuration files](/migratedb/documentation/configuration/configfile).

The encodings that MigrateDB supports are:

- `US-ASCII`
- `ISO-8859-1`
- `UTF-8`
- `UTF-16BE`
- `UTF-16LE`
- `UTF-16`

All your config files must have the same file encoding. Seriously though, just use UTF-8, always, everywhere.

## Default

UTF-8

## Usage

### Command line

```powershell
./migratedb -configFileEncoding="UTF-16" info
```

### Configuration File

Not available

### Environment Variable

```properties
MIGRATEDB_CONFIG_FILE_ENCODING=UTF-16
```

### API

Not available
