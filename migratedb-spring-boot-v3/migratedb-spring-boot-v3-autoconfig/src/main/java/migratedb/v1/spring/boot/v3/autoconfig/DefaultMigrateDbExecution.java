package migratedb.v1.spring.boot.v3.autoconfig;

import migratedb.v1.core.MigrateDb;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DefaultMigrateDbExecution implements MigrateDbExecution {
    private final @Nullable MigrateDbProperties migrateDbProperties;

    public DefaultMigrateDbExecution(@Nullable MigrateDbProperties migrateDbProperties) {
        this.migrateDbProperties = migrateDbProperties;
    }

    @Override
    public void run(MigrateDb migrateDb) {
        if (migrateDbProperties == null || migrateDbProperties.isRepairOnMigrate()) {
            migrateDb.repair();
        }
        migrateDb.migrate();
    }
}
