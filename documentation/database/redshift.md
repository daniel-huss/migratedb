---
layout: documentation
menu: redshift
subtitle: Redshift
---

# Redshift

## Driver

<table class="table">
<tr>
<th>URL format</th>
<td><code>jdbc:redshift://<i>host</i>:<i>port</i>/<i>database</i></code></td>
</tr>
<tr>
<th>Ships with MigrateDB Command-line</th>
<td>No</td>
</tr>
<tr>
<th>Download</th>
<td>Follow the instructions at <a href="http://docs.aws.amazon.com/redshift/latest/mgmt/configure-jdbc-connection.html#download-jdbc-driver">docs.aws.amazon.com</a></td>
</tr>
<tr>
<th>Supported versions</th>
<td><code>1.2.10.1009</code> and later</td>
</tr>
<tr>
<th></th>
<td><code>2.0.0.5</code> and later are <strong>partially</strong> supported (see below)</td>
</tr>
<tr>
<th>Default Java class</th>
<td><code>com.amazon.redshift.jdbc42.Driver</code></td>
</tr>
</table>

## SQL Script Syntax

- [Standard SQL syntax](/migratedb/documentation/concepts/migrations#sql-based-migrations#syntax) with statement delimiter **;**
- Stored procedures (CREATE FUNCTION with $$ escapes)

### Compatibility

- DDL exported by pg_dump can be used unchanged in a MigrateDB migration. Please note that Redshift does not support
  exporting data using
  pg_dump, so you must export only the schema, using <code>pg_dump -s</code>.
- Any Redshift SQL script executed by MigrateDB,
  can be executed by the PostgreSQL command-line tool and other PostgreSQL-compatible tools,
  such as SQL Workbench/J (after the placeholders have been replaced).

### Example

```sql
/* Single line comment */
CREATE TABLE test_data (
  test_id INT IDENTITY NOT NULL PRIMARY KEY,
  value VARCHAR(25) NOT NULL
);

/*
Multi-line
comment
*/
INSERT INTO test_data (value) VALUES ('Hello');

CREATE VIEW value_only AS SELECT value FROM test_data;

CREATE TABLE another_table AS SELECT 'some-data' as name;

CREATE FUNCTION add(integer, integer) RETURNS integer
     IMMUTABLE
    AS $$
    select $1 + $2;
$$ LANGUAGE sql;

-- Placeholder
INSERT INTO ${tableName} (value) VALUES ('Mr. T');
```

## Limitations

Due to Redshift limitations `ALTER TABLE` and `DROP TABLE` for **external tables** cannot run within a transaction, yet
MigrateDB doesn't
autodetect this. You can work around this limitation and successfully execute such a statement by including a `VACUUM`
statement in the same SQL file as this will force MigrateDB to run the entire migration without a transaction.

The v2 driver is only supported from v2.0.0.5, and only then by MigrateDB setting the connection
property `enableFetchRingBuffer=false`.
We recommend using the latest v1 driver for the time being.
See [here](https://github.com/aws/amazon-redshift-jdbc-driver/issues/4) for more details.

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/cockroachdb">CockroachDB ➡️</a>
</p>
