---
layout: api
pill: hooks
subtitle: API hooks
---

# API: hooks

There are three ways you can hook into the MigrateDB API.

## Java-based Migrations

The first one is the the most common
one: [Java-based Migrations](/migratedb/documentation/concepts/migrations#java-based-migrations)
when you need more power than SQL can offer you. This is great to for dealing with LOBs or performing advanced
data transformations.

In order to be picked up by MigrateDB, Java-based Migrations must implement the
[`JavaMigration`](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/migration/JavaMigration) interface. Most
users
however should inherit from the convenience
class [`BaseJavaMigration`](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/migration/BaseJavaMigration)
instead as it encourages MigrateDB's default naming convention, enabling MigrateDB to automatically extract the version
and
the description from the class name.

### Java-based migrations as Spring Beans

By default Java-based migrations discovered through classpath scanning and instantiated by MigrateDB. In a dependency
injection container it is sometimes useful to let the container instantiate the class and wire up its dependencies for
you.

The MigrateDB API lets you pass pre-instantiated Java-based migrations using the `javaMigrations` property.

Spring users can use this to automatically use all `JavaMigration` Spring beans with MigrateDB:

```java
import migratedb.core.MigrateDB;
import migratedb.core.api.migration.JavaMigration;
import org.springframework.context.ApplicationContext;

...
ApplicationContext applicationContext = ...; // obtain a reference to Spring's ApplicationContext.

MigrateDB migratedb = MigrateDB.configure()
    .dataSource(url, user, password)
    // Add all Spring-instantiated JavaMigration beans
    .javaMigrations(applicationContext.getBeansOfType(JavaMigration.class).values().toArray(new JavaMigration[0]))
    .load();
migratedb.migrate();
```

## Java-based Callbacks

Building upon that are the Java-based [Callbacks](/migratedb/documentation/concepts/callbacks)
when you need more power or flexibility in a Callback than SQL can offer you.

They can be created by implementing the 
[**Callback**](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/callback/Callback)
interface.

The `event` argument tells you
which [`Event`](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/callback/Event)
(`beforeClean`, `afterMigrate`, ...) is being handled and the `context` argument gives you access to things
like the database connection and the MigrateDB configuration.

It is possible for a Java callback to handle multiple events; for example, if you wanted to write a callback to
fire off a notification to a third party service at the end of a migration, whether successful or not, and didn't
want to duplicate the code, then you could achieve this by handling both `afterMigrate` and `afterMigrateError`:

```java
public class MyNotifierCallback implements Callback {
    
    // Ensures that this callback handles both events
    @Override
    public boolean supports(Event event, Context context) {
        return event.equals(Event.AFTER_MIGRATE) || event.equals(Event.AFTER_MIGRATE_ERROR);
    }
    
    // Not relevant if we don't interact with the database
    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }
    
    // Send a notification when either event happens.
    @Override
    public void handle(Event event, Context context) {
        String notification = event.equals(Event.AFTER_MIGRATE) ? "Success" : "Failed";
        // ... Notification logic ...
        notificationService.send(notification);
    }

    String getCallbackName() {
        return "MyNotifier";
    }
}
``` 

In order to be picked up by MigrateDB, Java-based Callbacks must implement the Callback interface.
MigrateDB will automatically scan for and load all callbacks found in the `db/callback` package. Additional callback
classes or scan locations can be specified by the `migratedb.callbacks` configuration property.

## Custom Migration resolvers &amp; executors

For those that need more than what the SQL and Java-based migrations offer, you also have the possibility to
implement your
own [`MigrationResolver`](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/resolver/MigrationResolver)
coupled with a
custom [`MigrationExecutor`](/migratedb/documentation/usage/api/javadoc/migratedb/core/api/executor/MigrationExecutor).

These can then be used for loading things like CSV-based migrations or other custom formats.

By using the `skipDefaultResolvers` property, these custom resolvers can also be used
to completely replace the built-in ones (by default, custom resolvers will run in addition to
built-in ones).

<p class="next-steps">
    <a class="btn btn-primary" href="/migratedb/documentation/usage/api/javadoc">API: Javadoc <i class="fa fa-arrow-right"></i></a>
</p>
