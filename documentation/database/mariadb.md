---
layout: documentation
menu: mariadb
subtitle: MariaDB
---

# MariaDB

## Supported Versions

- `10.6`
- `10.5`
- `10.4`
- `10.3`
- `10.2`
- `10.1`
- `10.0`
- `5.5`
- `5.3`
- `5.2`
- `5.1`

## Driver

<table class="table">
<tr>
<th>URL format</th>
<td><code>jdbc:(mysql|mariadb)://<i>host</i>:<i>port</i>/<i>database</i></code></td>
</tr>
<tr>
<th>SSL support</th>
<td>Yes - add <code>?useSsl=true</code></td>
</tr>
<tr>
<th>Maven Central coordinates</th>
<td><code>org.mariadb.jdbc:mariadb-java-client:2.6.0</code></td>
</tr>
<tr>
<th>Supported versions</th>
<td><code>2.0.0</code> and later</td>
</tr>
<tr>
<th>Default Java class</th>
<td><code>org.mariadb.jdbc.Driver</code></td>
</tr>
</table>

## SQL Script Syntax

- [Standard SQL syntax](/migratedb/documentation/concepts/migrations#sql-based-migrations#syntax) with statement delimiter **;**
- Delimiter change for stored procedures using DELIMITER statements
- Comment directives generated by mysqldump (/!.../;)
- MySQL-style single-line comments (# Comment)

### Compatibility

- DDL exported by mysqldump can be used unchanged in a MigrateDB migration.
- Any MySQL SQL script executed by MigrateDB, can be executed by the MySQL command-line tool and other
  MySQL-compatible tools (after the placeholders have been replaced).

### Example

```sql
/* Single line comment */
CREATE TABLE test_data (
 value VARCHAR(25) NOT NULL,
 PRIMARY KEY(value)
);

/*
Multi-line
comment
*/

-- MySQL procedure
DELIMITER //
CREATE PROCEDURE AddData()
 BEGIN
   # MySQL-style single line comment
   INSERT INTO test_data (value) VALUES ('Hello');
 END //
DELIMITER;

CALL AddData();

-- MySQL comments directives generated by mysqlsump
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;

-- Placeholder
INSERT INTO ${tableName} (value) VALUES ('Mr. T');
```

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/xtradb">Percona XtraDB Cluster ➡️</a>
</p>