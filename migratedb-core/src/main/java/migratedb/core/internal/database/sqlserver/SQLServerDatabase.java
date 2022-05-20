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
package migratedb.core.internal.database.sqlserver;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import migratedb.core.api.Version;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.database.base.Schema;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.jdbc.JdbcConnectionFactory;
import migratedb.core.api.internal.jdbc.StatementInterceptor;
import migratedb.core.api.internal.sqlscript.Delimiter;
import migratedb.core.internal.database.base.BaseDatabase;
import migratedb.core.internal.util.StringUtils;

public class SQLServerDatabase extends BaseDatabase<SQLServerConnection> {
    public SQLServerDatabase(Configuration configuration, JdbcConnectionFactory jdbcConnectionFactory,
                             StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    protected SQLServerConnection doGetConnection(Connection connection) {
        return new SQLServerConnection(this, connection);
    }

    @Override
    public final void ensureSupported() {
        if (isAzure()) {
            ensureDatabaseIsRecentEnough("11.0");
            recommendMigrateDbUpgradeIfNecessary("13.0");
        } else {
            ensureDatabaseIsRecentEnough("10.0");
            recommendMigrateDbUpgradeIfNecessary("15.0");
        }
    }

    @Override
    protected String computeVersionDisplayName(Version version) {
        if (isAzure()) {
            return "Azure v" + getVersion().getMajorAsString();
        }
        if (getVersion().isAtLeast("8")) {
            if ("8".equals(getVersion().getMajorAsString())) {
                return "2000";
            }
            if ("9".equals(getVersion().getMajorAsString())) {
                return "2005";
            }
            if ("10".equals(getVersion().getMajorAsString())) {
                if ("0".equals(getVersion().getMinorAsString())) {
                    return "2008";
                }
                return "2008 R2";
            }
            if ("11".equals(getVersion().getMajorAsString())) {
                return "2012";
            }
            if ("12".equals(getVersion().getMajorAsString())) {
                return "2014";
            }
            if ("13".equals(getVersion().getMajorAsString())) {
                return "2016";
            }
            if ("14".equals(getVersion().getMajorAsString())) {
                return "2017";
            }
            if ("15".equals(getVersion().getMajorAsString())) {
                return "2019";
            }
        }
        return super.computeVersionDisplayName(version);
    }

    @Override
    public Delimiter getDefaultDelimiter() {
        return Delimiter.GO;
    }

    @Override
    protected String doGetCurrentUser() throws SQLException {
        return getMainConnection().getJdbcTemplate().queryForString("SELECT SUSER_SNAME()");
    }

    @Override
    public boolean supportsDdlTransactions() {
        return true;
    }

    @Override
    public boolean supportsChangingCurrentSchema() {
        return false;
    }

    @Override
    public String getBooleanTrue() {
        return "1";
    }

    @Override
    public String getBooleanFalse() {
        return "0";
    }

    /**
     * Escapes this identifier, so it can be safely used in sql queries.
     *
     * @param identifier The identifier to escaped.
     *
     * @return The escaped version.
     */
    private String escapeIdentifier(String identifier) {
        return StringUtils.replaceAll(identifier, getCloseQuote(), getEscapedQuote());
    }

    @Override
    public String doQuote(String identifier) {
        return getOpenQuote() + escapeIdentifier(identifier) + getCloseQuote();
    }

    @Override
    public String getOpenQuote() {
        return "[";
    }

    @Override
    public String getCloseQuote() {
        return "]";
    }

    @Override
    public String getEscapedQuote() {
        return "]]";
    }

    @Override
    public boolean catalogIsSchema() {
        return false;
    }

    @Override
    public boolean useSingleConnection() {
        return true;
    }

    @Override
    public String getRawCreateScript(Table table, boolean baseline) {
        String filegroup = isAzure() || configuration.getTablespace() == null ?
                           "" : " ON \"" + configuration.getTablespace() + "\"";

        return "CREATE TABLE " + table + " (\n" +
               "    [installed_rank] INT NOT NULL,\n" +
               "    [" + "version] NVARCHAR(50),\n" +
               "    [description] NVARCHAR(200),\n" +
               "    [type] NVARCHAR(20) NOT NULL,\n" +
               "    [script] NVARCHAR(1000) NOT NULL,\n" +
               "    [checksum] NVARCHAR(100),\n" +
               "    [installed_by] NVARCHAR(100) NOT NULL,\n" +
               "    [installed_on] DATETIME NOT NULL DEFAULT GETDATE(),\n" +
               "    [execution_time] INT NOT NULL,\n" +
               "    [success] BIT NOT NULL\n" +
               ")" + filegroup + ";\n" +
               (baseline ? getBaselineStatement(table) + ";\n" : "") +
               "ALTER TABLE " + table + " ADD CONSTRAINT [" + table.getName() +
               "_pk] PRIMARY KEY ([installed_rank]);\n" +
               "CREATE INDEX [" + table.getName() + "_s_idx] ON " + table + " ([success]);\n" +
               "GO\n";
    }

    boolean isAzure() {
        return getMainConnection().isAzureConnection();
    }

    SQLServerEngineEdition getEngineEdition() {
        return getMainConnection().getEngineEdition();
    }

    boolean supportsTemporalTables() {
        // SQL Server 2016+, or Azure (which has different versioning)
        return isAzure() || getVersion().isAtLeast("13.0");
    }

    protected boolean supportsPartitions() {
        return isAzure() || SQLServerEngineEdition.ENTERPRISE.equals(getEngineEdition()) ||
               getVersion().isAtLeast("13");
    }

    protected boolean supportsSequences() {
        return getVersion().isAtLeast("11");
    }

    protected boolean supportsSynonyms() {
        return true;
    }

    protected boolean supportsRules() {
        return true;
    }

    protected boolean supportsTypes() {
        return true;
    }

    protected boolean supportsTriggers() {
        return true;
    }

    protected boolean supportsAssemblies() {
        return true;
    }

    /**
     * Cleans all the objects in this database that need to be cleaned after cleaning schemas.
     *
     * @param schemas The list of schemas managed by MigrateDb
     *
     * @throws SQLException when the clean failed.
     */
    @Override
    protected void doCleanPostSchemas(Schema[] schemas) throws SQLException {
        if (supportsPartitions()) {
            for (String statement : cleanPartitionSchemes()) {
                jdbcTemplate.execute(statement);
            }
            for (String statement : cleanPartitionFunctions()) {
                jdbcTemplate.execute(statement);
            }
        }

        if (supportsAssemblies()) {
            for (String statement : cleanAssemblies()) {
                jdbcTemplate.execute(statement);
            }
        }

        if (supportsTypes()) {
            for (String statement : cleanTypes(schemas)) {
                jdbcTemplate.execute(statement);
            }
        }
    }

    /**
     * @param schemas The list of schemas managed by MigrateDb
     * @return The drop statements.
     * @throws SQLException when the clean statements could not be generated.
     */
    private List<String> cleanTypes(Schema[] schemas) throws SQLException {
        List<String> statements = new ArrayList<>();
        String schemaList = Arrays.stream(schemas).map(s -> "'" + s.getName() + "'").collect(Collectors.joining(","));
        if (schemaList.isEmpty()) {
            schemaList = "''";
        }
        List<Map<String, String>> typesAndSchemas = jdbcTemplate.queryForList(
            "SELECT t.name as type_name, s.name as schema_name " +
            "FROM sys.types t INNER JOIN sys.schemas s ON t.schema_id = s.schema_id " +
            "WHERE t.is_user_defined = 1 AND s.name IN (" + schemaList + ")");

        for (Map<String, String> typeAndSchema : typesAndSchemas) {
            statements.add("DROP TYPE " + quote(typeAndSchema.get("schema_name"), typeAndSchema.get("type_name")));
        }
        return statements;
    }

    /**
     * @return The drop statements.
     *
     * @throws SQLException when the clean statements could not be generated.
     */
    private List<String> cleanPartitionSchemes() throws SQLException {
        List<String> statements = new ArrayList<>();
        List<String> partitionSchemeNames = jdbcTemplate.queryForStringList("SELECT name FROM sys.partition_schemes");
        for (String partitionSchemeName : partitionSchemeNames) {
            statements.add("DROP PARTITION SCHEME " + quote(partitionSchemeName));
        }
        return statements;
    }

    /**
     * @return The drop statements.
     *
     * @throws SQLException when the clean statements could not be generated.
     */
    private List<String> cleanAssemblies() throws SQLException {
        List<String> statements = new ArrayList<>();
        List<String> assemblyNames = jdbcTemplate.queryForStringList(
            "SELECT * FROM sys.assemblies WHERE is_user_defined=1");
        for (String assemblyName : assemblyNames) {
            statements.add("DROP ASSEMBLY " + quote(assemblyName));
        }
        return statements;
    }

    /**
     * @return The drop statements.
     *
     * @throws SQLException when the clean statements could not be generated.
     */
    private List<String> cleanPartitionFunctions() throws SQLException {
        List<String> statements = new ArrayList<>();
        List<String> partitionFunctionNames = jdbcTemplate.queryForStringList("SELECT name FROM sys.partition_functions");
        for (String partitionFunctionName : partitionFunctionNames) {
            statements.add("DROP PARTITION FUNCTION " + quote(partitionFunctionName));
        }
        return statements;
    }
}
