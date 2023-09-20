---
layout: commandLine
pill: cli_validate
subtitle: 'Command-line: validate'
---

# Command-line: validate

Validate applied migrations against resolved ones (on the filesystem or classpath)
to detect accidental changes that may prevent the schema(s) from being recreated exactly.

Validation fails if

- differences in migration names, types or checksums are found
- versions have been applied that aren't resolved locally anymore
- versions have been resolved that haven't been applied yet

<a href="/migratedb/documentation/command/validate"><img src="/migratedb/assets/balsamiq/command-validate.png" alt="validate"></a>

## Usage

<pre class="console">migratedb [options] validate</pre>

## Options

See [configuration](/migratedb/documentation/configuration/parameters) for a full list of supported configuration parameters.

## Sample configuration

```properties
migratedb.driver=org.hsqldb.jdbcDriver
migratedb.url=jdbc:hsqldb:file:/db/migratedb_sample
migratedb.user=SA
migratedb.password=mySecretPwd
migratedb.connectRetries=10
migratedb.initSql=SET ROLE 'myuser'
migratedb.schemas=schema1,schema2,schema3
migratedb.table=schema_history
migratedb.locations=classpath:com.mycomp.migration,database/migrations,filesystem:/sql-migrations
migratedb.sqlMigrationPrefix=Migration-
migratedb.repeatableSqlMigrationPrefix=RRR
migratedb.sqlMigrationSeparator=__
migratedb.sqlMigrationSuffixes=.sql,.pkg,.pkb
migratedb.encoding=ISO-8859-1
migratedb.placeholderReplacement=true
migratedb.placeholders.aplaceholder=value
migratedb.placeholders.otherplaceholder=value123
migratedb.placeholderPrefix=#[
migratedb.placeholderSuffix=]
migratedb.resolvers=com.mycomp.project.CustomResolver,com.mycomp.project.AnotherResolver
migratedb.skipDefaultResolvers=false
migratedb.callbacks=com.mycomp.project.CustomCallback,com.mycomp.project.AnotherCallback
migratedb.skipDefaultCallbacks=false
migratedb.target=5.1
migratedb.outOfOrder=false
migratedb.cleanOnValidationError=false
migratedb.ignoreMissingMigrations=false
migratedb.ignoreIgnoredMigrations=false
migratedb.ignorePendingMigrations=false
migratedb.ignoreFutureMigrations=false
migratedb.oracle.sqlplus=true
migratedb.oracle.sqlplusWarn=true
migratedb.workingDirectory=C:/myProject
migratedb.jdbcProperties.myProperty=value
```

## Sample output

<pre class="console">&gt; migratedb validate

MigrateDB {{ site.migratedbReleaseVersion }} 

No migrations applied yet. No validation necessary.</pre>

## Sample JSON output

<pre class="console">&gt; migratedb validate -outputType=json

{
  "errorDetails": null,
  "invalidMigrations": [],
  "validationSuccessful": true,
  "validateCount": 2,
  "migratedbReleaseVersion": "{{ site.migratedbReleaseVersion }}",
  "database": "testdb",
  "warnings": [],
  "operation": "validate"
}</pre>
