---
layout: documentation
menu: contributing-database-support
subtitle: Contributing Database Compatibility
---

# Contributing Database Compatibility to MigrateDB

We welcome code contributions through Pull Requests on
the [MigrateDB GitHub page](https://github.com/daniel-huss/migratedb). This article will provide help with contributing
code to make MigrateDB compatible with a new database platform.

MigrateDB supports migrations for a large number of database platforms in a unified and consistent way. It does this by
abstracting away the details of each database into a set of classes for each platform, plus factory classes that
construct the appropriate objects for the database at hand; all communication with the database is done through a JDBC
connection. The advantage of this approach is that JDBC is a widely adopted standard; with little more than a JDBC
driver and knowledge of the SQL dialect used by a database it is possible to make MigrateDB compatible with your
database of choice.

## You will need...

* A JDBC driver for your database.
* A Java IDE that builds with Java 11 or higher. We use and recommend IntelliJ

## Getting started

Fork the [MigrateDB](https://github.com/daniel-huss/migratedb) repo. If you’re using IntelliJ, you should be able to
open the MigrateDB top level folder and see a number of projects. Copy the
file `/migratedb-commandline/src/main/assembly/migratedb.conf` to an accessible location on your machine. This location
will be a temporary 'scratch' area for testing. Use this copy to set up the following properties:

* `migratedb.url` - the JDBC URL of your development database
* `migratedb.user` - the user account
* `migratedb.password` - the password to the database
* `migratedb.locations` - to point to an accessible folder where you can put test migrations.

You can now set up a run configuration in your IDE that will compile MigrateDB and run using your newly created
configuration:

* Main class: `migratedb.v{{ site.migratedbApiMajorVersion }}.commandline.Main`
* Program arguments: `info -X -configFiles=<scratch location>\migratedb.conf`
* Classpath of module: `migratedb-commandline`

MigrateDB itself should start. Since MigrateDB doesn't yet support your database you should see a message like:

`migratedb.{{ site.migratedbApiMajorVersion }}.core.api.MigrateDBException: Unable to autodetect JDBC driver for url: jdbc:mydatabase://<host>:<port>/<databasename>`

You’re now ready to start adding that database support. We’re going to assume your database platform is called **FooDb**
. Change the obvious naming conventions to suit your database.

## Let's code!

Here are all the changes and additions you'll need to make:

1. Document the format of the JDBC connection url for your database. This is not necessary to make MigrateDB work, but
   it
   will help adoption of your database!
1. Create a new folder `foo` in `migratedb.internal.database` to contain your new classes.
1. In the folder create classes `FooDatabase` (subclassed from BaseDatabase), `FooSchema` (subclassed from BaseSchema),
   and `FooTable` (subclassed from BaseTable), using the canonical signatures. These classes make up MigrateDB’s
   internal representation of the parts of your database that it works on.
1. Create class `FooParser` (subclassed from BaseParser) using the canonical signature. This represents a simplified
   version of a parser for your database’s dialect of SQL. When finished it will be able to decompose a migration script
   into separate statements and report on serious errors, but it does not need to fully understand them.
1. Create a class `FooDatabaseType` subclassed from `BaseDatabaseType` in the folder your created. This class acts as
   the collation class that brings together all the classes you created before. Implement the required methods. There
   are also some optional methods you can override to customize the behavior.
1. Create class `FooConnection` subclassed from `BaseConnection<FooDatabase>` using the canonical signature. This
   represents a JDBC connection to your database. You probably won’t use it in isolation but it is an important
   component of a `JdbcTemplate`, which provides numerous convenience methods for running queries on your database.  
   In the constructor of `FooConnection`, you can use the `jdbcTemplate` field of `Connection` to query for any database
   properties that you need to acquire immediately and maintain as part of the state of the connection. You will need to
   override the following methods as a minimum:
    * `doRestoreOriginalState()` – to reset anything that a migration may have changed
    * `getCurrentSchemaNameOrSearchPath()` – to return the current database schema for the connection, if this is a
      concept in your database, or the default schema name if not.
    * `doChangeCurrentSchemaOrSearchPath()` – to change the current database schema, if this is a concept in your
      database. If not, use the default which is a no-op.
    * `getSchema()` – to return a constructed `FooSchema` object.
1. Add overrides for `FooDatabase` to customize it to fit the SQL conventions of your database:
    * `doGetConnection()` - to return a new `FooConnection`
    * `ensureSupported()` - to determine which versions of your database will be supported by MigrateDB. During
      development, you can leave this as a no-op.
    * `getRawCreateScript()` - to return SQL appropriate for your database to create the schema history table. Refer to
      an existing database type to see the column types needed. The table name will be provided by the table argument.
      If the baseline argument is true, this method should also insert a row for the baseline migration.
    * `getSelectStatement()` – to return SQL appropriate for your database to select all rows from the history table
      with installed\_rank greater than a parameter value.
    * `getInsertStatement()` – to return SQL appropriate to insert a row into the history table with nine parameter
      values (corresponding to the table columns in order).
    * `supportsDdlTransactions()` – to return whether the database can support executing DDL statements inside a
      transaction or not.
    * `supportsChangingCurrentSchema()` – to return whether the database can support the concept of a current schema
      attached to a connection, which can be changed via SQL.
    * `supportsEmptyMigrationDescription()` - if your database can't support an empty string in the description column
      of the history table verbatim (eg. Oracle implicitly converts it to NULL), override this to return false.
    * `getBooleanTrue()` and `getBooleanFalse()` – to return string representations of the Boolean values as used in
      your database’s dialect of SQL. Typically these are "true" and "false", but could be, for example, "1" and "0"
    * `doQuote()` - to return an escaped version of an identifier for use in SQL. Typically this is the provided value
      with a double-quote added either side, but could be, for example, square brackets either side as in SQL Server.
    * `catalogIsSchema()` – to return true if the database uses a catalog to represent a single schema (eg. MySQL,
      SQLite); false if a catalog is a collection of schemas.
1. Add overrides for `FooParser` to customize it to fit the SQL dialect your database uses:
    * The constructor should call the superclass constructor with a peek depth. This determines how far in advance the
      parser looks to determine the nature of various symbols. 2 is a reasonable start, unless you know your database
      has two-character entities (like Snowflake DB’s `$$` for javascript delimiters) in which case start at 3.
    * Override `getDefaultDelimiter()` if your database uses something other than a semicolon to delimit separate
      statements
    * Override `getIdentifierQuote()` if your database uses something other than a double-quote to escape identifiers (
      eg. MySQL uses backticks)
    * Override `getAlternativeIdentifierQuote()` if your database has a second way to escape identifiers in addition to
      double-quotes.
    * Override `getAlternativeStringLiteralQuote()` if your database has a second way to mark string literals in
      addition to single-quotes (eg. MySql allows double-quotes)
    * Override `getValidKeywords()` if your database has a different set of valid keywords to the standard ones. It's
      not strictly necessary to include keywords that cannot be found in migration scripts.
    * There are other overrides available for handling more complex SQL; contact us for advice in these cases as it is
      beyond the scope of this guide.
1. Add overrides for `FooSchema` to customize it to fit the SQL dialect your database uses:
    * `doExists()` – to query whether the schema described exists in the database
    * `doEmpty()` – to query whether the schema contains any sub-objects eg. tables, views, procedures.
    * `getObjectCount()` – to query the number of objects of a given type that exist in the schema
    * `doCreate()` – to create the schema in the database
    * `doDrop()` – to drop the schema in the database
    * `doClean()` – to drop all the objects that exist in the schema
    * `doAllTables()` – to query for all the tables in the schema and return a populated array of `FooTable` objects
    * `getTable()` – to return a `FooTable` object for the given name
1. Add overrides for `FooTable` to customize it to fit the SQL dialect your database uses:
    * `doDrop()` – to drop the table
    * `doExists()` – to query whether the table described exists in the database
    * `doLock()` – to lock the table with a read/write pessimistic lock until the end of the current transaction. This
      is used to prevent concurrent reads and writes to the schema history while a migration is underway. If your
      database doesn’t support table-level locks, do nothing.`

## Try it!

You should at this point be able to drop your jar into MigrateDB (the `lib` folder is preferable) and
the necessary driver jars into `drivers`, run the `migratedb info` build configuration and see an empty version history.
Congratulations! You have got a basic implementation up and running. You can now start creating migration scripts and
running
`migratedb migrate` on them.

Basic SQL scripts should run with few problems, but you may find more edge cases, particularly in `Parser`. Look at the
existing overrides for existing platforms for examples of how to deal with them. If you find you need to make more
invasive changes in the core of MigrateDB, please do contact us for advice. We will need to test bigger changes
ourselves against all our test instances before we can accept them.

## What's next?

Now that you've proven that MigrateDB can work with your database, you may wish to submit a request for your database to
be listed as compatible on our support pages.

In this case you will need to:

+ Have completed every section of this tutorial
+ Submitted your code as a [Pull Request](https://github.com/daniel-huss/migratedb/pulls) for our review, remembering to
  include supporting material (e.g. test code, results, screenshots etc.) to prove compatibility
+ Completed any requested code changes
