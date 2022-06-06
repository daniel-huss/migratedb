---
layout: commandLine
pill: cli_overview
subtitle: Command-line
redirect_from: /documentation/commandline/
---

# Command-line tool

The MigrateDB command-line tool is a standalone MigrateDB distribution. It is primarily meant for users who wish to
migrate their database from the command-line without having to integrate MigrateDB into their applications nor having to
install a build tool.

## Download and installation

TODO Just link to the Maven Central artifact.

## Directory structure

The MigrateDB download, once extracted, now becomes a directory with the following structure:

<pre class="filetree"><i class="fa fa-folder-open"></i> migratedb-{{site.migratedbVersion}}
  <i class="fa fa-folder-open"></i> conf
    <span><i class="fa fa-file-text"></i> migratedb.conf</span> <i class="fa fa-long-arrow-left"></i> configuration file
  <i class="fa fa-folder"></i> drivers        <i class="fa fa-long-arrow-left" style="margin-left: -3px"></i> JDBC drivers
  <i class="fa fa-folder"></i> jars           <i class="fa fa-long-arrow-left" style="margin-left: -3px"></i> Java-based migrations (as jars)
  <i class="fa fa-folder"></i> jre
  <i class="fa fa-folder"></i> lib
  <i class="fa fa-folder"></i> licenses
  <i class="fa fa-folder"></i> sql            <i class="fa fa-long-arrow-left" style="margin-left: -3px"></i> SQL migrations
  <span><i class="fa fa-file"></i> migratedb</span>        <i class="fa fa-long-arrow-left"></i> macOS/Linux executable
  <span><i class="fa fa-file"></i> migratedb.cmd</span>    <i class="fa fa-long-arrow-left"></i> Windows executable
</pre>

## Usage

<pre class="console"><span>&gt;</span> migratedb [options] command</pre>

## Help flags

The following flags provide helpful information without carrying out any other operations:

<table class="table table-striped">
    <tr>
        <td><code>--help</code><br/><code>-h</code><br/><code>-?</code></td>
        <td>Print the list of available commands and options</td>
    </tr>
    <tr>
        <td><code>--version</code><br/><code>-v</code></td>
        <td>Print the MigrateDB version</td>
    </tr>
</table>

## Commands

<table class="table table-bordered table-hover">
    <thead>
    <tr>
        <th>Name</th>
        <th>Description</th>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td><a href="/documentation/usage/commandline/migrate">migrate</a></td>
        <td>Migrates the database</td>
    </tr>
    <tr>
        <td><a href="/documentation/usage/commandline/clean">clean</a></td>
        <td>Drops all objects in the configured schemas</td>
    </tr>
    <tr>
        <td><a href="/documentation/usage/commandline/info">info</a></td>
        <td>Prints the details and status information about all the migrations</td>
    </tr>
    <tr>
        <td><a href="/documentation/usage/commandline/validate">validate</a></td>
        <td>Validates the applied migrations against the ones available on the classpath</td>
    <tr>
        <td><a href="/documentation/usage/commandline/baseline">baseline</a></td>
        <td>Baselines an existing database, excluding all migrations up to and including baselineVersion</td>
    </tr>
    <tr>
        <td><a href="/documentation/usage/commandline/repair">repair</a></td>
        <td>Repairs the schema history table</td>
    </tr>
    </tbody>
</table>

## JDBC drivers

In order to connect with your database, MigrateDB needs the appropriate JDBC driver to be available in its `drivers`
directory.

