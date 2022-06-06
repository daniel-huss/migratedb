---
layout: documentation
menu: configuration
pill: sqlServerKerberosLoginFile
subtitle: migratedb.sqlServer.kerberosLoginFile
---

# SQL Server Kerberos Login File

## Description

The path to the SQL Server login module configuration file (e.g. `SQLJDBCDriver.conf`) for use in Kerberos
authentication.

## Usage

### Command line

```powershell
./migratedb -plugins.sqlserver.kerberos.login.file="/path/to/SQLJDBCDriver.conf" info
```

### Configuration File

```properties
migratedb.plugins.sqlserver.kerberos.login.file=/path/to/SQLJDBCDriver.conf
```

### Environment Variable

```properties
MIGRATEDB_PLUGINS_SQL_SERVER_KERBEROS_LOGIN_FILE=/path/to/SQLJDBCDriver.conf
```

### API

```java
SQLServerConfigurationExtension sqlServerConfigurationExtension=PluginRegister.getPlugin(SQLServerConfigurationExtension.class)
    sqlServerConfigurationExtension.setKerberosLoginFile("/path/to/SQLJDBCDriver.conf");
```
