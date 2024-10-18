---
layout: documentation
menu: configFiles
subtitle: Config Files
redirect_from: /documentation/configfiles/
---

# Config Files

MigrateDB supports loading configuration via config files.

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

## Reference

See [configuration](/migratedb/documentation/configuration/parameters) for a full list of supported configuration parameters.