To see if MigrateDB ships with the JDBC driver for your database, visit the *Driver* section of the documentation page
for your database. For example, here is the [Oracle Drivers section](/documentation/database/oracle#driver).

If MigrateDB does not ship with the JDBC driver, you will need to download the driver and place it in the `drivers`
directory yourself. Instructions on where to download drivers from are also in the *Driver* section of the documentation
page for each database, under `Maven Central coordinates`.

## Configuration

The MigrateDB Command-line tool can be configured in a wide variety of ways. You can use config files, environment
variables and command-line parameters. These different means of configuration can be combined at will.

### Config files

[Config files](/documentation/configuration/configfile) are supported by the MigrateDB command-line tool. If you are not
familiar with them,
check out the [MigrateDB config file structure and settings reference](/documentation/configuration/configfile) first.

MigrateDB will search for and automatically load the following config files if present:

- `<install-dir>/conf/migratedb.conf`
- `<user-home>/migratedb.conf`
- `<current-dir>/migratedb.conf`

It is also possible to point MigrateDB at one or more additional config files. This is achieved by
supplying the command line parameter `-configFiles=` as follows:

<pre class="console"><span>&gt;</span> migratedb <strong>-configFiles=</strong>path/to/myAlternativeConfig.conf migrate</pre>

To pass in multiple files, separate their names with commas:

<pre class="console"><span>&gt;</span> migratedb <strong>-configFiles</strong>=path/to/myAlternativeConfig.conf,other.conf migrate</pre>

Relative paths are relative to the current working directory. The special option `-configFiles=-` reads from
standard input.

Alternatively you can also use the `MIGRATEDB_CONFIG_FILES` environment variable for this.
When set it will take preference over the command-line parameter.

<pre class="console"><span>&gt;</span> export <strong>MIGRATEDB_CONFIG_FILES</strong>=path/to/myAlternativeConfig.conf,other.conf
<span>&gt;</span> migratedb migrate</pre>

By default MigrateDB loads configuration files using UTF-8. To use an alternative encoding, use the command line
parameter `-configFileEncoding=` as follows:
<pre class="console"><span>&gt;</span> migratedb <strong>-configFileEncoding=</strong>ISO-8859-1 migrate</pre>

Alternatively you can also use the `MIGRATEDB_CONFIG_FILE_ENCODING` environment variable for this.
When set it will take preference over the command-line parameter.

<pre class="console"><span>&gt;</span> export <strong>MIGRATEDB_CONFIG_FILE_ENCODING</strong>=ISO-8859-1</pre>

### Environment Variables

To make it easier to work with cloud and containerized environments, MigrateDB also supports configuration via
[environment variables](/documentation/configuration/envvars). Check out
the [MigrateDB environment variable reference](/documentation/configuration/envvars) for details.

### Command-line Arguments

Finally, MigrateDB can also be configured by passing arguments directly from the command-line:

<pre class="console"><span>&gt;</span> migratedb -user=myuser -schemas=schema1,schema2 -placeholders.keyABC=valueXYZ migrate</pre>

#### A note on escaping command-line arguments

Some command-line arguments will need care as specific characters may be interpreted differently depending on the
shell you are working in. The `url` parameter is particularly affected when it contains extra parameters with
equals `=` and ampersands `&`. For example:

**bash**, **macOS terminal** and **Windows cmd**: use double-quotes:

<pre class="console"><span>&gt;</span> migratedb info -url="jdbc:snowflake://ab12345.snowflakecomputing.com/?db=demo_db&user=foo"</pre>

**Powershell**: use double-quotes inside single-quotes:

<pre class="console"><span>&gt;</span> ./migratedb info -url='"jdbc:snowflake://ab12345.snowflakecomputing.com/?db=demo_db&user=foo"'</pre>

### Configuration from standard input

You can provide configuration options to the standard input of the MigrateDB command line, using the
` -configFiles=-` option. MigrateDB will expect such configuration to be in the same format as a configuration file.

This allows you to compose MigrateDB with other operations. For instance, you can decrypt a config file containing
login credentials and pipe it straight into MigrateDB.

#### Examples

Read a single option from `echo`:
<pre class="console">
<span>&gt;</span> echo $'migratedb.url=jdbc:h2:mem:mydb' | migratedb info -configFiles=-
</pre>

Read multiple options from `echo`, delimited by newlines:
<pre class="console">
<span>&gt;</span> echo $'migratedb.url=jdbc:h2:mem:mydb\nmigratedb.user=sa' | migratedb info -configFiles=-
</pre>

Use `cat` to read a config file and pipe it directly into MigrateDB:
<pre class="console">
<span>&gt;</span> cat migratedb.conf | migratedb migrate -configFiles=-
</pre>

Use `gpg` to encrypt a config file, then pipe it into MigrateDB.

Encrypt the config file:
<pre class="console">
<span>&gt;</span> gpg -e -r "Your Name" migratedb.conf
</pre>

Decrypt the file and pipe it to MigrateDB:
<pre class="console">
<span>&gt;</span> gpg -d -q migratedb.conf.gpg | migratedb info -configFiles=-
</pre>

### Overriding order

The MigrateDB command-line tool has been carefully designed to load and override configuration in a sensible order.

Settings are loaded in the following order (higher items in the list take precedence over lower ones):

1. Command-line arguments
1. Environment variables
1. Standard input
1. Custom config files
1. `<current-dir>/migratedb.conf`
1. `<user-home>/migratedb.conf`
1. `<install-dir>/conf/migratedb.conf`
1. MigrateDB command-line defaults

The means that if for example `migratedb.url` is both present in a config file and passed as `-url=` from the
command-line,
the command-line argument will take precedence and be used.

### Credentials

If you do not supply a database `user` or `password` via any of the means above, you will be
prompted to enter them:
<pre class="console">Database user: myuser
Database password:</pre>

If you want MigrateDB to connect to your database without a user or password, you can suppress prompting by adding
the `-n` flag.

There are exceptions, where the credentials are passed in the JDBC URL or where a password-less method of
authentication is being used.

### Java Arguments

If you need to to pass custom arguments to MigrateDB's JVM, you can do so by setting the `JAVA_ARGS` environment
variable.
They will then automatically be taken into account when launching MigrateDB. This is particularly useful when needing to
set JVM system properties.

## Output

By default, all debug, info and warning output is sent to `stdout`. All errors are sent to `stderr`.

MigrateDB will automatically detect and use any logger class that it finds on its classpath that derives from any of the
following:

- the Apache Commons Logging framework `org.apache.commons.logging.Log` (including Log4j v1)
- SLF4J `org.slf4j.Logger`
- Log4J v2 `org.apache.logging.log4j.Logger`

Alternatively, you can use the [loggers](/documentation/configuration/parameters/loggers) configuration parameter to
specify an exact desired logging framework to use.

The simplest way to make use of MigrateDB's auto-detection is to put all the necessary JAR files in MigrateDB's `lib`
folder and any configuration in the MigrateDB root folder.
For example, if you wished to use `log4j` v2 with the MigrateDB command line, you would achieve this by placing the
log4j JAR files and the corresponding configuration file `log4j2.xml` like this:

<pre class="filetree"><i class="fa fa-folder-open"></i> migratedb-{{site.migratedbVersion}}
  <i class="fa fa-folder"></i> conf
  <i class="fa fa-folder"></i> drivers
  <i class="fa fa-folder"></i> jars
  <i class="fa fa-folder"></i> jre
  <i class="fa fa-folder-open"></i> lib
    <span><i class="fa fa-file-text"></i> log4j-api-2.17.1.jar</span>       <i class="fa fa-long-arrow-left"></i> log4j v2 jar
    <span><i class="fa fa-file-text"></i> log4j-core-2.17.1.jar</span>      <i class="fa fa-long-arrow-left"></i> log4j v2 jar
  <i class="fa fa-folder"></i> licenses
  <i class="fa fa-folder"></i> sql
  <span><i class="fa fa-file"></i> log4j2.xml</span>                   <i class="fa fa-long-arrow-left"></i> log4j configuration
</pre>

Similarly, to use `Logback` add the relevant files like this:

<pre class="filetree"><i class="fa fa-folder-open"></i> migratedb-{{site.migratedbVersion}}
  <i class="fa fa-folder"></i> conf
  <i class="fa fa-folder"></i> drivers
  <i class="fa fa-folder"></i> jars
  <i class="fa fa-folder"></i> jre
  <i class="fa fa-folder-open"></i> lib
    <span><i class="fa fa-file-text"></i> logback-classic.1.1.7.jar</span> <i class="fa fa-long-arrow-left"></i> Logback jar
    <span><i class="fa fa-file-text"></i> logback-core-1.1.7.jar</span>    <i class="fa fa-long-arrow-left"></i> Logback jar
    <span><i class="fa fa-file-text"></i> slf4j-api-1.7.21.jar</span>      <i class="fa fa-long-arrow-left"></i> Logback dependency
  <i class="fa fa-folder"></i> licenses
  <i class="fa fa-folder"></i> sql
  <span><i class="fa fa-file"></i> logback.xml</span>                 <i class="fa fa-long-arrow-left"></i> Logback configuration
</pre>

If you are building MigrateDB into a larger application, this means you do not need to explicitly wire up any logging as
it will auto-detect one of these frameworks.

### P6Spy

P6Spy is another approach to logging which operates at the driver or datasource level, and MigrateDB has integration
with this. You can read about setting it
up [here](https://p6spy.readthedocs.io/en/latest/install.html#generic-instructions) and configuring
it [here](https://p6spy.readthedocs.io/en/latest/configandusage.html#configuration-and-usage).

### Colors

By default the output is automatically colorized if `stdout` is associated with a terminal.

You can override this behavior with the `-color` option. Possible values:

- `auto` (default) : Colorize output, unless `stdout` is not associated with a terminal
- `always` : Always colorize output
- `never` : Never colorize output

### Debug output

Add `-X` to the argument list to also print debug output. If this gives you too much information, you can filter it
with normal command-line tools, for example:

**bash, macOS terminal**

<pre class="console"><span>&gt;</span> migratedb migrate -X <strong>| grep -v 'term-to-filter-out'</strong></pre>

**Powershell**

<pre class="console"><span>&gt;</span> migratedb migrate -X <strong>| sls -Pattern 'term-to-filter-out' -NoMatch</strong></pre>

**Windows cmd**

<pre class="console"><span>&gt;</span> migratedb migrate -X <strong>| findstr /v /c:"term-to-filter-out"</strong></pre>

### Quiet mode

Add `-q` to the argument list to suppress all output, except for errors and warnings.

### Machine-readable output

Add `-outputType=json` to the argument list to print JSON instead of human-readable output. Errors are included in the
JSON payload instead of being sent to `stderr`.

### Writing to a file

Add `-outputFile=/my/output.txt` to the argument list to also write output to the specified file.

<p class="next-steps">
    <a class="btn btn-primary" href="/documentation/usage/commandline/migrate">Command-line: migrate <i class="fa fa-arrow-right"></i></a>
</p>
