---
layout: commandLine
pill: cli_clean
subtitle: 'Command-line: clean'
---

# Command-line: clean

Drops all objects (tables, views, procedures, triggers, ...) in the configured schemas.<br/>
The schemas are cleaned in the order specified by the `schemas` property.

<a href="/migratedb/documentation/command/clean"><img src="/migratedb/assets/balsamiq/command-clean.png" alt="clean"></a>

## Usage

<pre class="console"><span>&gt;</span> migratedb [options] clean</pre>

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
migratedb.callbacks=com.mycomp.project.CustomCallback,com.mycomp.project.AnotherCallback
migratedb.skipDefaultCallbacks=false
migratedb.cleanDisabled=false
migratedb.workingDirectory=C:/myProject
migratedb.jdbcProperties.myProperty=value
```

## Sample output

<pre class="console">&gt; migratedb clean

MigrateDB {{ site.migratedbVersion }} 

Cleaned database schema 'PUBLIC' (execution time 00:00.014s)</pre>

## Sample JSON output

<pre class="console">&gt; migratedb clean -outputType=json

{
  "schemasCleaned": [
    "public"
  ],
  "schemasDropped": [],
  "migratedbVersion": "{{ site.migratedbVersion }}",
  "database": "testdb",
  "warnings": [],
  "operation": "clean"
}</pre>

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/usage/commandline/info">Command-line: info <i class="fa fa-arrow-right"></i></a>
</p>
