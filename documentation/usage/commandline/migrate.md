---
layout: commandLine
pill: cli_migrate
subtitle: 'Command-line: migrate'
---

# Command-line: migrate

Migrates the schema to the latest version. MigrateDB will create the schema history table automatically if it doesn't
exist.

<a href="/migratedb/documentation/command/migrate"><img src="/migratedb/assets/balsamiq/command-migrate.png" alt="migrate"></a>

## Usage

<pre class="console"><span>&gt;</span> migratedb [options] migrate</pre>

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
migratedb.defaultSchema=schema1
migratedb.schemas=schema1,schema2,schema3
migratedb.createSchemas=true
migratedb.table=schema_history
migratedb.tablespace=my_tablespace
migratedb.locations=classpath:com.mycomp.migration,database/migrations,filesystem:/sql-migrations,s3:migrationsBucket,gcs:migrationsBucket
migratedb.sqlMigrationPrefix=Migration-
migratedb.repeatableSqlMigrationPrefix=RRR
migratedb.sqlMigrationSeparator=__
migratedb.sqlMigrationSuffixes=.sql,.pkg,.pkb
migratedb.stream=true
migratedb.batch=true
migratedb.encoding=ISO-8859-1
migratedb.placeholderReplacement=true
migratedb.placeholders.aplaceholder=value
migratedb.placeholders.otherplaceholder=value123
migratedb.placeholderPrefix=#[
migratedb.placeholderSuffix=]
migratedb.resolvers=com.mycomp.project.CustomResolver,com.mycomp.project.AnotherResolver
migratedb.skipDefaultCallResolvers=false
migratedb.callbacks=com.mycomp.project.CustomCallback,com.mycomp.project.AnotherCallback
migratedb.skipDefaultCallbacks=false
migratedb.target=5.1
migratedb.outOfOrder=false
migratedb.outputQueryResults=false
migratedb.validateOnMigrate=true
migratedb.cleanOnValidationError=false
migratedb.mixed=false
migratedb.group=false
migratedb.ignoreMissingMigrations=false
migratedb.ignoreIgnoredMigrations=false
migratedb.ignoreFutureMigrations=false
migratedb.cleanDisabled=false
migratedb.baselineOnMigrate=false
migratedb.installedBy=my-user
migratedb.errorOverrides=99999:17110:E,42001:42001:W
migratedb.lockRetryCount=10
migratedb.oracle.sqlplus=true
migratedb.oracle.sqlplusWarn=true
migratedb.workingDirectory=C:/myProject
migratedb.jdbcProperties.myProperty=value
```

## Sample output

<pre class="console">&gt; migratedb migrate

MigrateDB {{ site.migratedbVersion }} 

Database: jdbc:h2:file:migratedb.db (H2 1.3)
Successfully validated 5 migrations (execution time 00:00.010s)
Creating Schema History table: "PUBLIC"."migratedb_state"
Current version of schema "PUBLIC": << Empty Schema >>
Migrating schema "PUBLIC" to version 1 - First
Migrating schema "PUBLIC" to version 1.1 - View
Successfully applied 2 migrations to schema "PUBLIC" (execution time 00:00.030s).</pre>

## Sample JSON output

<pre class="console">&gt; migratedb migrate -outputType=json

{
  "initialSchemaVersion": null,
  "targetSchemaVersion": "1",
  "schemaName": "public",
  "migrations": [
    {
      "category": "Versioned",
      "version": "1",
      "description": "first",
      "type": "SQL",
      "filepath": "C:\\migratedb\\sql\\V1__first.sql",
      "executionTime": 0
    },
    {
      "category": "Repeatable",
      "version": "",
      "description": "repeatable",
      "type": "SQL",
      "filepath": "C:\\migratedb\\sql\\R__repeatable.sql",
      "executionTime": 0
    }
  ],
  "migrationsExecuted": 2,
  "migratedbVersion": "{{ site.migratedbVersion }}",
  "database": "testdb",
  "warnings": [],
  "operation": "migrate"
}</pre>

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/usage/commandline/clean">Command-line: clean <i class="fa fa-arrow-right"></i></a>
</p>
