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

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.internal.parser.Parser;
import migratedb.v1.core.api.internal.sqlscript.SqlScript;
import migratedb.v1.core.api.internal.sqlscript.SqlScriptMetadata;
import migratedb.v1.core.api.internal.sqlscript.SqlStatement;
import migratedb.v1.core.api.internal.sqlscript.SqlStatementIterator;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.api.resource.Resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParserSqlScript implements SqlScript {
    private static final Log LOG = Log.getLog(ParserSqlScript.class);

    /**
     * The sql statements contained in this script.
     */
    protected final List<SqlStatement> sqlStatements = new ArrayList<>();

    private int sqlStatementCount;

    /**
     * Whether this SQL script contains at least one non-transactional statement.
     */
    private boolean nonTransactionalStatementFound;

    /**
     * The resource containing the statements.
     */
    protected final Resource resource;

    private final SqlScriptMetadata metadata;
    protected final Parser parser;
    private final boolean mixed;
    private boolean parsed;

    /**
     * Creates a new sql script from this source.
     *
     * @param resource         The sql script resource.
     * @param metadataResource The sql script metadata resource.
     * @param mixed            Whether to allow mixing transactional and non-transactional statements within the same
     *                         migration.
     */
    public ParserSqlScript(Parser parser, Resource resource, Resource metadataResource, boolean mixed) {
        this.resource = resource;
        this.metadata = SqlScriptMetadataImpl.fromResource(metadataResource, parser);
        this.parser = parser;
        this.mixed = mixed;
    }

    protected void parse() {
        try (SqlStatementIterator sqlStatementIterator = parser.parse(resource, metadata)) {
            boolean transactionalStatementFound = false;
            while (sqlStatementIterator.hasNext()) {
                SqlStatement sqlStatement = sqlStatementIterator.next();
                sqlStatements.add(sqlStatement);

                sqlStatementCount++;

                if (sqlStatement.canExecuteInTransaction()) {
                    transactionalStatementFound = true;
                } else {
                    nonTransactionalStatementFound = true;
                }

                if (!mixed && transactionalStatementFound && nonTransactionalStatementFound &&
                    metadata.executeInTransaction() == null) {
                    throw new MigrateDbException(
                        "Detected both transactional and non-transactional statements within the same migration"
                        + " (even though mixed is false). Offending statement found at line "
                        + sqlStatement.getLineNumber() + ": " + sqlStatement.getSql()
                        + (sqlStatement.canExecuteInTransaction() ? "" : " [non-transactional]"));
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found statement at line " + sqlStatement.getLineNumber() + ": " + sqlStatement.getSql()
                              + (sqlStatement.canExecuteInTransaction() ? "" : " [non-transactional]"));
                }
            }
        }
        parsed = true;
    }

    @Override
    public void validate() {
        if (!parsed) {
            parse();
        }
    }

    @Override
    public SqlStatementIterator getSqlStatements() {
        validate();

        Iterator<SqlStatement> iterator = sqlStatements.iterator();
        return new SqlStatementIterator() {
            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public SqlStatement next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Override
    public int getSqlStatementCount() {
        validate();

        return sqlStatementCount;
    }

    @Override
    public final Resource getResource() {
        return resource;
    }

    @Override
    public boolean executeInTransaction() {
        Boolean executeInTransactionOverride = metadata.executeInTransaction();
        if (executeInTransactionOverride != null) {
            LOG.debug("Using executeInTransaction=" + executeInTransactionOverride + " from script configuration");
            return executeInTransactionOverride;
        }

        validate();

        return !nonTransactionalStatementFound;
    }

    @Override
    public boolean shouldExecute() {
        return metadata.shouldExecute();
    }

    @Override
    public int compareTo(SqlScript o) {
        return resource.getName().compareTo(o.getResource().getName());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
               "resource=" + resource.getName() +
               '}';
    }
}
