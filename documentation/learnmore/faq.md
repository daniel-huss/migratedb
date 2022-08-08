---
layout: documentation
menu: faq
subtitle: FAQ
redirect_from:
  - /documentation/faq/
  - /documentation/faq.html/
---
# Frequently Asked Questions


* [I found a defect. Where should I report it?](#defect)
* [I have a feature request. Where should I submit it?](#feature-request)
* [I have a question. Where can I ask it?](#question)
* [Does MigrateDB support undo/downgrade/downward migrations?](#downgrade)
* [What is the best strategy for dealing with hot fixes?](#hot-fixes)
* [Can multiple nodes migrate in parallel?](#parallel)
* [Does MigrateDB perform a rollback if a migration fails?](#rollback)
* [Does MigrateDB support multiple schemas?](#multiple-schemas)
* [Does MigrateDB work with OSGI?](#osgi?)
* [Does MigrateDB support placeholder replacement?](#placeholders)
* [Does MigrateDB depend on Spring?](#spring)
* [Can I make structure changes to the DB outside MigrateDB?](#outside-changes)
* [How do you repair the database after a failed migration?](#repair)
* [Why does clean drop individual objects instead of the schema itself?](#clean-objects)
* [What is the best strategy for handling database-specific sql?](#db-specific-sql)
* [Why is the migratedb_state table case-sensitive?](#case-sensitive)

<div id="defect"></div>

## I found a defect. Where should I report it?

Check the [issue tracker](https://github.com/daniel-huss/migratedb/issues?state=open) if someone else already reported
it. If not, [raise a new issue](https://github.com/daniel-huss/migratedb/issues?state=open).


<div id="feature-request"></div>

## I have a feature request. Where should I submit it?

Check the [issue tracker](https://github.com/daniel-huss/migratedb/issues?state=open) if someone else already suggested
it. If not, [raise a new issue](https://github.com/daniel-huss/migratedb/issues?state=open).

<div id="question"></div>

##  I have a question. Where can I ask it?

Post your question on [StackOverflow](http://stackoverflow.com).

<div id="downgrade"></div>

## What about undo/downgrade/downward migrations?

[These are not supported.](/migratedb/documentation/migrations#no-undo-migrations)

<div id="hot-fixes"></div>

## What is the best strategy for dealing with hot fixes?

You have a regular release schedule, say once per sprint. Version 7 is live and you are developing version 8. Version 8
contains DB Schema Changes. Suddenly hot fix is required for version 7, and it also needs a schema change.

**How can you deal with this?**

Even though the code may be branched, the database schema won't. It will have a linear evolution.

This means that the emergency fix, say version 7.1 needs to be deployed as part of the hot fix AND the new version 8.

By the time version 8 will be deployed, MigrateDB will recognize that the migration version 7.1 has already be applied.
It will ignore it and migrate to version 8.

When recreating the database, everything with be cleanly installed in order: version 7, 7.1 and 8.

**If this isn't an option** you can activate the outOfOrder property to allow MigrateDB to run the migrations out of
order and fill the gaps.

<div id="parallel"></div>

## Can multiple nodes migrate in parallel?

Yes! MigrateDB uses the locking technology of your database to coordinate multiple nodes. This ensures that even if
multiple instances of your application attempt to migrate the database at the same time, it still works. Cluster
configurations are fully supported.

<div id="rollback"></div>

## Does MigrateDB perform a rollback if a migration fails?

MigrateDB runs each migration in a separate transaction. In case of failure this transaction is rolled back.
Unfortunately, today only DB2, PostgreSQL, Derby, EnterpriseDB and to a certain extent SQL Server support DDL statements
inside a transaction. Other databases such as Oracle will implicitly sneak in a commit before and after each DDL
statement, drastically reducing the effectiveness of this roll back. One alternative if you want to work around this, is
to include only a single DDL statement per migration. This solution however has the drawback of being quite cumbersome.


<div id="multiple-schemas"></div>

## Does MigrateDB support multiple schemas?

Yes! These are the recommended strategies for dealing with them:

### Multiple identical schemas

If you have multiple identical schemas, say one per tenant, invoke MigrateDB in a loop and change `migratedb.schemas` to
match the name of the schema of the current tenant.

### The schemas are distinct, but have the same life-cycle:

Use a single MigrateDB instance. MigrateDB has support for this built-in. Fill the `migratedb.schemas` property with the
comma-separated list of schemas you wish to manage. All schemas will be tracked using a single schema history table that
will be placed in the first schema of the list. Make sure the user of the datasource has the necessary grants for all
schemas, and prefix the objects (tables, views, ...) you reference.

### The schemas have a distinct life-cycle or must be autonomous and cleanly separated:

Use multiple MigrateDB instances. Each instance manages its own schema and references its own schema history table.
Place migrations for each schema in a distinct location.

Schema foo:

    locations = /sql/foo
    schemas = foo
    table = migratedb_state

Schema bar:

    locations = /sql/bar
    schemas = bar
    table = migratedb_state


<div id="osgi"></div>

## Does MigrateDB work with OSGI?

Yes!


<div id="placeholders"></div>

## Does MigrateDB support placeholder replacement?

Yes! MigrateDB can replace placeholders in SQL migrations. The default pattern is ${placeholder}. This can be configured
using the placeholderPrefix and placeholderSuffix properties.

See [Placeholders](../configuration/placeholder) for more details.


<div id="spring"></div>

## Does MigrateDB depend on Spring?

No. MigrateDB has zero required dependences.

<div id="outside-changes"></div>

## Can I make structure changes to the DB outside MigrateDB?

No. One of the prerequisites for being able to rely on the metadata in the database and having reliable migrations is
that ALL database changes are made by MigrateDB. No exceptions. The price for this reliability is discipline. Ad hoc
changes have no room here as they will literally sabotage your confidence. Even simple things like adding an index can
trip over a migration if it has already been added manually before.

<div id="repair"></div>

## How do you repair the database after a failed migration?

If your database supports DDL transactions, MigrateDB does the work for you.

If your database doesn't, these are the steps to follow:

1. Manually undo the changes of the migration
2. Invoke the repair command
3. Fix the failed migration
4. Try again


<div id="clean-objects"></div>

## Why does `clean` drop individual objects instead of the schema itself?

`clean` will remove what MigrateDB created. If MigrateDB also created the schema itself, `clean` will drop it. Otherwise,
it will only drop the objects within the schema.


<div id="db-specific-sql"></div>

## What is the best strategy for handling database-specific sql?

Assuming you use Derby in TEST and Oracle in PROD.

You can use the `migratedb.locations` property. It would look like this:

TEST (Derby): `migratedb.locations=sql/common,sql/derby`

PROD (Oracle): `migratedb.locations=sql/common,sql/oracle`

You could then have the common statements (`V1__Create_table.sql`) in common and different copies of the DB-specific
statements (`V2__Alter_table.sql`) in the db-specific locations.

An even better solution, in my opinion, is to have the same DB in prod and test. Yes, you do lose a bit of performance,
but on the other hand you also eliminate another difference (and potential source of errors) between the environments.


<div id="case-sensitive"></div>

## Why is the migratedb_state table case-sensitive?

The migratedb_state is case-sensitive due to the quotes used in its creation script. This allows for characters not
supported in identifiers otherwise.

The name (and case) can be configured through the `migratedb.table` property.

The table is an internal MigrateDB implementation detail and not part of the public API. It can therefore change from
time to time.
