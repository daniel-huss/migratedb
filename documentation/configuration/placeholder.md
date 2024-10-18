---
layout: documentation
menu: placeholders
subtitle: Placeholders
redirect_from: /documentation/placeholders/
---

# Placeholders

MigrateDB comes with support for placeholder replacement in:

- [SQL migrations](/migratedb/documentation/configuration/placeholder#sql-migration-placeholders)

# SQL Migration Placeholders

In addition to regular SQL syntax, MigrateDB also supports placeholder replacement with configurable prefixes and
suffixes.
By default it looks for Ant-style placeholders like `${myplaceholder}`. This can be very useful to abstract differences
between environments.

Changing the value of placeholders will cause repeatable migrations to be re-applied on next migrate.

Placeholders are also provided as additional properties to the database connection, so placeholders reserved by your
database (e.g. `serverName` for SQL Server) will be used by the connection.

Placeholders are supported in versioned migrations, repeatable migrations, and SQL callbacks.

## How to configure

Placeholders can be configured through a number of different ways.

- Via configuration parameters. `migratedb.placeholders.myplaceholder=value`
- Via the api. `.placeholders(Map.of("myplaceholder", "value"))`

Placeholders are case insensitive, so a placeholder like `${myplaceholder}` can be specified with any of the above
techniques.

See [parameters](/migratedb/documentation/configuration/parameters/#placeholders) for placeholder specific configuration
parameters.

## Default placeholders

MigrateDB also provides default placeholders, whose values are automatically populated:

- `${migratedb:defaultSchema}` = The default schema for MigrateDB
- `${migratedb:database}` = The name of the database from the connection url
- `${migratedb:timestamp}` = The time that MigrateDB parsed the script, formatted as 'yyyy-MM-dd HH:mm:ss'
- `${migratedb:filename}` = The filename of the current script
- `${migratedb:workingDirectory}` = The user working directory as defined by
  the ['user.dir']((https://docs.oracle.com/javase/tutorial/essential/environment/sysprop.html)) System Property
- `${migratedb:table}` = The name of the MigrateDB schema history table

### Example

Here is a small example of the supported syntax:

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

-- Default placeholders
GRANT SELECT ON SCHEMA ${migratedb:defaultSchema} TO ${migratedb:user};

-- User defined placeholder
INSERT INTO ${tableName} (name) VALUES ('Mr. T');
```
