---
layout: documentation
menu: tut_customvalidationrules
subtitle: 'Tutorial: Custom Validation Rules'
---

# Tutorial: Custom Validation Rules

This brief tutorial will teach you **how to customize your validation rules**.

## Introduction

MigrateDB validates your migrations according to its own conventions, however as the lifetime of a project increases
there will inevitably be hotfixes, deleted migrations and other changes that break the conventions of MigrateDB's
validation.

`ignoreMigrationPatterns` let's you add your own validation rules to tell MigrateDB which migrations are valid.

You can read about how to
configure `ignoreMigrationPatterns` [here](/migratedb/documentation/configuration/parameters/ignoreMigrationPatterns). In
summary, `ignoreMigrationPatterns` allows you to specify a list of patterns of the form `type:status` and any migration
that matches any of these patterns is ignored during validate.

You can see a video showing how to use `ignoreMigrationPatterns`
parameter [here](/blog/customize-validation-rules-with-ignoremigrationpatterns).

## Example: Ignore missing repeatable migrations and pending versioned migrations

Let's assume our schema history table is as follows:

```
+------------+---------+-------------+------+--------------+---------+
| Category   | Version | Description | Type | Installed On | State   |
+------------+---------+-------------+------+--------------+---------+
| Repeatable |         | repeatable  | SQL  |      ...     | Missing |
| Versioned  | 1       | first       | SQL  |              | Pending |
+------------+---------+-------------+------+--------------+---------+
```

We have a missing repeatable migration `repeatable` and a pending versioned migration `first`.
Running `validate` will fail for both of these migrations, erroring that there is
a `Detected applied migration not resolved locally` and `Detected resolved migration not applied to database`.

While the default behavior of validate here causes an error, you may not want to error in this scenario.

What if `repeatable` was deleted intentionally? This can be the case when it is infeasible to keep every migration. In
particular, you might delete repeatable migrations but not versioned migrations, and you need a way for validate to
reflect this. In the case of `first`, if you are validating before applying new migrations then you don't want to fail
on any pending migrations. Instead, you want to ensure that migrations that have been applied up to now can be
successfully validated instead.

Achieving the desired result only requires passing a list of patterns to `ignoreMigrationPatterns` with the
value `repeatable:missing,versioned:pending`. `validate` will no longer fail for missing repeatable migrations
or pending versioned migrations.

## Summary

In this brief tutorial we saw how to:

- Configure `ignoreMigrationPatterns` to ignore missing repeatable migrations and pending versioned migrations
