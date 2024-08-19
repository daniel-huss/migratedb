/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2024 The MigrateDB contributors
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
package migratedb.v1.core.internal.database.oracle;

import migratedb.v1.core.api.internal.database.base.Table;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.*;

import static migratedb.v1.core.internal.database.oracle.OracleSchema.ObjectType.TABLE;
import static migratedb.v1.core.internal.database.oracle.OracleSchema.ObjectType.supportedTypesExist;

/**
 * Oracle implementation of Schema.
 */
public class OracleSchema extends BaseSchema {
    /**
     * Creates a new Oracle schema.
     *
     * @param jdbcTemplate The Jdbc Template for communicating with the DB.
     * @param database     The database-specific support.
     * @param name         The name of the schema.
     */
    OracleSchema(JdbcTemplate jdbcTemplate, OracleDatabase database, String name) {
        super(jdbcTemplate, database, name);
    }

    /**
     * Checks whether the schema is system, i.e. Oracle-maintained, or not.
     *
     * @return {@code true} if it is system, {@code false} if not.
     */
    public boolean isSystem() throws SQLException {
        return getDatabase().getSystemSchemas().contains(name);
    }

    @Override
    protected OracleDatabase getDatabase() {
        return (OracleDatabase) super.getDatabase();
    }

    @Override
    protected boolean doExists() throws SQLException {
        return getDatabase().queryReturnsRows("SELECT * FROM ALL_USERS WHERE USERNAME = ?", name);
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        return !supportedTypesExist(jdbcTemplate, getDatabase(), this);
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE USER " + getDatabase().quote(name) + " IDENTIFIED BY "
                             + getDatabase().quote("FFllyywwaayy00!!"));
        jdbcTemplate.execute("GRANT RESOURCE TO " + getDatabase().quote(name));
        jdbcTemplate.execute("GRANT UNLIMITED TABLESPACE TO " + getDatabase().quote(name));
    }

    @Override
    protected List<OracleTable> doAllTables() throws SQLException {
        List<String> tableNames = TABLE.getObjectNames(jdbcTemplate, getDatabase(), this);

        List<OracleTable> tables = new ArrayList<>(tableNames.size());
        for (var tableName : tableNames) {
            tables.add(new OracleTable(jdbcTemplate, getDatabase(), this, tableName));
        }
        return tables;
    }

    @Override
    public Table getTable(String tableName) {
        return new OracleTable(jdbcTemplate, getDatabase(), this, tableName);
    }

    /**
     * Oracle object types.
     */
    public enum ObjectType {
        TABLE("TABLE") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                boolean referencePartitionedTablesExist = database.queryReturnsRows(
                        "SELECT * FROM ALL_PART_TABLES WHERE OWNER = ? AND PARTITIONING_TYPE = 'REFERENCE'",
                        schema.getName());
                boolean xmlDbAvailable = database.isXmlDbAvailable();

                StringBuilder tablesQuery = new StringBuilder();
                tablesQuery.append("WITH TABLES AS (\n" +
                                   "  SELECT TABLE_NAME, OWNER\n" +
                                   "  FROM ALL_TABLES\n" +
                                   "  WHERE OWNER = ?\n" +
                                   "    AND (IOT_TYPE IS NULL OR IOT_TYPE NOT LIKE '%OVERFLOW%')\n" +
                                   "    AND NESTED != 'YES'\n" +
                                   "    AND SECONDARY != 'Y'\n");

                if (xmlDbAvailable) {
                    tablesQuery.append("  UNION ALL\n" +
                                       "  SELECT TABLE_NAME, OWNER\n" +
                                       "  FROM ALL_XML_TABLES\n" +
                                       "  WHERE OWNER = ?\n" +
                                       // ALL_XML_TABLES shows objects in RECYCLEBIN, ignore them
                                       "    AND TABLE_NAME NOT LIKE 'BIN$________________________$_'\n");
                }

                tablesQuery.append(")\n" +
                                   "SELECT t.TABLE_NAME\n" +
                                   "FROM TABLES t\n");

                // Reference partitioned tables should be dropped in child-to-parent order.
                if (referencePartitionedTablesExist) {
                    tablesQuery.append("  LEFT JOIN ALL_PART_TABLES pt\n" +
                                       "    ON t.OWNER = pt.OWNER\n" +
                                       "   AND t.TABLE_NAME = pt.TABLE_NAME\n" +
                                       "   AND pt.PARTITIONING_TYPE = 'REFERENCE'\n" +
                                       "  LEFT JOIN ALL_CONSTRAINTS fk\n" +
                                       "    ON pt.OWNER = fk.OWNER\n" +
                                       "   AND pt.TABLE_NAME = fk.TABLE_NAME\n" +
                                       "   AND pt.REF_PTN_CONSTRAINT_NAME = fk.CONSTRAINT_NAME\n" +
                                       "   AND fk.CONSTRAINT_TYPE = 'R'\n" +
                                       "  LEFT JOIN ALL_CONSTRAINTS puk\n" +
                                       "    ON fk.R_OWNER = puk.OWNER\n" +
                                       "   AND fk.R_CONSTRAINT_NAME = puk.CONSTRAINT_NAME\n" +
                                       "   AND puk.CONSTRAINT_TYPE IN ('P', 'U')\n" +
                                       "  LEFT JOIN TABLES p\n" +
                                       "    ON puk.OWNER = p.OWNER\n" +
                                       "   AND puk.TABLE_NAME = p.TABLE_NAME\n" +
                                       "START WITH p.TABLE_NAME IS NULL\n" +
                                       "CONNECT BY PRIOR t.TABLE_NAME = p.TABLE_NAME\n" +
                                       "ORDER BY LEVEL DESC");
                }

                int n = 1 + (xmlDbAvailable ? 1 : 0);
                String[] params = new String[n];
                Arrays.fill(params, schema.getName());

                return jdbcTemplate.queryForStringList(tablesQuery.toString(), params);
            }
        },
        QUEUE_TABLE("QUEUE TABLE") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                return jdbcTemplate.queryForStringList(
                        "SELECT QUEUE_TABLE FROM ALL_QUEUE_TABLES WHERE OWNER = ?",
                        schema.getName()
                );
            }
        },
        MATERIALIZED_VIEW_LOG("MATERIALIZED VIEW LOG") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                return jdbcTemplate.queryForStringList(
                        "SELECT MASTER FROM ALL_MVIEW_LOGS WHERE LOG_OWNER = ?",
                        schema.getName()
                );
            }
        },
        INDEX("INDEX") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                return jdbcTemplate.queryForStringList(
                        "SELECT INDEX_NAME FROM ALL_INDEXES WHERE OWNER = ?" +
                        //" AND INDEX_NAME NOT LIKE 'SYS_C%'"+
                        " AND INDEX_TYPE NOT LIKE '%DOMAIN%'",
                        schema.getName()
                );
            }
        },
        DOMAIN_INDEX("INDEX") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                return jdbcTemplate.queryForStringList(
                        "SELECT INDEX_NAME FROM ALL_INDEXES WHERE OWNER = ? AND INDEX_TYPE LIKE '%DOMAIN%'",
                        schema.getName()
                );
            }
        },
        DOMAIN_INDEX_TYPE("INDEXTYPE"),
        OPERATOR("OPERATOR"),
        CLUSTER("CLUSTER"),
        VIEW("VIEW"),
        MATERIALIZED_VIEW("MATERIALIZED VIEW"),
        DIMENSION("DIMENSION") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                return jdbcTemplate.queryForStringList(
                        "SELECT DIMENSION_NAME FROM ALL_DIMENSIONS WHERE OWNER = ?",
                        schema.getName()
                );
            }
        },
        SYNONYM("SYNONYM"),
        SEQUENCE("SEQUENCE"),
        PROCEDURE("PROCEDURE"),
        FUNCTION("FUNCTION"),
        PACKAGE("PACKAGE"),
        CONTEXT("CONTEXT") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                return jdbcTemplate.queryForStringList(
                        "SELECT NAMESPACE FROM " + database.dbaOrAll("CONTEXT") + " WHERE SCHEMA = ?",
                        schema.getName()
                );
            }
        },
        TRIGGER("TRIGGER"),
        TYPE("TYPE"),
        JAVA_SOURCE("JAVA SOURCE"),
        JAVA_CLASS("JAVA CLASS"),
        JAVA_RESOURCE("JAVA RESOURCE"),
        LIBRARY("LIBRARY"),
        XML_SCHEMA("XML SCHEMA") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                if (!database.isXmlDbAvailable()) {
                    return Collections.emptyList();
                }
                return jdbcTemplate.queryForStringList(
                        "SELECT QUAL_SCHEMA_URL FROM " + database.dbaOrAll("XML_SCHEMAS") + " WHERE OWNER = ?",
                        schema.getName()
                );
            }
        },

        REWRITE_EQUIVALENCE("REWRITE EQUIVALENCE"),
        SQL_TRANSLATION_PROFILE("SQL TRANSLATION PROFILE"),
        MINING_MODEL("MINING MODEL") {
            @Override
            public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                    throws SQLException {
                return super.getObjectNames(jdbcTemplate, database, schema);

            }
        },
        SCHEDULER_JOB("JOB"),
        SCHEDULER_PROGRAM("PROGRAM"),
        SCHEDULE("SCHEDULE"),
        SCHEDULER_CHAIN("CHAIN"),
        FILE_WATCHER("FILE WATCHER"),
        RULE_SET("RULE SET"),
        RULE("RULE"),
        EVALUATION_CONTEXT("EVALUATION CONTEXT"),
        FILE_GROUP("FILE GROUP");

        private final String name;

        ObjectType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return super.toString().replace('_', ' ');
        }

        /**
         * Returns the list of object names of this type.
         *
         * @throws SQLException if retrieving of objects failed.
         */
        public List<String> getObjectNames(JdbcTemplate jdbcTemplate, OracleDatabase database, OracleSchema schema)
                throws SQLException {
            return jdbcTemplate.queryForStringList(
                    "SELECT DISTINCT OBJECT_NAME FROM ALL_OBJECTS WHERE OWNER = ? AND OBJECT_TYPE = ?",
                    schema.getName(), getName()
            );
        }

        /**
         * Returns the schema's existing object types.
         *
         * @return a set of object type names.
         * @throws SQLException if retrieving of object types failed.
         */
        public static Set<String> getObjectTypeNames(JdbcTemplate jdbcTemplate,
                                                     OracleDatabase database,
                                                     OracleSchema schema) throws SQLException {
            boolean xmlDbAvailable = database.isXmlDbAvailable();

            String query =
                    // Most object types can be correctly selected from DBA_/ALL_OBJECTS.
                    "SELECT DISTINCT OBJECT_TYPE FROM " + database.dbaOrAll("OBJECTS") + " WHERE OWNER = ? " +
                    // Materialized view logs.
                    "UNION SELECT '" + MATERIALIZED_VIEW_LOG.getName() + "' FROM DUAL WHERE EXISTS(" +
                    "SELECT * FROM ALL_MVIEW_LOGS WHERE LOG_OWNER = ?) " +
                    // Dimensions.
                    "UNION SELECT '" + DIMENSION.getName() + "' FROM DUAL WHERE EXISTS(" +
                    "SELECT * FROM ALL_DIMENSIONS WHERE OWNER = ?) " +
                    // Queue tables.
                    "UNION SELECT '" + QUEUE_TABLE.getName() + "' FROM DUAL WHERE EXISTS(" +
                    "SELECT * FROM ALL_QUEUE_TABLES WHERE OWNER = ?) " +
                    // Contexts.
                    "UNION SELECT '" + CONTEXT.getName() + "' FROM DUAL WHERE EXISTS(" +
                    "SELECT * FROM " + database.dbaOrAll("CONTEXT") + " WHERE SCHEMA = ?) " +
                    // XML schemas.
                    (xmlDbAvailable
                            ? "UNION SELECT '" + XML_SCHEMA.getName() + "' FROM DUAL WHERE EXISTS(" +
                              "SELECT * FROM " + database.dbaOrAll("XML_SCHEMAS") + " WHERE OWNER = ?) "
                            : "");
            String[] params = new String[(int) query.chars().filter(it -> it == '?').count()];
            Arrays.fill(params, schema.getName());

            return new HashSet<>(jdbcTemplate.queryForStringList(query, params));
        }

        /**
         * Checks whether the specified schema contains object types that can be cleaned.
         *
         * @return {@code true} if it contains, {@code false} if not.
         * @throws SQLException if retrieving of object types failed.
         */
        public static boolean supportedTypesExist(JdbcTemplate jdbcTemplate,
                                                  OracleDatabase database,
                                                  OracleSchema schema) throws SQLException {
            Set<String> existingTypeNames = new HashSet<>(getObjectTypeNames(jdbcTemplate, database, schema));
            return !existingTypeNames.isEmpty();
        }
    }
}
