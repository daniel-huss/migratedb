---
layout: documentation
menu: oracle
subtitle: Oracle
---

# Oracle

## Supported Versions

- `19.3`
- `18.3`
- `12.2`
- `12.1`

## Driver

<table class="table">
    <tr>
        <th>URL format</th>
        <td>
            <code>jdbc:oracle:thin:@//<i>host</i>:<i>port</i>/<i>service</i></code><br>
            <code>jdbc:oracle:thin:@<i>tns_entry</i></code> *
        </td>
    </tr>
    <tr>
        <th>Maven Central coordinates</th>
        <td><code>com.oracle.database.jdbc:ojdbc8:19.6.0.0</code></td>
    </tr>
    <tr>
        <th>Supported versions</th>
        <td><code>18.3.0.0</code> and later</td>
    </tr>
    <tr>
        <th>Default Java class</th>
        <td><code>oracle.jdbc.OracleDriver</code></td>
    </tr>
</table>

\* `TNS_ADMIN` environment variable must point to the directory of where `tnsnames.ora` resides

## SQL Script Syntax

- [Standard SQL syntax](/migratedb/documentation/concepts/migrations#sql-based-migrations#syntax) with statement
  delimiter `;`
- PL/SQL blocks starting with `DECLARE` or `BEGIN` and finishing with `END; /`

### Compatibility

- DDL exported by Oracle can be used unchanged in a MigrateDB migration
- Any Oracle SQL script executed by MigrateDB can be executed by SQL*Plus and other Oracle-compatible tools (after the
  placeholders have been replaced)

### Example

```sql
/* Single line comment */
CREATE TABLE test_user (
  name VARCHAR(25) NOT NULL,
  PRIMARY KEY(name)
);

/*
Multi-line
comment
*/
-- PL/SQL block
CREATE TRIGGER test_trig AFTER insert ON test_user
BEGIN
   UPDATE test_user SET name = CONCAT(name, ' triggered');
END;
/

-- Placeholder
INSERT INTO ${tableName} (name) VALUES ('Mr. T');
```

### Output

When `SET SERVEROUTPUT ON` is invoked, output produced by `DBMS_OUTPUT.PUT_LINE` will be shown in the console.

## Authentication

### JDBC

Oracle supports user and password being provided in the JDBC URL, in the form

`jdbc:oracle:thin:<user>/<password>@//<host>:<port>/<database>`

In this case, they do not need to be passed separately in configuration and the MigrateDB commandline will not prompt
for them.

### Oracle Wallet

MigrateDB can connect to your databases using credentials in your Oracle Wallet.

First you need to ensure you have set the environment variable `TNS_ADMIN` to point to the location containing
your `tnsnames.ora` file. Then you will need to configure
the [`migratedb.oracle.walletLocation`](/migratedb/documentation/configuration/parameters/oracleWalletLocation) parameter to point
to the location of your Oracle wallet. Lastly your URL should be provided as specified in `tnsnames.ora` i.e. if it is
using an alias then connect with the `jdbc:oracle:thin:@db_alias` syntax.

With that configuration you will be able to connect to your database without providing any credentials in config.

### Kerberos

You can authenticate using Kerberos by specifying the location of the local Kerberos configuration file (which contains
details such as the locations of Kerberos Key Distribution Centers), and optionally the local credential cache, to
MigrateDB. For example, in `migratedb.conf`:

```properties
migratedb.oracle.kerberosConfigFile=/etc/krb5.conf
migratedb.oracle.kerberosCacheFile=/tmp/krb5cc_123
```

### Proxy Authentication

MigrateDB allows you to proxy through other users during migrations. You can read about how to enable proxying for
users [here](https://docs.oracle.com/cd/E11882_01/java.112/e16548/proxya.htm#JJDBC28352).

To configure MigrateDB to use a proxy connection, you need to add
to [jdbcProperties](/migratedb/documentation/configuration/parameters/jdbcProperties) a key `PROXY_USER_NAME` whose value is the
name of the user you are trying to proxy as. For example, if you connect as user `A` to MigrateDB (
i.e. `migratedb.user=A`) and you want to proxy as user `B` for migrations, you need to
add `migratedb.jdbcproperties.PROXY_USER_NAME=B`.

## Limitations

- SPATIAL EXTENSIONS: `sdo_geom_metadata` can only be cleaned for the user currently logged in

### SQL*Plus

#### Unsupported commands

Not all SQL*Plus commands are supported by MigrateDB. Unsupported commands are gracefully ignored with a warning
message.

#### Behavior parity

As much as possible, MigrateDB aims to emulate the behavior of the SQL*Plus client in Oracle SQL Developer. However,
there are some edge cases where MigrateDB isn't able to emulate the behavior exactly. Known cases are detailed below:

- Abbreviations: MigrateDB is limited by JDBC support for particular commands, and this is more strict than the
  SQL*Plus client; in general abbreviations are supported by MigrateDB as
  documented [here](https://docs.oracle.com/cd/B19306_01/server.102/b14357/ch12041.htm),
  so for example `SHOW ERRORS` can be abbreviated to `SHO ERR`, but not `SHOW ERROR` (which is accepted by the client).

- SQL*Plus is known to replace CRLF pairs in string literals with single LFs. MigrateDB will not do this - instead it
  preserves scripts as they are written

If you encounter a discrepancy between the Oracle SQL*Plus client and MigrateDB, let us know via the official support
email.

#### Referenced scripts and checksums

MigrateDB includes any referenced scripts when calculating checksums. This also extends to `login.sql` and `glogin.sql`
since their contents can affect the reproducibility of a migration and can differ in different environments.

### Known issues and workarounds

Implementing a compatible solution to some problems isn't always possible, so we document those problems and the valid
workarounds.

#### A default schema different to the current user's causes remote links to fail

MigrateDB alters the current schema to the
specified [default schema](/migratedb/documentation/configuration/parameters/defaultSchema) as this is where the schema history
table should reside. This causes remote links to fail in migrations that expect the current schema to be the user's. The
workarounds for this are:

- Create the remote link via dynamic SQL in a stored procedure that resides in the correct schema. Stored procedures
  execute as the schema owner, so the remote link is created in the correct schema
- Use [beforeEachMigrate](/migratedb/documentation/concepts/callbacks#beforeEachMigrate)
  and [afterEachMigrate](/migratedb/documentation/concepts/callbacks#afterEachMigrate) callbacks to alter the current schema as
  needed

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/sqlserver">SQL Server ➡️</a>
</p>
