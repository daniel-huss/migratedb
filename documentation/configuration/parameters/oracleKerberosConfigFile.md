---
layout: documentation
menu: configuration
pill: oracleKerberosConfigFile
subtitle: migratedb.oracle.kerberosConfigFile
redirect_from: /documentation/configuration/oracleKerberosConfigFile/
---

# Oracle Kerberos Config File

## Description

The location of the `krb5.conf` file for use in Kerberos authentication.

## Usage

### Command line

```powershell
./migratedb -oracle.kerberosConfigFile="/etc/krb5.conf" info
```

### Configuration File

```properties
migratedb.oracle.kerberosConfigFile=/etc/krb5.conf
```

### Environment Variable

```properties
MIGRATEDB_ORACLE_KERBEROS_CONFIG_FILE=/etc/krb5.conf
```

### API

```java
MigrateDB.configure()
    .oracleKerberosConfigFile("/etc/krb5.conf")
    .load()
```
