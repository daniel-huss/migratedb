/*
 * Copyright 2022 The MigrateDB contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package migratedb.core.internal.extension;

import java.util.Set;
import migratedb.core.api.MigrateDbExtension;
import migratedb.core.api.internal.database.base.DatabaseType;
import migratedb.core.internal.database.bigquery.BigQueryDatabaseType;
import migratedb.core.internal.database.cockroachdb.CockroachDBDatabaseType;
import migratedb.core.internal.database.db2.DB2DatabaseType;
import migratedb.core.internal.database.derby.DerbyDatabaseType;
import migratedb.core.internal.database.firebird.FirebirdDatabaseType;
import migratedb.core.internal.database.h2.H2DatabaseType;
import migratedb.core.internal.database.hsqldb.HSQLDBDatabaseType;
import migratedb.core.internal.database.ignite.thin.IgniteThinDatabaseType;
import migratedb.core.internal.database.informix.InformixDatabaseType;
import migratedb.core.internal.database.mysql.MySQLDatabaseType;
import migratedb.core.internal.database.mysql.mariadb.MariaDBDatabaseType;
import migratedb.core.internal.database.mysql.tidb.TiDBDatabaseType;
import migratedb.core.internal.database.oracle.OracleDatabaseType;
import migratedb.core.internal.database.postgresql.PostgreSQLDatabaseType;
import migratedb.core.internal.database.redshift.RedshiftDatabaseType;
import migratedb.core.internal.database.saphana.SAPHANADatabaseType;
import migratedb.core.internal.database.snowflake.SnowflakeDatabaseType;
import migratedb.core.internal.database.spanner.SpannerDatabaseType;
import migratedb.core.internal.database.sqlite.SQLiteDatabaseType;
import migratedb.core.internal.database.sqlserver.SQLServerDatabaseType;
import migratedb.core.internal.database.sqlserver.synapse.SynapseDatabaseType;
import migratedb.core.internal.database.sybasease.SybaseASEJTDSDatabaseType;
import migratedb.core.internal.database.yugabytedb.YugabyteDBDatabaseType;

public final class BuiltinFeatures implements MigrateDbExtension {

    private static BuiltinFeatures INSTANCE;

    public static BuiltinFeatures instance() {
        synchronized (BuiltinFeatures.class) {
            if (INSTANCE == null) {
                INSTANCE = new BuiltinFeatures();
            }
            return INSTANCE;
        }
    }

    private BuiltinFeatures() {
    }

    @Override
    public String getDescription() {
        return "Built-in database types";
    }

    @Override
    public Set<DatabaseType> getDatabaseTypes() {
        return Set.of(
            new BigQueryDatabaseType(),
            new CockroachDBDatabaseType(),
            new DB2DatabaseType(),
            new DerbyDatabaseType(),
            new FirebirdDatabaseType(),
            new H2DatabaseType(),
            new HSQLDBDatabaseType(),
            new IgniteThinDatabaseType(),
            new InformixDatabaseType(),
            new MariaDBDatabaseType(),
            new MySQLDatabaseType(),
            new TiDBDatabaseType(),
            new PostgreSQLDatabaseType(),
            new OracleDatabaseType(),
            new RedshiftDatabaseType(),
            new SAPHANADatabaseType(),
            new SnowflakeDatabaseType(),
            new SpannerDatabaseType(),
            new SQLServerDatabaseType(),
            new SQLiteDatabaseType(),
            new SynapseDatabaseType(),
            new SybaseASEJTDSDatabaseType(),
            new YugabyteDBDatabaseType()
        );
    }
}
