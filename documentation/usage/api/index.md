---
layout: api
pill: api_overview
subtitle: API
redirect_from: /documentation/api/
---

# API

MigrateDB brings the largest benefits when **integrated within an application**. By integrating MigrateDB
you can ensure that the application and its **database will always be compatible**, with no manual
intervention required. MigrateDB checks the version of the database and applies new migrations automatically
**before** the rest of the application starts. This is important, because the database must first
be migrated to a state the rest of the code can work with.

## Supported Java Versions

- `Java 11+`

## The MigrateDb Class

The central piece of MigrateDB's database migration infrastructure is the
**`migratedb.core.MigrateDb`**
class. It is your **one-stop shop** for working with MigrateDB programmatically. It serves both as a
**configuration** and a **launching** point for all of MigrateDB's functions.

### Programmatic Configuration (Java)

MigrateDB is super easy to use programmatically:

```java
import migratedb.core.MigrateDb;

...
MigrateDb migratedb = MigrateDb.configure().dataSource(url, user, password).load();
migratedb.migrate();

// Start the rest of the application
...
```

See [configuration](/migratedb/documentation/configuration/parameters) for a full list of supported configuration parameters.

### JDBC Drivers

You will need to include the relevant JDBC driver for your chosen database as a dependency in your Java project.
For instance in your `pom.xml` for a Maven project. The version of the JDBC driver supported for each database is
specified in the 'Supported Databases' list in the left hand side navigation menu.

### Spring Configuration

As an alternative to the programmatic configuration, here is how you can configure and start MigrateDB in a classic
Spring application using XML bean configuration:

```xml
<bean id="migratedbConfig" class="migratedb.core.api.configuration.ClassicConfiguration">
    <property name="dataSource" ref="..."/>
    ...
</bean>

<bean id="migratedb" class="migratedb.core.MigrateDB" init-method="migrate">
    <constructor-arg ref="migratedbConfig"/>
</bean>

<!-- The rest of the application (incl. Hibernate) -->
<!-- Must be run after MigrateDB to ensure the database is compatible with the code -->
<bean id="sessionFactory" class="..." depends-on="migratedb">
    ...
</bean>
```

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/usage/api/hooks">API: Hooks <i class="fa fa-arrow-right"></i></a>
</p>


