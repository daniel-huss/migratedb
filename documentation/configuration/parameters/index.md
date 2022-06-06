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
e.g. `./migratedb -url=jdbc:h2:mem:migratedb info`), [configuration files](/documentation/configuration/configfile), or
environment variables (e.g. `MIGRATEDB_URL=jdbc:h2:mem:migratedb`).

### API

If using the API, config parameters can be set via calling methods on the configuration object returned
by `MigrateDB.configure()` (e.g. `MigrateDB.configure().url("jdbc:h2:mem:migratedb").load()`)
, [configuration files](/documentation/configuration/configfile).

## Parameters

### Connection

- [url](/documentation/configuration/parameters/url)
- [user](/documentation/configuration/parameters/user)
- [password](/documentation/configuration/parameters/password)
- [driver](/documentation/configuration/parameters/driver)
- [connectRetries](/documentation/configuration/parameters/connectRetries)
- [connectRetriesInterval](/documentation/configuration/parameters/connectRetriesInterval)
- [initSql](/documentation/configuration/parameters/initSql)
- [jdbcProperties](/documentation/configuration/parameters/jdbcProperties)

### General

- [batch](/documentation/configuration/parameters/batch)
- [callbacks](/documentation/configuration/parameters/callbacks)
- [cherryPick](/documentation/configuration/parameters/cherryPick)
- [configFileEncoding](/documentation/configuration/parameters/configFileEncoding)
- [configFiles](/documentation/configuration/parameters/configFiles)
- [dryRunOutput](/documentation/configuration/parameters/dryRunOutput)
- [encoding](/documentation/configuration/parameters/encoding)
- [errorOverrides](/documentation/configuration/parameters/errorOverrides)
- [group](/documentation/configuration/parameters/group)
- [installedBy](/documentation/configuration/parameters/installedBy)
- [jarDirs](/documentation/configuration/parameters/jarDirs)
- [kerberosConfigFile](/documentation/configuration/parameters/kerberosConfigFile)
- [licenseKey](/documentation/configuration/parameters/licenseKey)
- [locations](/documentation/configuration/parameters/locations)
- [failOnMissingLocations](/documentation/configuration/parameters/failOnMissingLocations)
- [lockRetryCount](/documentation/configuration/parameters/lockRetryCount)
- [loggers](/documentation/configuration/parameters/loggers)
- [mixed](/documentation/configuration/parameters/mixed)
- [outOfOrder](/documentation/configuration/parameters/outOfOrder)
- [outputQueryResults](/documentation/configuration/parameters/outputQueryResults)
- [skipDefaultCallbacks](/documentation/configuration/parameters/skipDefaultCallbacks)
- [skipDefaultResolvers](/documentation/configuration/parameters/skipDefaultResolvers)
- [skipExecutingMigrations](/documentation/configuration/parameters/skipExecutingMigrations)
- [stream](/documentation/configuration/parameters/stream)
- [table](/documentation/configuration/parameters/table)
- [tablespace](/documentation/configuration/parameters/tablespace)
- [target](/documentation/configuration/parameters/target)
- [validateMigrationNaming](/documentation/configuration/parameters/validateMigrationNaming)
- [validateOnMigrate](/documentation/configuration/parameters/validateOnMigrate)
- [workingDirectory](/documentation/configuration/parameters/workingDirectory)

### Schema

- [createSchemas](/documentation/configuration/parameters/createSchemas)
- [defaultSchema](/documentation/configuration/parameters/defaultSchema)
- [schemas](/documentation/configuration/parameters/schemas)

### Baseline

- [baselineDescription](/documentation/configuration/parameters/baselineDescription)
- [baselineOnMigrate](/documentation/configuration/parameters/baselineOnMigrate)
- [baselineVersion](/documentation/configuration/parameters/baselineVersion)

### Clean

- [cleanDisabled](/documentation/configuration/parameters/cleanDisabled)
- [cleanOnValidationError](/documentation/configuration/parameters/cleanOnValidationError)

### Validate

- [ignoreFutureMigrations](/documentation/configuration/parameters/ignoreFutureMigrations)
- [ignoreIgnoredMigrations](/documentation/configuration/parameters/ignoreIgnoredMigrations)
- [ignoreMissingMigrations](/documentation/configuration/parameters/ignoreMissingMigrations)
- [ignorePendingMigrations](/documentation/configuration/parameters/ignorePendingMigrations)
- [ignoreMigrationPatterns](/documentation/configuration/parameters/ignoreMigrationPatterns)

### Migrations

- [repeatableSqlMigrationPrefix](/documentation/configuration/parameters/repeatableSqlMigrationPrefix)
- [resolvers](/documentation/configuration/parameters/resolvers)
- [sqlMigrationPrefix](/documentation/configuration/parameters/sqlMigrationPrefix)
- [sqlMigrationSeparator](/documentation/configuration/parameters/sqlMigrationSeparator)
- [sqlMigrationSuffixes](/documentation/configuration/parameters/sqlMigrationSuffixes)
- [baselineMigrationPrefix](/documentation/configuration/parameters/baselineMigrationPrefix)

### Placeholders

- [placeholderPrefix](/documentation/configuration/parameters/placeholderPrefix)
- [scriptPlaceholderPrefix](/documentation/configuration/parameters/scriptPlaceholderPrefix)
- [placeholderReplacement](/documentation/configuration/parameters/placeholderReplacement)
- [placeholders](/documentation/configuration/parameters/placeholders)
- [placeholderSeparator](/documentation/configuration/parameters/placeholderSeparator)
- [placeholderSuffix](/documentation/configuration/parameters/placeholderSuffix)
- [scriptPlaceholderSuffix](/documentation/configuration/parameters/scriptPlaceholderSuffix)
