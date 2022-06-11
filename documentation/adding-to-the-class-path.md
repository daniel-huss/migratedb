---
layout: documentation
pill: addingtotheclasspath
subtitle: Adding to the classpath
---

# Adding to the classpath

MigrateDB ships with support for a large number of databases and functionality, but due to limitations (such as
licensing) it cannot ship containing everything it supports. In these situations, MigrateDB will load the extra
support/functionality if it is present on the classpath.

## How to add to the classpath

How you add to the classpath depends on how you are invoking MigrateDB.

### Command Line

When using the CLI, you can add to the classpath by dropping the `.jar` files for the libraries you want to include into
either the `drivers` or the `jars` folder in the downloaded folder structure. The two folders are provided to make it
easier to separate jars used for adding database driver support, and jars used to contain other functionality (such
as [Java migrations](/migratedb/documentation/concepts/migrations#java-based-migrations)
or [Java callbacks](/migratedb/documentation/concepts/callbacks#java-callbacks)).

<pre class="filetree"><i class="fa fa-folder-open"></i> migratedb
  <i class="fa fa-folder-open"></i> conf
  <i class="fa fa-folder-open"></i> drivers <i class="fa fa-long-arrow-left"></i> <code>here</code>
  <i class="fa fa-folder-open"></i> jars    <i class="fa fa-long-arrow-left"></i> <code>or here</code>
  <i class="fa fa-folder-open"></i> jre
  <i class="fa fa-folder-open"></i> lib
  <i class="fa fa-folder-open"></i> licenses
  <i class="fa fa-folder-open"></i> sql
  <i class="fa fa-file"></i> migratedb
  <i class="fa fa-file"></i> migratedb.cmd
  <i class="fa fa-file-text"></i> README.txt
</pre>

You can also specify more folders to load jars from using the [jarDirs](/migratedb/documentation/configuration/parameters/jarDirs)
configuration parameter.

### API

When using the API, the jars you wish to include should be added as dependencies of the overall project, just as you
would with any other java dependencies.

## What can be added

The most common library to be added to MigrateDB is those that add JDBC driver support. For example
the [Informix](/migratedb/documentation/database/informix) database is supported by MigrateDB, but the JDBC driver is not shipped
with it. Therefore the `com.ibm.informix:jdbc:4.10.10.0` dependency needs to be added to the classpath to allow
MigrateDB to work with it. See each database page for the JDBC driver they use and whether they are shipped with
MigrateDB or not.

Other uses for adding libraries are [adding logging support](/migratedb/documentation/usage/commandline/#output),
adding [Java migrations](/migratedb/documentation/concepts/migrations#java-based-migrations), and more.