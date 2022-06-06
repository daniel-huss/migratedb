---
layout: documentation
menu: configuration
pill: kerberosConfigFile
subtitle: migratedb.kerberosConfigFile
---

# Kerberos Config File

## Description

The path to the your Kerberos configuration file (e.g. `krb5.ini`) for use in Kerberos authentication.

## Usage

### Command line

```powershell
./migratedb -kerberosConfigFile="/path/to/krb5.ini" info
```

### Configuration File

```properties
migratedb.kerberosConfigFile=/path/to/krb5.ini
```

### Environment Variable

```properties
MIGRATEDB_KERBEROS_CONFIG_FILE=/path/to/krb5.ini
```

### API

```java
MigrateDB.configure()
    .kerberosConfigFile("/path/to/krb5.ini")
    .load()
```

