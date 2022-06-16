---
layout: documentation
menu: cockroachdb
subtitle: CockroachDB
---

# CockroachDB

## Supported Versions

- `21.1`
- `21.0`
- `20.x`

## Driver

<table class="table">
<tr>
<th>URL format</th>
<td><code>jdbc:postgresql://<i>host</i>:<i>port</i>/<i>database</i></code></td>
</tr>
<tr>
<th>SSL support</th>
<td><a href="https://forum.cockroachlabs.com/t/connecting-to-an-ssl-secure-server-using-jdbc-java-and-client-certificate-authentication/400">Yes</a></td>
</tr>
<tr>
<th>Maven Central coordinates</th>
<td><code>org.postgresql:postgresql:42.2.5</code></td>
</tr>
<tr>
<th>Supported versions</th>
<td><code>9.3-1104-jdbc4</code> and later</td>
</tr>
<tr>
<th>Default Java class</th>
<td><code>org.postgresql.Driver</code></td>
</tr>
</table>

## SQL Script Syntax

- [Standard SQL syntax](/migratedb/documentation/concepts/migrations#sql-based-migrations#syntax) with statement delimiter **;**

### Compatibility

- DDL exported by pg_dump can be used unchanged in a MigrateDB migration.
- Any CockroachDB sql script executed by MigrateDB, can be executed by the CockroachDB command-line tool and other
  PostgreSQL-compatible tools (after the placeholders have been replaced).

### Example

```sql
/* Single line comment */
CREATE TABLE test_data (
 value VARCHAR(25) NOT NULL PRIMARY KEY
);


/*
Multi-line
comment
*/

-- Placeholder
INSERT INTO ${tableName} (value) VALUES ('Mr. T');
```

## Limitations

- No support for psql meta-commands with no JDBC equivalent like `\set`

## Additional Information

- See CockroachDB's walkthrough on using MigrateDB [here](https://www.cockroachlabs.com/docs/stable/migratedb.html)

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/saphana">SAP HANA <i class="fa fa-arrow-right"></i></a>
</p>
