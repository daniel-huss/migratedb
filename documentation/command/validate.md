---
layout: documentation
menu: validate
subtitle: Validate
---

# Validate

Validates the applied migrations against the available ones.

![Validate](/assets/balsamiq/command-validate.png)

Validate helps you verify that the migrations applied to the database match the ones available locally.

This is very useful to detect accidental changes that may prevent you from reliably recreating the schema.

Validate works by storing a checksum when a migration is executed. The validate mechanism checks if the migration
locally still has the same checksum as the migration already executed in the database.

## Custom validation rules

As the lifetime of a project increases, there will inevitably be hotfixes, deleted migrations and other changes that
break the conventions of MigrateDB’s validation.

In these cases you need a way to tell MigrateDB that these migrations are valid.

<a class="btn btn-primary" href="custom-validate-rules">Learn more about custom validate rules</a>

## Usage

See [configuration](/migratedb/documentation/configuration/parameters/#validate) for validate specific configuration parameters.
{% include commandUsage.html command="validate" %}
