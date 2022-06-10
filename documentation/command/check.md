---
layout: documentation
menu: check
subtitle: Check
---

# Check

Produces reports to increase confidence in your migrations.

MigrateDB migrates against a temporary database and compares this against the target database in order to generate a
report.

#### Flags:

_One or more flags must be present_

| Parameter                    | Description
| ---------------------------- | --------------------------------------------------------------
|    -changes                  |  Include pending changes that will be applied to the database

#### Configuration parameters:

_Format: -key=value_

| Parameter                    | Description
| ---------------------------- | -----------------------------------------------------------
|    url                       | **[REQUIRED]** URL of the target database
|    user                      | **[REQUIRED]** Username for the target database
|    password                  | **[REQUIRED]** Password for the target database
|    check.tempUrl             | **[REQUIRED]** URL of the temporary database
|    check.tempUsername        | Username for the temporary database. Defaults to 'migratedb.user'
|    check.tempPassword        | Password for the temporary database. Defaults to 'migratedb.password'
|    check.outputLocation      | Destination folder for reports. Defaults to the current directory
|    check.outputTypes         | Format of reports. Options are: 'json', and 'html'. Defaults to 'json'

#### Usage Example:

```
migratedb check -changes -url=jdbc:example:database -user=username -password=password -check.tempUrl=jdbc:example:tempdatabase -check.outputTypes=html,json
```

#### Requirements

- .NET 6 is required in order to generate reports. You can download it
  from [here](https://dotnet.microsoft.com/en-us/download/dotnet/6.0).
