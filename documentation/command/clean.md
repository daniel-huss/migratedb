---
layout: documentation
menu: clean
subtitle: Clean
---

# Clean

Drops all objects in the configured schemas.

![Clean](/assets/balsamiq/command-clean.png)

Clean is a great help in development and test. It will effectively give you a fresh start, by wiping your configured
schemas completely clean. All objects (tables, views, procedures, ...) will be dropped.

Needless to say: **do not use against your production DB!**

## Usage

See [configuration](/migratedb/documentation/configuration/parameters/#clean) for clean specific configuration parameters.
{% include commandUsage.html command="clean" %}

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/command/info">Info <i class="fa fa-arrow-right"></i></a>
</p>
