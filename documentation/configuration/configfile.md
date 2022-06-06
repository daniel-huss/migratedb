---
layout: documentation
menu: configFiles
subtitle: Config Files
redirect_from: /documentation/configfiles/
---

# Config Files

MigrateDB supports loading configuration via config files.

## Loading

By default MigrateDB will load configuration files from the following locations:

- <i>installDir<i>/conf/migratedb.conf
- <i>userhome<i>/migratedb.conf
- <i>workingDir<i>/migratedb.conf

Additionally you can make MigrateDB load other configurations using
the [configFiles](/documentation/configuration/parameters/configFiles) configuration parameter.

## Structure

Config files have the following structure:

```properties
# Settings are simple key-value pairs
migratedb.key=value
# Single line comment start with a hash
# Long properties can be split over multiple lines by ending each line with a backslash
migratedb.locations=filesystem:my/really/long/path/folder1,\
filesystem:my/really/long/path/folder2,\
filesystem:my/really/long/path/folder3
# These are some example settings
migratedb.url=jdbc:mydb://mydatabaseurl
migratedb.schemas=schema1,schema2
migratedb.placeholders.keyABC=valueXYZ
```

### Environment variable substitution

Environment variables in config files are substituted:

```properties
migratedb.placeholders.abc=${ABC}
```

If an environment variable isn't set, an empty value is assumed.

## Reference

See [configuration](/documentation/configuration/parameters) for a full list of supported configuration parameters.

<p class="next-steps">
  <a class="btn btn-primary" href="/documentation/configuration/envvars">Environment Variables <i class="fa fa-arrow-right"></i></a>
</p>
