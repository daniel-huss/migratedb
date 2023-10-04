/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.internal.database.sqlserver;

import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.internal.database.base.BaseSchema;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SQLServerSchema extends BaseSchema {
    protected final String databaseName;

    /**
     * SQL Server object types for which we support automatic clean-up. These types can be used in conjunction with the
     * {@code sys.objects} catalog. The full list of object types is available in the
     * <a href="https://msdn.microsoft.com/en-us/library/ms190324.aspx">MSDN documentation</a>
     * (see the {@code type} column description.)
     */
    protected enum ObjectType {
        /**
         * Aggregate function (CLR).
         */
        AGGREGATE("AF"),
        /**
         * CHECK constraint.
         */
        CHECK_CONSTRAINT("C"),
        /**
         * DEFAULT constraint.
         */
        DEFAULT_CONSTRAINT("D"),
        /**
         * PRIMARY KEY constraint.
         */
        PRIMARY_KEY("PK"),
        /**
         * FOREIGN KEY constraint.
         */
        FOREIGN_KEY("F"),
        /**
         * In-lined table-function.
         */
        INLINED_TABLE_FUNCTION("IF"),
        /**
         * Scalar function.
         */
        SCALAR_FUNCTION("FN"),
        /**
         * Assembly (CLR) scalar-function.
         */
        CLR_SCALAR_FUNCTION("FS"),
        /**
         * Assembly (CLR) table-valued function.
         */
        CLR_TABLE_VALUED_FUNCTION("FT"),
        /**
         * Stored procedure.
         */
        STORED_PROCEDURE("P"),
        /**
         * Assembly (CLR) stored-procedure.
         */
        CLR_STORED_PROCEDURE("PC"),
        /**
         * Rule (old-style, stand-alone).
         */
        RULE("R"),
        /**
         * Synonym.
         */
        SYNONYM("SN"),
        /**
         * Table-valued function.
         */
        TABLE_VALUED_FUNCTION("TF"),
        /**
         * Assembly (CLR) DML trigger.
         */
        ASSEMBLY_DML_TRIGGER("TA"),
        /**
         * SQL DML trigger.
         */
        SQL_DML_TRIGGER("TR"),
        /**
         * Unique Constraint.
         */
        UNIQUE_CONSTRAINT("UQ"),
        /**
         * User table.
         */
        USER_TABLE("U"),
        /**
         * View.
         */
        VIEW("V"),
        /**
         * Sequence object.
         */
        SEQUENCE_OBJECT("SO");

        public final String code;

        ObjectType(String code) {
            assert code != null;
            this.code = code;
        }
    }

    /**
     * SQL Server object meta-data.
     */
    public static class DBObject {
        public final String name;
        public final long objectId;

        public DBObject(long objectId, String name) {
            assert name != null;
            this.objectId = objectId;
            this.name = name;
        }
    }

    public SQLServerSchema(JdbcTemplate jdbcTemplate, SQLServerDatabase database, String databaseName, String name) {
        super(jdbcTemplate, database, name);
        this.databaseName = databaseName;
    }

    @Override
    protected boolean doExists() throws SQLException {
        return jdbcTemplate.queryForInt("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME=?", name) >
               0;
    }

    @Override
    protected boolean doCheckIfEmpty() throws SQLException {
        boolean empty = queryDBObjects(ObjectType.SCALAR_FUNCTION,
                                       ObjectType.AGGREGATE,
                                       ObjectType.CLR_SCALAR_FUNCTION,
                                       ObjectType.CLR_TABLE_VALUED_FUNCTION,
                                       ObjectType.TABLE_VALUED_FUNCTION,
                                       ObjectType.STORED_PROCEDURE,
                                       ObjectType.CLR_STORED_PROCEDURE,
                                       ObjectType.USER_TABLE,
                                       ObjectType.SYNONYM,
                                       ObjectType.SEQUENCE_OBJECT,
                                       ObjectType.FOREIGN_KEY,
                                       ObjectType.VIEW).isEmpty();
        if (empty) {
            int objectCount = jdbcTemplate.queryForInt("SELECT count(*) FROM " +
                                                       "( " +
                                                       "SELECT t.name FROM sys.types t INNER JOIN sys.schemas s ON t" +
                                                       ".schema_id = s.schema_id " +
                                                       "WHERE t.is_user_defined = 1 AND s.name = ? " +
                                                       "Union " +
                                                       "SELECT name FROM sys.assemblies WHERE is_user_defined=1" +
                                                       ") R", name);
            empty = objectCount == 0;
        }
        return empty;
    }

    @Override
    protected void doCreate() throws SQLException {
        jdbcTemplate.execute("CREATE SCHEMA " + getDatabase().quote(name));
    }

    /**
     * Query objects with any of the given types.
     *
     * @param types The object types to be queried.
     * @return The found objects.
     * @throws SQLException when the retrieval failed.
     */
    protected List<DBObject> queryDBObjects(ObjectType... types) throws SQLException {
        return queryDBObjectsWithParent(null, types);
    }

    /**
     * Query objects with any of the given types and parent (if non-null).
     *
     * @param parent The parent object or {@code null} if unspecified.
     * @param types  The object types to be queried.
     * @return The found objects.
     * @throws SQLException when the retrieval failed.
     */
    private List<DBObject> queryDBObjectsWithParent(DBObject parent, ObjectType... types) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT obj.object_id, obj.name FROM sys.objects AS obj " +
                                                "LEFT JOIN sys.extended_properties AS eps " +
                                                "ON obj.object_id = eps.major_id " +
                                                "AND eps.class = 1 " +
                                                // Class 1 = objects and columns (we are only interested in objects).
                                                "AND eps.minor_id = 0 " + // Minor ID, always 0 for objects.
                                                "AND eps.name='microsoft_database_tools_support' " +
                                                // Select all objects generated from MS database tools.
                                                "WHERE SCHEMA_NAME(obj.schema_id) = '" + name + "' " +
                                                "AND eps.major_id IS NULL " +
                                                // Left Excluding JOIN (we are only interested in user defined entries).
                                                "AND obj.is_ms_shipped = 0 " +
                                                // Make sure we do not return anything MS shipped.
                                                "AND obj.type IN (" // Select the object types.
        );

        // Build the types IN clause.
        boolean first = true;
        for (ObjectType type : types) {
            if (!first) {
                query.append(", ");
            }
            query.append("'").append(type.code).append("'");
            first = false;
        }
        query.append(")");

        // Apply the parent selection if one was given.
        if (parent != null) {
            query.append(" AND obj.parent_object_id = ").append(parent.objectId);
        }

        query.append(" order by create_date desc, object_id desc");

        return jdbcTemplate.query(query.toString(), rs -> new DBObject(rs.getLong("object_id"), rs.getString("name")));
    }

    @Override
    protected List<? extends SQLServerTable> doAllTables() throws SQLException {
        return queryDBObjects(ObjectType.USER_TABLE).stream()
                                                    .map(table -> new SQLServerTable(jdbcTemplate,
                                                                                     getDatabase(),
                                                                                     databaseName,
                                                                                     this,
                                                                                     table.name))
                                                    .collect(Collectors.toList());

    }

    @Override
    protected SQLServerDatabase getDatabase() {
        return (SQLServerDatabase) super.getDatabase();
    }

    @Override
    public SQLServerTable getTable(String tableName) {
        return new SQLServerTable(jdbcTemplate, getDatabase(), databaseName, this, tableName);
    }
}
