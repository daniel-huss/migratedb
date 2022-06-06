---
layout: commandLine
pill: cli_baseline
subtitle: 'Command-line: baseline'
---

# Command-line: baseline

Baselines an existing database, excluding all migrations up to and including `baselineVersion`.

<a href="/documentation/command/baseline"><img src="/assets/balsamiq/command-baseline.png" alt="baseline"></a>

## Usage

<pre class="console"><span>&gt;</span> migratedb [options] baseline</pre>

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
migratedb.tablespace=my_tablespace
migratedb.callbacks=com.mycomp.project.CustomCallback,com.mycomp.project.AnotherCallback
migratedb.skipDefaultCallbacks=false
migratedb.baselineVersion=1.0
migratedb.baselineDescription=Base Migration
migratedb.workingDirectory=C:/myProject
migratedb.createSchemas=true
migratedb.jdbcProperties.myProperty=value
```

## Sample output

<pre class="console">&gt; migratedb baseline

MigrateDB {{ site.migratedbVersion }} 

Creating schema history table: "PUBLIC"."migratedb_state"
Schema baselined with version: 1</pre>

## Sample JSON output

<pre class="console">&gt; migratedb baseline -outputType=json

{
  "successfullyBaselined": true,
  "baselineVersion": "1",
  "migratedbVersion": "{{ site.migratedbVersion }}",
  "database": "testdb",
  "warnings": [],
  "operation": "baseline"
}</pre>

<p class="next-steps">
    <a class="btn btn-primary" href="/documentation/usage/commandline/repair">Command-line: repair <i class="fa fa-arrow-right"></i></a>
</p>
