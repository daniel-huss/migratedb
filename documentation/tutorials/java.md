---
layout: documentation
menu: java
subtitle: 'Tutorial: Java-based Migrations'
redirect_from:

- /getStarted/java/
- /documentation/getstarted/java/
- /documentation/getstarted/advanced/java/

---

## Introduction

**Java-based migrations** are a great fit for all changes that can not easily be expressed using SQL.

These would typically be things like

- BLOB &amp; CLOB changes
- Advanced bulk data changes (Recalculations, advanced format changes, ...)

## Creating a Java-based migrations

Now let's create a Java-based migration to anonymize the data in the person table.

Start by

- adding the `migratedb-core` dependency to our `pom.xml`
- configuring the Java compiler for Java 11
- configuring MigrateDB to scan the Java classpath for migrations

```xml
<project xmlns="...">
    ...
    <dependencies>
        <dependency>
            <groupId>de.unentscheidbar</groupId>
            <artifactId>migratedb-core</artifactId>
            <version>{{ site.migratedbVersion }}</version>
        </dependency>
        ...
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.7.0</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Now create the migration directory `src/main/java/db/migration`.

Followed by a first migration called `src/main/java/db/migration/V3__Anonymize.java`:

```java
package db.migration;

import migratedb.core.api.migration.BaseJavaMigration;
import migratedb.core.api.migration.Context;

import java.sql.ResultSet;
import java.sql.Statement;

public class V3__Anonymize extends BaseJavaMigration {
    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement()) {
            try (ResultSet rows = select.executeQuery("SELECT id FROM person ORDER BY id")) {
                while (rows.next()) {
                    int id = rows.getInt(1);
                    String anonymizedName = "Anonymous" + id;
                    try (Statement update = context.getConnection().createStatement()) {
                        update.execute("UPDATE person SET name='" + anonymizedName + "' WHERE id=" + id);
                    }
                }
            }
        }
    }
}
```

Finally compile the project using
<pre class="console"><span>bar&gt;</span> mvn compile</pre>

This is now the status

<pre class="console"><span>bar&gt;</span>./migratedb <strong>info</strong>

[INFO] Database: jdbc:h2:file:./target/foobar (H2 1.4)
[INFO]
+-----------+---------+---------------------+------+---------------------+---------+
| Category  | Version | Description         | Type | Installed On        | State   |
+-----------+---------+---------------------+------+---------------------+---------+
| Versioned | 1       | Create person table | SQL  | 2017-12-22 15:26:39 | Success |
| Versioned | 2       | Add people          | SQL  | 2017-12-22 15:28:17 | Success |
| Versioned | 3       | Anonymize           | JDBC |                     | Pending |
+-----------+---------+---------------------+------+---------------------+---------+</pre>

Note the new pending migration of type `JDBC`.

## Executing the migration

It's time to execute our new migration.

So go ahead and invoke

<pre class="console"><span>bar&gt;</span>./migratedb <strong>migrate</strong></pre>

This will give you the following result:

<pre class="console">[INFO] Database: jdbc:h2:file:./target/foobar (H2 1.4)
[INFO] Successfully validated 3 migrations (execution time 00:00.022s)
[INFO] Current version of schema "PUBLIC": 2
[INFO] Migrating schema "PUBLIC" to version 3 - Anonymize
[INFO] Successfully applied 1 migration to schema "PUBLIC" (execution time 00:00.011s)</pre>

And you can check that this is indeed the new status:

<pre class="console"><span>bar&gt;</span>./migratedb <strong>info</strong>

[INFO] Database: jdbc:h2:file:./target/foobar (H2 1.4)
[INFO]
+-----------+---------+---------------------+------+---------------------+---------+
| Category  | Version | Description         | Type | Installed On        | State   |
+-----------+---------+---------------------+------+---------------------+---------+
| Versioned | 1       | Create person table | SQL  | 2017-12-22 15:26:39 | Success |
| Versioned | 2       | Add people          | SQL  | 2017-12-22 15:28:17 | Success |
| Versioned | 3       | Anonymize           | JDBC | 2017-12-22 16:03:37 | Success |
+-----------+---------+---------------------+------+---------------------+---------+</pre>

As expected we can see that the Java-based migration was applied successfully.

## Summary

In this brief tutorial we saw how to

- create Java-based migrations
- configure MigrateDB to load and run them

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/concepts/migrations#java-based-migrations">Read the Java-based migration documentation ➡️</a>
</p>
