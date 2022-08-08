---
layout: commandLine
pill: cli_info
subtitle: 'Command-line: info'
---

# Command-line: info

Prints the details and status information about all the migrations.

<a href="/migratedb/documentation/command/info"><img src="/migratedb/assets/balsamiq/command-info.png" alt="info"></a>

## Usage

<pre class="console">migratedb [options] info</pre>

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
migratedb.locations=classpath:com.mycomp.migration,database/migrations,filesystem:/sql-migrations,s3:migrationsBucket,gcs:migrationsBucket
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
migratedb.workingDirectory=C:/myProject
migratedb.jdbcProperties.myProperty=value
```

## Sample output

<pre class="console">&gt; migratedb info

MigrateDB {{ site.migratedbVersion }} 

Database: jdbc:h2:file:migratedb.db (H2 1.3)

+------------+---------+----------------+------+---------------------+---------+
| Category   | Version | Description    | Type | Installed on        | State   |
+------------+---------+----------------+------+---------------------+---------+
| Versioned  | 1       | First          | SQL  |                     | Pending |
| Versioned  | 1.1     | View           | SQL  |                     | Pending |
| Versioned  | 1.2     | Populate table | SQL  |                     | Pending |
+------------+---------+----------------+------+---------------------+---------+</pre>

## Sample JSON output

<pre class="console">&gt; migratedb info -outputType=json

{
  "schemaVersion": null,
  "schemaName": "public",
  "migrations": [
    {
      "category": "Versioned",
      "version": "1",
      "description": "first",
      "type": "SQL",
      "installedOnUTC": "",
      "state": "Pending",
      "filepath": "C:\\migratedb\\sql\\V1__first.sql",
      "installedBy": "",
      "executionTime": 0
    },
    {
      "category": "Repeatable",
      "version": "",
      "description": "repeatable",
      "type": "SQL",
      "installedOnUTC": "",
      "state": "Pending",
      "filepath": "C:\\migratedb\\sql\\R__repeatable.sql",
      "installedBy": "",
      "executionTime": 0
    }
  ],
  "allSchemasEmpty": false,
  "migratedbVersion": "{{ site.migratedbVersion }}",
  "database": "testdb",
  "warnings": [],
  "operation": "info"
}</pre>

## Filtering output

The output from `info` can be filtered to only the parts of the history that you care about using the following
parameters:

- `infoSinceDate`: Limits info to show only migrations applied after this date, and any unapplied migrations. Must be in
  the format `dd/MM/yyyy HH:mm` (e.g. `01/12/2020 13:00`)
- `infoUntilDate`: Limits info to show only migrations applied before this date. Must be in the
  format `dd/MM/yyyy HH:mm` (e.g. `01/12/2020 13:00`)
- `infoSinceVersion`: Limits info to show only migrations greater than or equal to this version, and any repeatable
  migrations. (e.g `1.1`)
- `infoUntilVersion`: Limits info to show only migrations less than or equal to this version, and any repeatable
  migrations. (e.g. `1.1`)
- `infoOfState`: Limits info to show only migrations of the provided state. This is case insensitive. The valid states
  can be found at [Migration States](/migratedb/documentation/concepts/migrations#migration-states).

Example:
<pre class="console">&gt; migratedb info -infoSinceDate="01/12/2020 13:00"
</pre>


<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/usage/commandline/validate">Command-line: validate ➡️</a>
</p>
