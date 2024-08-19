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
package migratedb.v1.core.internal.sqlscript;

import migratedb.v1.core.api.callback.Event;
import migratedb.v1.core.api.callback.Warning;
import migratedb.v1.core.api.internal.callback.CallbackExecutor;
import migratedb.v1.core.api.internal.jdbc.JdbcTemplate;
import migratedb.v1.core.api.internal.jdbc.Result;
import migratedb.v1.core.api.internal.jdbc.Results;
import migratedb.v1.core.api.internal.sqlscript.SqlScript;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptExecutor;
import migratedb.v1.core.api.internal.sqlscript.SqlStatement;
import migratedb.v1.core.api.internal.sqlscript.SqlStatementIterator;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.util.AsciiTable;

public class DefaultSqlScriptExecutor implements SqlScriptExecutor {
    private static final Log LOG = Log.getLog(DefaultSqlScriptExecutor.class);

    protected final JdbcTemplate jdbcTemplate;
    private final CallbackExecutor callbackExecutor;
    private final boolean outputQueryResults;

    public DefaultSqlScriptExecutor(JdbcTemplate jdbcTemplate,
                                    CallbackExecutor callbackExecutor,
                                    boolean outputQueryResults) {
        this.jdbcTemplate = jdbcTemplate;
        this.callbackExecutor = callbackExecutor;
        this.outputQueryResults = outputQueryResults;
    }

    @Override
    public void execute(SqlScript sqlScript) {
        try (SqlStatementIterator sqlStatementIterator = sqlScript.getSqlStatements()) {
            while (sqlStatementIterator.hasNext()) {
                SqlStatement sqlStatement = sqlStatementIterator.next();
                executeStatement(jdbcTemplate, sqlScript, sqlStatement);
            }
        }
    }

    protected void logStatementExecution(SqlStatement sqlStatement) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing SQL: " + sqlStatement.getSql());
        }
    }

    protected void executeStatement(JdbcTemplate jdbcTemplate, SqlScript sqlScript, SqlStatement sqlStatement) {
        logStatementExecution(sqlStatement);
        callbackExecutor.onEachMigrateEvent(Event.BEFORE_EACH_MIGRATE_STATEMENT);
        Results results = sqlStatement.execute(jdbcTemplate);
        callbackExecutor.onEachMigrateEvent(Event.AFTER_EACH_MIGRATE_STATEMENT);
        if (results.getException() != null) {
            callbackExecutor.onEachMigrateEvent(Event.AFTER_EACH_MIGRATE_STATEMENT_ERROR);
            printWarnings(results);
            handleException(results, sqlScript, sqlStatement);
            return;
        }

        printWarnings(results);
        handleResults(results);
    }

    protected void handleResults(Results results) {
        for (Result result : results.getResults()) {
            long updateCount = result.getUpdateCount();
            if (updateCount != -1) {
                handleUpdateCount(updateCount);
            }
            outputQueryResult(result);
        }
    }

    protected void outputQueryResult(Result result) {
        if (outputQueryResults && result.getColumns() != null && !result.getColumns().isEmpty()) {
            var output = new StringBuilder("\n");
            new AsciiTable(result.getColumns(),
                    result.getData(),
                    true,
                    "",
                    "No rows returned")
                    .render(output);
            LOG.info(output.toString());
        }
    }

    private void handleUpdateCount(long updateCount) {
        LOG.debug(updateCount + (updateCount == 1 ? " row" : " rows") + " affected");
    }

    protected void handleException(Results results, SqlScript sqlScript, SqlStatement sqlStatement) {
        throw new MigrateDbSqlScriptException(sqlScript.getResource(), sqlStatement, results.getException());
    }

    private void printWarnings(Results results) {
        for (Warning warning : results.getWarnings()) {
            if ("00000".equals(warning.getState())) {
                LOG.info("DB: " + warning.getMessage());
            } else {
                LOG.warn("DB: " + warning.getMessage()
                        + " (SQL State: " + warning.getState() + " - Error Code: " + warning.getCode() + ")");
            }
        }
    }
}
