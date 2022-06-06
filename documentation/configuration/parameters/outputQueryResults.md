---
layout: documentation
menu: configuration
pill: outputQueryResults
subtitle: migratedb.outputQueryResults
redirect_from: /documentation/configuration/outputQueryResults/
---

# Output Query Results

## Description

Controls whether MigrateDB should output a table with the results of queries when executing migrations.

## Default

true

## Usage

### Command line

```powershell
./migratedb -outputQueryResults="false" info
```

### Configuration File

```properties
migratedb.outputQueryResults=false
```

### Environment Variable

```properties
MIGRATEDB_OUTPUT_QUERY_RESULTS=false
```

### API

```java
MigrateDB.configure()
    .outputQueryResults(false)
    .load()
```

## Use Cases

### Checking the result of your migrations

When developing and testing migrations, you often want to do a sanity check to ensure that they behave and return
expected values. For example, you may have applied some migrations that insert data. You could then also execute a
select query such as:

```
SELECT * FROM my_table
```

With `outputQueryResults` enabled the result of this `SELECT` will be printed for you to inspect and verify before you
continue.
