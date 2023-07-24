package migratedb.v1.spring.boot.v3.autoconfig;

import migratedb.v1.core.MigrateDb;

/**
 * Invokes {@code repair} before migrating, for self-healing application deployments.
 */
public class DefaultMigrateDbExecution implements MigrateDbExecution {
    @Override
    public void run(MigrateDb migrateDb) {
        migrateDb.repair();
        migrateDb.migrate();
    }
}
