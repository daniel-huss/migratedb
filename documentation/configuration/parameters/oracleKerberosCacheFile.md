---
layout: documentation
menu: configuration
pill: oracleKerberosCacheFile
subtitle: migratedb.oracle.kerberosCacheFile
redirect_from: /documentation/configuration/oracleKerberosCacheFile/
---
{% include not-implemented.html %}

# Oracle Kerberos Cache File

## Description

The location of the `krb5cc_<UID>` credential cache file for use in Kerberos authentication. This is optional,
and only has any significance when `oracleKerberosConfigFile` is also specified. It may assist performance.

## Usage

### API

```java
MigrateDb.configure()
    .extensionConfig(OracleConfig.class, new OracleConfig()
            .kerberosCacheFile("/temp/krb5cc_123"))
    .load()
```
