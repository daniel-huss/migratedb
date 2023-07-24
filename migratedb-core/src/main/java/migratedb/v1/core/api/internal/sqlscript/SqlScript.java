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
package migratedb.v1.core.api.internal.sqlscript;

import migratedb.v1.core.api.resource.Resource;

/**
 * SQL script containing a series of statements terminated by a delimiter (eg: ;). Single-line (--) and multi-line (/* *
 * /) comments are stripped and ignored.
 */
public interface SqlScript extends Comparable<SqlScript> {
    /**
     * @return The sql statements contained in this script.
     */
    SqlStatementIterator getSqlStatements();

    /**
     * @return The number of sql statements contained in this script.
     */
    int getSqlStatementCount();

    /**
     * @return The resource containing the statements.
     */
    Resource getResource();

    /**
     * Whether the execution should take place inside a transaction. This is useful for databases like PostgreSQL where
     * certain statement can only execute outside a transaction.
     *
     * @return {@code true} if a transaction should be used (highly recommended), or {@code false} if not.
     */
    boolean executeInTransaction();

    /**
     * Whether the script should execute or not.
     *
     * @return {@code true} if the script should execute, or {@code false} if not.
     */
    boolean shouldExecute();

    /**
     * Validates this SQL script.
     */
    void validate();
}
