---
layout: commandLine
pill: cli_repair
subtitle: 'Command-line: repair'
---

# Command-line: repair

Repairs the MigrateDB schema history table. This will perform the following actions:

- Remove any failed migrations on databases without DDL transactions<br/>
  (User objects left behind must still be cleaned up manually)
- Realign the checksums, descriptions and types of the applied migrations with the ones of the available migrations

<a href="/documentation/command/repair"><img src="/assets/balsamiq/command-repair.png" alt="repair"></a>

## Usage

<pre class="console"><span>&gt;</span> migratedb [options] repair</pre>

## Options

See [configuration](/documentation/configuration/parameters) for a full list of supported configuration parameters.

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
migratedb.workingDirectory=C:/myProject
migratedb.jdbcProperties.myProperty=value
```

## Sample output

<pre class="console">&gt; migratedb repair

MigrateDB {{ site.migratedbVersion }} 

Repair not necessary. No failed migration detected.</pre>

## Sample JSON output

<pre class="console">&gt; migratedb repair -outputType=json

{
  "repairActions": [
    "ALIGNED APPLIED MIGRATION CHECKSUMS"
  ],
  "migrationsRemoved": [],
  "migrationsDeleted": [],
  "migrationsAligned": [
    {
      "version": "1",
      "description": "first",
      "filepath": "C:\\migratedb\\sql\\V1__first.sql"
    }
  ],
  "migratedbVersion": "{{ site.migratedbVersion }}",
  "database": "testdb",
  "warnings": [],
  "operation": "repair"
}</pre>

<p class="next-steps">
    <a class="btn btn-primary" href="/documentation/usage/api">API <i class="fa fa-arrow-right"></i></a>
</p>
