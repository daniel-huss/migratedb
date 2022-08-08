---
layout: documentation
menu: derby
subtitle: Derby
---

# Derby

## Supported Versions

- `10.15` (Important: see 'Compatibility' below)
- `10.14`
- `10.13`
- `10.12`
- `10.11`

## Driver

<table class="table">
<tr>
<th>URL format</th>
<td><code>jdbc:derby:<i>subsubprotocol</i>:<i>databaseName</i></code></td>
</tr>
<tr>
<th>Maven Central coordinates</th>
<td><code>org.apache.derby:derbyclient:10.14.1.0</code></td>
</tr>
<tr>
<th>Supported versions</th>
<td><code>10.11</code> and later</td>
</tr>
<tr>
<th>Default Java class</th>
<td><code>org.apache.derby.jdbc.EmbeddedDriver</code></td>
</tr>
</table>

## SQL Script Syntax

- [Standard SQL syntax](/migratedb/documentation/concepts/migrations#sql-based-migrations#syntax) with statement delimiter **;**

### Compatibility

- DDL exported by Derby can be used unchanged in a MigrateDB migration
- Any Derby SQL script executed by MigrateDB, can be executed by the Derby tools (after the placeholders have been
  replaced)
- The Derby 10.15 driver requires Java 9+.

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

-- Sql-style comment

-- Placeholder
INSERT INTO ${tableName} (value) VALUES ('Mr. T');
```

## Limitations

- *None*

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/snowflake">Snowflake ➡️</a>
</p>
