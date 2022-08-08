---
layout: documentation
menu: api
subtitle: API - First Steps
redirect_from: /getStarted/firststeps/api/
---

# First Steps: API

This brief tutorial will teach **how to get up and running with the MigrateDB API**. It will take you through the
steps on how to configure it and how to write and execute your first few database migrations.

This tutorial should take you about **5 minutes** to complete.

## Prerequisites

- Java 11+
- Maven 3.x

## Creating the project

We're going to create our project using the Maven Archetype Plugin by issuing the following command:
<pre class="console">mvn archetype:generate -B ^
    -DarchetypeGroupId=org.apache.maven.archetypes ^
    -DarchetypeArtifactId=maven-archetype-quickstart ^
    -DarchetypeVersion=1.1 ^
    -DgroupId=foo ^
    -DartifactId=bar ^
    -Dversion=1.0-SNAPSHOT ^
    -Dpackage=foobar</pre>

We are now ready to get started. Let's jump into our project:
<pre class="console">cd bar</pre>

## Adding the dependencies

Let's add MigrateDB and H2 to our new `pom.xml`:

```xml
<project xmlns="...">;
    ...
    <dependencies>
        <dependency>
            <groupId>de.unentscheidbar</groupId>
            <artifactId>migratedb-core</artifactId>
            <version>{{ site.migratedbVersion }}</version>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <version>2.1.212</version>
        </dependency>
        ...
    </dependencies>
    
    <plugins>
        <plugin>
            <groupId>de.unentscheidbar</groupId>
            <artifactId>migratedb-maven-plugin</artifactId>
            <version>{{ site.migratedbVersion }}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>scan</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
    ...
</project>
```

## Integrating MigrateDB

Now it's time to integrate MigrateDB into `src/main/java/foobar/App.java` and point it to our database:

```java
package foobar;

import migratedb.core.MigrateDB;

public class App {
    public static void main(String[] args) {
        // Create the MigrateDB instance and point it to the database
        var migratedb = MigrateDb.configure().dataSource("jdbc:h2:file:./target/foobar", "sa", null).load();

        // Start the migration
        migratedb.migrate();
    }
}
```

## Creating the first migration

We create the migration directory `src/main/resources/db/migration`.

Followed by a first migration called `src/main/resources/db/migration/V1__Create_person_table.sql`:

```sql
create table PERSON (
    ID int not null,
    NAME varchar(100) not null
);
```

## Executing our program

It's now time to execute our program by issuing this command:
<pre class="console"><span>bar&gt;</span> mvn package exec:java -Dexec.mainClass=foobar.App</pre>

If all went well, you should see the following output (timestamps omitted):
<pre class="console">INFO: Creating schema history table: "PUBLIC"."migratedb_state"
INFO: Current version of schema "PUBLIC": &lt;&lt; Empty Schema &gt;&gt;
INFO: Migrating schema "PUBLIC" to version 1 - Create person table
INFO: Successfully applied 1 migration to schema "PUBLIC" (execution time 00:00.062s).</pre>

## Adding a second migration

Now add a second migration called `src/main/resources/db/migration/V2__Add_people.sql`:

```sql
insert into PERSON (ID, NAME) values (1, 'Axel');
insert into PERSON (ID, NAME) values (2, 'Mr. Foo');
insert into PERSON (ID, NAME) values (3, 'Ms. Bar');
```

and execute it by issuing:
<pre class="console"><span>bar&gt;</span> mvn package exec:java -Dexec.mainClass=foobar.App</pre>

We now get:
<pre class="console">INFO: Current version of schema "PUBLIC": 1
INFO: Migrating schema "PUBLIC" to version 2 - Add people
INFO: Successfully applied 1 migration to schema "PUBLIC" (execution time 00:00.090s).</pre>

## Summary

In this brief tutorial we saw how to

- integrate MigrateDB into a project
- configure it so it can talk to our database
- write our first couple of migrations

These migrations were then successfully found and executed.

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/usage/api">Read the documentation ➡️</a>
</p>
