---
layout: documentation
menu: configuration
pill: oracleKerberosConfigFile
subtitle: migratedb.oracle.kerberosConfigFile
redirect_from: /documentation/configuration/oracleKerberosConfigFile/
---
{% include not-implemented.html %}

# Oracle Kerberos Config File

## Description

The location of the `krb5.conf` file for use in Kerberos authentication.

## Usage

### API

```java
MigrateDb.configure()
    .extensionConfig(OracleConfig.class, new OracleConfig()
            .kerberosConfigFile("/etc/krb5.conf"))
    .load()
```
