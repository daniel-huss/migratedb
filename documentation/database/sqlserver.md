---
layout: documentation
menu: sqlServer
subtitle: SQL Server
---

# SQL Server

## Supported Versions

- `2019`
- `2017`
- `2016`
- `2014`
- `2012`

## Driver

<table class="table">
<tr>
<th>URL format</th>
<td><code>jdbc:sqlserver://<i>host</i>:<i>port</i>;databaseName=<i>database</i></code></td>
</tr>
<tr>
<th>SSL support</th>
<td><a href="https://docs.microsoft.com/en-us/sql/connect/jdbc/connecting-with-ssl-encryption?view=sql-server-ver15">Yes</a> - add <code>;encrypt=true</code></td>
</tr>
<tr>
<th>Maven Central coordinates</th>
<td><code>com.microsoft.sqlserver:mssql-jdbc:9.2.1.jre8</code></td>
</tr>
<tr>
<th>Supported versions</th>
<td><code>4.0</code> and later</td>
</tr>
<tr>
<th>Default Java class</th>
<td><code>com.microsoft.sqlserver.jdbc.SQLServerDriver</code></td>
</tr>
</table>

## SQL Script Syntax

- [Standard SQL syntax](/migratedb/documentation/concepts/migrations#sql-based-migrations#syntax) with statement delimiter **GO**
- T-SQL

### Compatibility

- DDL exported by SQL Server can be used unchanged in a MigrateDB migration.
- Any SQL Server sql script executed by MigrateDB, can be executed by Sqlcmd, SQL Server Management Studio and other SQL
  Server-compatible tools (after the placeholders have been replaced).

### Example

```sql
/* Single line comment */
CREATE TABLE Customers (
CustomerId smallint identity(1,1),
Name nvarchar(255),
Priority tinyint
)
CREATE TABLE Sales (
TransactionId smallint identity(1,1),
CustomerId smallint,
[Net Amount] int,
Completed bit
)
GO

/*
Multi-line
comment
*/
-- TSQL
CREATE TRIGGER dbo.Update_Customer_Priority
 ON dbo.Sales
AFTER INSERT, UPDATE, DELETE
AS
WITH CTE AS (
 select CustomerId from inserted
 union
 select CustomerId from deleted
)
UPDATE Customers
SET
 Priority =
   case
     when t.Total < 10000 then 3
     when t.Total between 10000 and 50000 then 2
     when t.Total > 50000 then 1
     when t.Total IS NULL then NULL
   end
FROM Customers c
INNER JOIN CTE ON CTE.CustomerId = c.CustomerId
LEFT JOIN (
 select
   Sales.CustomerId,
   SUM([Net Amount]) Total
 from Sales
 inner join CTE on CTE.CustomerId = Sales.CustomerId
 where
   Completed = 1
 group by Sales.CustomerId
) t ON t.CustomerId = c.CustomerId
GO

-- Placeholder
INSERT INTO ${tableName} (Name, Priority) VALUES ('Mr. T', 1);
```

## Authentication

SQL Server supports several methods of authentication. These include:

- SQL Server Authentication
- Windows Authentication
- Azure Active Directory
- Kerberos

SQL Server Authentication works 'out-of-the-box' with MigrateDB, whereas the others require extra manual setup.

The instructions provided here are adapted from
the [Microsoft JDBC Driver for SQL Server documentation](https://docs.microsoft.com/en-us/sql/connect/jdbc/microsoft-jdbc-driver-for-sql-server?view=sql-server-ver15)
. Refer to this when troubleshooting authentication problems.

**Note:** These instructions may be incomplete. MigrateDB depends on Microsoft's JDBC drivers, which in turn have many
environmental dependencies to enable different authentication types. You may have to perform your own research to get
the JDBC driver working for the different authentication types.

### SQL Server Authentication

This uses a straightforward username and password to authenticate. Provide these with the `user` and `password`
configuration options.

### Windows Authentication

[Windows Authentication, also known as Integrated Security](https://docs.microsoft.com/en-us/dotnet/framework/data/adonet/sql/authentication-in-sql-server)
, is enabled by amending your JDBC connection string to set `integratedSecurity=true`.

Example: `jdbc:sqlserver://<i>host</i>:<i>port</i>;databaseName=<i>database</i>;integratedSecurity=true`.

### Azure Active Directory

#### Installing MSAL4J

You must add Microsoft's [MSAL4J library](https://mvnrepository.com/artifact/com.microsoft.azure/msal4j) to your
classpath. For instance, as a Maven or Gradle dependency.

For command-line users, MSAL4J is already included, so no extra installation is required.

#### Connecting

There are several types of Azure Active Directory authentication:

- Azure Active Directory with MFA
- Azure Active Directory Integrated
- Azure Active Directory MSI
- Azure Active Directory with Password
- Azure Active Directory Service Principal
- Access Tokens

To use the various authentication types, amend your JDBC URL to set the `authentication` parameter:

- For Active Directory Integrated set `authentication=ActiveDirectoryIntegrated`
    - e.g: <code>jdbc:sqlserver://<i>host</i>:<i>port</i>;databaseName=<i>database</i>
      ;authentication=ActiveDirectoryIntegrated</code>
- For Active Directory MSI set `authentication=ActiveDirectoryMSI`
    - e.g: <code>jdbc:sqlserver://<i>host</i>:<i>port</i>;databaseName=<i>database</i>
      ;authentication=ActiveDirectoryMSI</code>
- For Active Directory With Password set `authentication=ActiveDirectoryPassword`
    - e.g: <code>jdbc:sqlserver://<i>host</i>:<i>port</i>;databaseName=<i>database</i>
      ;authentication=ActiveDirectoryPassword</code>
    - You must also supply a username and password with MigrateDB's `user` and `password` configuration options
- For Active Directory Interactive set `authentication=ActiveDirectoryInteractive`
    - e.g: <code>jdbc:sqlserver://<i>host</i>:<i>port</i>;databaseName=<i>database</i>
      ;authentication=ActiveDirectoryInteractive</code>
    - This will begin an interactive process which expects user input (e.g. a dialogue box), so it's not recommended in
      automated environments
- For Active Directory Service Principal set `authentication=ActiveDirectoryServicePrincipal `
    - e.g: <code>jdbc:sqlserver://<i>host</i>:<i>port</i>;databaseName=<i>database</i>
      ;authentication=ActiveDirectoryServicePrincipal</code>

[The Microsoft documentation has more details about how these work with JDBC URLs](https://docs.microsoft.com/en-us/sql/connect/jdbc/connecting-using-azure-active-directory-authentication?view=sql-server-ver15)
.

## Connecting to a Named Instance

When connecting to a named instance, the JDBC URL must be of the form:

```
jdbc:sqlserver://<server_name>;instanceName=<instance_name>;databaseName=<database_name>
```

For example:

```
jdbc:sqlserver://test_server;instanceName=test_instance;databaseName=test_database
```

**Note:** If a named instance is used along with the `<host>:<port>` syntax in the JDBC URL, the driver will connect to
the port over the named instance.

## Limitations

- MigrateDB's automatic detection for whether SQL statements are valid in transactions does not apply to
  `CREATE/ALTER/DROP` statements acting on memory-optimized tables (that is, those created with
  `WITH (MEMORY_OPTIMIZED = ON)`). You will need to override the `executeInTransaction` setting to be false,
  either on a [per-script basis](/migratedb/documentation/configuration/scriptconfigfiles) or globally.
- SQL Server is unable to change the default schema for a session. Therefore, setting the `migratedb.defaultSchema`
  property
  has no value, unless used for a [Placeholder](/migratedb/documentation/concepts/migrations#placeholder-replacement) in
  your sql scripts. If you decide to use `migratedb.defaultSchema`, it also must exist in `migratedb.schemas`.
- By default, the migratedb schema history table will try to write to the default schema for the database connection.
  You may
  specify which schema to write this table to by setting `migratedb.schemas=custom_schema`, as the first entry will
  become the
  default schema if `migratedb.defaultSchema` itself is not set.
- With these limitations in mind, please refer to the properties or options
  mentioned [here](/migratedb/documentation/configuration/parameters/defaultSchema) for descriptions/consequences.
- If using the JTDS driver, then setting `ANSI_NULLS` or `QUOTED_IDENTIFIER` in a script will cause an error. This is
  a driver limitation, and can be solved by using the Microsoft driver instead.

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/azuresynapse">Azure Synapse ➡️</a>
</p>