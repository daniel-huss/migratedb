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
package migratedb.core.internal.sqlscript;

import java.sql.SQLException;
import migratedb.core.api.resource.Resource;
import migratedb.core.internal.exception.MigrateDbSqlException;

/**
 * This specific exception thrown when MigrateDB encounters a problem in SQL script
 */
public class MigrateDbSqlScriptException extends MigrateDbSqlException {
    private final Resource resource;
    private final SqlStatement statement;

    /**
     * Creates new instance of MigrateDbSqlScriptException.
     *
     * @param resource     The resource containing the failed statement.
     * @param statement    The failed SQL statement.
     * @param sqlException Cause of the problem.
     */
    public MigrateDbSqlScriptException(Resource resource, SqlStatement statement, SQLException sqlException) {
        super(resource == null ? "Script failed" : "Migration " + resource.getName() + " failed", sqlException);
        this.resource = resource;
        this.statement = statement;
    }

    /**
     * @return The resource containing the failed statement.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Returns the line number in migration SQL script where exception occurred.
     *
     * @return The line number.
     */
    public int getLineNumber() {
        return statement == null ? -1 : statement.getLineNumber();
    }

    /**
     * Returns the failed statement in SQL script.
     *
     * @return The failed statement.
     */
    public String getStatement() {
        return statement == null ? "" : statement.getSql();
    }

    /**
     * Returns the failed statement in SQL script.
     *
     * @return The failed statement.
     */
    public SqlStatement getSqlStatement() {
        return statement;
    }

    @Override
    public String getMessage() {
        String message = super.getMessage();
        if (resource != null) {
            message += "Location   : " + resource.describeLocation() + "\n";
        }
        if (statement != null) {
            message += "Line       : " + getLineNumber() + "\n";
            message += "Statement  : " + getStatement() + "\n";
        }
        return message;
    }
}
