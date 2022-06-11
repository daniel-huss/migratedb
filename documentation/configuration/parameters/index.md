---
layout: documentation
menu: configuration
pill: configuration
subtitle: configuration
redirect_from: /documentation/configuration/
---

# Configuration

MigrateDB has many different parameters that can be set to configure its behavior. These parameters can be set through a
variety of different means, depending on how you are using MigrateDB.

## Usage

### Command Line

If using the command line, config parameters can be set via command line arguments (
e.g. `./migratedb -url=jdbc:h2:mem:migratedb info`), [configuration files](/migratedb/documentation/configuration/configfile), or
environment variables (e.g. `MIGRATEDB_URL=jdbc:h2:mem:migratedb`).

### API

If using the API, config parameters can be set via calling methods on the configuration object returned
by `MigrateDB.configure()` (e.g. `MigrateDB.configure().url("jdbc:h2:mem:migratedb").load()`)
, [configuration files](/migratedb/documentation/configuration/configfile).

## Parameters

### Connection

- [url](/migratedb/documentation/configuration/parameters/url)
- [user](/migratedb/documentation/configuration/parameters/user)
- [password](/migratedb/documentation/configuration/parameters/password)
- [driver](/migratedb/documentation/configuration/parameters/driver)
- [connectRetries](/migratedb/documentation/configuration/parameters/connectRetries)
- [connectRetriesInterval](/migratedb/documentation/configuration/parameters/connectRetriesInterval)
- [initSql](/migratedb/documentation/configuration/parameters/initSql)
- [jdbcProperties](/migratedb/documentation/configuration/parameters/jdbcProperties)

### General

- [batch](/migratedb/documentation/configuration/parameters/batch)
- [callbacks](/migratedb/documentation/configuration/parameters/callbacks)
- [cherryPick](/migratedb/documentation/configuration/parameters/cherryPick)
- [configFileEncoding](/migratedb/documentation/configuration/parameters/configFileEncoding)
- [configFiles](/migratedb/documentation/configuration/parameters/configFiles)
- [encoding](/migratedb/documentation/configuration/parameters/encoding)
- [errorOverrides](/migratedb/documentation/configuration/parameters/errorOverrides)
- [group](/migratedb/documentation/configuration/parameters/group)
- [installedBy](/migratedb/documentation/configuration/parameters/installedBy)
- [jarDirs](/migratedb/documentation/configuration/parameters/jarDirs)
- [kerberosConfigFile](/migratedb/documentation/configuration/parameters/kerberosConfigFile)
- [licenseKey](/migratedb/documentation/configuration/parameters/licenseKey)
- [locations](/migratedb/documentation/configuration/parameters/locations)
- [failOnMissingLocations](/migratedb/documentation/configuration/parameters/failOnMissingLocations)
- [lockRetryCount](/migratedb/documentation/configuration/parameters/lockRetryCount)
- [loggers](/migratedb/documentation/configuration/parameters/loggers)
- [mixed](/migratedb/documentation/configuration/parameters/mixed)
- [outOfOrder](/migratedb/documentation/configuration/parameters/outOfOrder)
- [outputQueryResults](/migratedb/documentation/configuration/parameters/outputQueryResults)
- [skipDefaultCallbacks](/migratedb/documentation/configuration/parameters/skipDefaultCallbacks)
- [skipDefaultResolvers](/migratedb/documentation/configuration/parameters/skipDefaultResolvers)
- [skipExecutingMigrations](/migratedb/documentation/configuration/parameters/skipExecutingMigrations)
- [stream](/migratedb/documentation/configuration/parameters/stream)
- [table](/migratedb/documentation/configuration/parameters/table)
- [tablespace](/migratedb/documentation/configuration/parameters/tablespace)
- [target](/migratedb/documentation/configuration/parameters/target)
- [validateMigrationNaming](/migratedb/documentation/configuration/parameters/validateMigrationNaming)
- [validateOnMigrate](/migratedb/documentation/configuration/parameters/validateOnMigrate)
- [workingDirectory](/migratedb/documentation/configuration/parameters/workingDirectory)

### Schema

- [createSchemas](/migratedb/documentation/configuration/parameters/createSchemas)
- [defaultSchema](/migratedb/documentation/configuration/parameters/defaultSchema)
- [schemas](/migratedb/documentation/configuration/parameters/schemas)

### Baseline

- [baselineDescription](/migratedb/documentation/configuration/parameters/baselineDescription)
- [baselineOnMigrate](/migratedb/documentation/configuration/parameters/baselineOnMigrate)
- [baselineVersion](/migratedb/documentation/configuration/parameters/baselineVersion)

### Clean

- [cleanDisabled](/migratedb/documentation/configuration/parameters/cleanDisabled)
- [cleanOnValidationError](/migratedb/documentation/configuration/parameters/cleanOnValidationError)

### Validate

- [ignoreFutureMigrations](/migratedb/documentation/configuration/parameters/ignoreFutureMigrations)
- [ignoreIgnoredMigrations](/migratedb/documentation/configuration/parameters/ignoreIgnoredMigrations)
- [ignoreMissingMigrations](/migratedb/documentation/configuration/parameters/ignoreMissingMigrations)
- [ignorePendingMigrations](/migratedb/documentation/configuration/parameters/ignorePendingMigrations)
- [ignoreMigrationPatterns](/migratedb/documentation/configuration/parameters/ignoreMigrationPatterns)

### Migrations

- [repeatableSqlMigrationPrefix](/migratedb/documentation/configuration/parameters/repeatableSqlMigrationPrefix)
- [resolvers](/migratedb/documentation/configuration/parameters/resolvers)
- [sqlMigrationPrefix](/migratedb/documentation/configuration/parameters/sqlMigrationPrefix)
- [sqlMigrationSeparator](/migratedb/documentation/configuration/parameters/sqlMigrationSeparator)
- [sqlMigrationSuffixes](/migratedb/documentation/configuration/parameters/sqlMigrationSuffixes)
- [baselineMigrationPrefix](/migratedb/documentation/configuration/parameters/baselineMigrationPrefix)

### Placeholders

- [placeholderPrefix](/migratedb/documentation/configuration/parameters/placeholderPrefix)
- [scriptPlaceholderPrefix](/migratedb/documentation/configuration/parameters/scriptPlaceholderPrefix)
- [placeholderReplacement](/migratedb/documentation/configuration/parameters/placeholderReplacement)
- [placeholders](/migratedb/documentation/configuration/parameters/placeholders)
- [placeholderSeparator](/migratedb/documentation/configuration/parameters/placeholderSeparator)
- [placeholderSuffix](/migratedb/documentation/configuration/parameters/placeholderSuffix)
- [scriptPlaceholderSuffix](/migratedb/documentation/configuration/parameters/scriptPlaceholderSuffix)
