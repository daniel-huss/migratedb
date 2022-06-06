---
layout: documentation
menu: configuration
pill: oracleKerberosCacheFile
subtitle: migratedb.oracle.kerberosCacheFile
redirect_from: /documentation/configuration/oracleKerberosCacheFile/
---

# Oracle Kerberos Cache File

## Description

The location of the `krb5cc_<UID>` credential cache file for use in Kerberos authentication. This is optional,
and only has any significance when `oracleKerberosConfigFile` is also specified. It may assist performance.

## Usage

### Command line

```powershell
./migratedb -oracle.kerberosCacheFile="/temp/krb5cc_123" info
```

### Configuration File

```properties
migratedb.oracle.kerberosCacheFile=/temp/krb5cc_123
```

### Environment Variable

```properties
MIGRATEDB_ORACLE_KERBEROS_CACHE_FILE=/temp/krb5cc_123
```

### API

```java
MigrateDB.configure()
    .oracleKerberosCacheFile("/temp/krb5cc_123")
    .load()
```
