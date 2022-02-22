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
package migratedb.core.api.callback;

import java.sql.Connection;
import migratedb.core.api.MigrationInfo;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.output.OperationResult;

/**
 * The context relevant to an event.
 */
public interface Context {
    /**
     * @return The configuration currently in use.
     */
    Configuration getConfiguration();

    /**
     * @return The JDBC connection being used. Transaction are managed by MigrateDb. When the context is passed to the
     * {@link Callback#handle(Event, Context)} method, a transaction will already have been started if required and will
     * be automatically committed or rolled back afterwards.
     */
    Connection getConnection();

    /**
     * @return The info about the migration being handled. Only relevant for the BEFORE_EACH_* and AFTER_EACH_* events.
     * {@code null} in all other cases.
     */
    MigrationInfo getMigrationInfo();

    /**
     * @return The info about the statement being handled. Only relevant for the statement-level events. {@code null} in
     * all other cases.
     *
     */
    Statement getStatement();

    /**
     * @return The OperationResult object for the finished operation. Only relevant for the AFTER_*_OPERATION_FINISH
     * events.
     */
    OperationResult getOperationResult();
}