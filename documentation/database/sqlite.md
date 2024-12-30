---
layout: documentation
menu: sqlite
subtitle: SQLite
---

# SQLite

## Supported Versions

- `3.7` and later `3.x` versions

## Drivers

<table class="table">
<thead>
<tr>
<th></th>
<th>Java (Xerial)</th>
</tr>
</thead>
<tr>
<th>URL format</th>
<td><code>jdbc:sqlite:<i>database</i></code></td>
</tr>
<tr>
<th>Maven Central coordinates</th>
<td><code>org.xerial:sqlite-jdbc:3.30.1</code></td>
</tr>
<tr>
<th>Supported versions</th>
<td><code>3.7</code> and later</td>
</tr>
<tr>
<th>Default Java class</th>
<td><code>org.sqlite.JDBC</code></td>
</tr>
</table>

## SQL Script Syntax

- [Standard SQL syntax](/migratedb/documentation/concepts/migrations#sql-based-migrations#syntax) with statement
  delimiter **;**
- Triggers with `BEGIN ... END;` block

### Compatibility

- DDL exported by SQLite can be used unchanged in a MigrateDB migration
- Any SQLite SQL script executed by MigrateDB, can be executed by the SQLite tools (after the placeholders have been
  replaced)

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

CREATE TABLE customers(name VARCHAR, address VARCHAR);
CREATE TABLE orders(address VARCHAR, customer_name VARCHAR);
CREATE TRIGGER update_customer_address UPDATE OF address ON customers
  BEGIN
    UPDATE orders SET address = new.address WHERE customer_name = old.name;
  END;
```

## Limitations

- No concurrent migration as SQLite does not support `SELECT ... FOR UPDATE` locking
- No support for multiple schemas or changing the current schema, as SQLite doesn't support schemas
- No support for `CREATE TRANSACTION` and `COMMIT` statements within a migration, as SQLite doesn't support nested
  transactions

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/database/firebird">Firebird ➡️</a>
</p>