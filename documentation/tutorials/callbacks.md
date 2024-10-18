---
layout: documentation
menu: tut_callbacks
subtitle: 'Tutorial: Callbacks'
redirect_from:

- /getStarted/callbacks/
- /documentation/getstarted/callbacks/
- /documentation/getstarted/advanced/callbacks/

---

# Tutorial: Callbacks

This brief tutorial will teach **how to use callbacks**. It will take you through the
steps on how to create and use them.

## Introduction

**Callbacks** let you hook into MigrateDB's lifecycle. This is particularly useful when you execute the same
housekeeping
action over and over again.

They are typically used for

- Recompiling procedures
- Updating materialized views
- Storage housekeeping (`VACUUM` for PostgreSQL for example)


## Creating a callback

Now let's create a callback to flush all data to disk before a migration run. To do so, we'll make use of MigrateDB's
`beforeMigrate` callback.

So go ahead and create `beforeMigrate.sql` in the `/sql` directory:

```sql
CHECKPOINT SYNC;
```

## Triggering the callback

To trigger the execution of the callback, we'll clean and migrate the database again.

So go ahead and invoke `migrate`.

This will give you the following result:

<pre class="console">Database: jdbc:h2:file:./foobardb (H2 1.4)
Successfully cleaned schema "PUBLIC" (execution time 00:00.003s)
Successfully validated 2 migrations (execution time 00:00.010s)
<strong>Executing SQL callback: beforeMigrate</strong>
Creating Schema History table: "PUBLIC"."migratedb_state"
Current version of schema "PUBLIC": << Empty Schema >>
Migrating schema "PUBLIC" to version 1 - Create person table
Migrating schema "PUBLIC" to version 2 - Add people
Successfully applied 2 migrations to schema "PUBLIC" (execution time 00:00.034s)</pre>

As expected we can see that the `beforeMigrate` callback was triggered and executed successfully before the `migrate`
operation. Each time you invoke migrate again in the future, the callback will now be executed again.

## Summary

In this brief tutorial we saw how to

- create callbacks
- triggers the execution of callbacks

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/concepts/callbacks">Read the callback documentation ➡️</a>
</p>
