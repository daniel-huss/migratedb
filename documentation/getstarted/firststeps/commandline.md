---
layout: documentation
menu: commandline
subtitle: MigrateDB Command-line - First Steps
redirect_from: /getStarted/firststeps/commandline/
---

# First Steps: Command-line

This brief tutorial will teach **how to get up and running with the MigrateDB Command-line tool**. It will take you
through the
steps on how to configure it and how to write and execute your first few database migrations.

This tutorial should take you about **5 minutes** to complete.

## Prerequisites

Start by [downloading the MigrateDB Command-line Tool](/download) for your platform and extract it.
             
## Get some drivers

The JDBC drivers are not part of the distribution, but you can download them easily:
<pre class="console">./migratedb <strong>download-drivers</strong></pre>

<pre class="console">INFO    Processing artifacts for 'DB2 11.5.7.0'
INFO    Downloading 'https://repo1.maven.org/maven2/com/ibm/db2/jcc/11.5.7.0/jcc-11.5.7.0.jar'
INFO    Validating 'com.ibm.db2.jcc-11.5.7.0.jar.tmp'
...
INFO    Validating 'org.xerial.sqlite-jdbc-3.34.0.jar.tmp'
INFO    Successfully downloaded 15 drivers
</pre>

## Configuring MigrateDB

Let's now jump into our new directory created from downloading MigrateDB:

<pre class="console"><span>&gt;</span> cd migratedb-{{ site.migratedbVersion }}</pre>

Configure MigrateDB by editing `/conf/migratedb.conf`, like this:

```properties
migratedb.url=jdbc:h2:file:./foobardb
migratedb.user=SA
migratedb.password=
```

## Creating the first migration

Now create your first migration in the `/sql` directory called `V1__Create_person_table.sql`:

```sql
create table PERSON (
    ID int not null,
    NAME varchar(100) not null
);
```

## Migrating the database

It's now time to execute MigrateDB to migrate your database:

<pre class="console">./migratedb <strong>migrate</strong></pre>

If all went well, you should see the following output:

<pre class="console">Database: jdbc:h2:file:./foobardb (H2 1.4)
Successfully validated 1 migration (execution time 00:00.008s)
Creating Schema History table: "PUBLIC"."migratedb_state"
Current version of schema "PUBLIC": << Empty Schema >>
Migrating schema "PUBLIC" to version 1 - Create person table
Successfully applied 1 migration to schema "PUBLIC" (execution time 00:00.033s)</pre>

## Adding a second migration

If you now add a second migration to the `/sql` directory called `V2__Add_people.sql`:

<pre class="prettyprint">insert into PERSON (ID, NAME) values (1, 'Axel');
insert into PERSON (ID, NAME) values (2, 'Mr. Foo');
insert into PERSON (ID, NAME) values (3, 'Ms. Bar');</pre>

and execute it by issuing:

<pre class="console"><span>migratedb-{{ site.migratedbVersion }}&gt;</span> migratedb <strong>migrate</strong></pre>

You now get:

<pre class="console">Database: jdbc:h2:file:./foobardb (H2 1.4)
Successfully validated 2 migrations (execution time 00:00.018s)
Current version of schema "PUBLIC": 1
Migrating schema "PUBLIC" to version 2 - Add people
Successfully applied 1 migration to schema "PUBLIC" (execution time 00:00.016s)</pre>

## Summary

In this brief tutorial we saw how to:

- install the MigrateDB Command-line tool
- configure it so it can talk to our database
- write our first couple of migrations

These migrations were then successfully found and executed.

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/usage/commandline">Read the documentation <i class="fa fa-arrow-right"></i></a>
</p>
