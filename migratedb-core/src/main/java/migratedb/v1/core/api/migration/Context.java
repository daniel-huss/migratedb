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
package migratedb.v1.core.api.migration;

import migratedb.v1.core.api.configuration.Configuration;

import java.io.Reader;
import java.sql.Connection;

/**
 * The context relevant to a Java-based migration.
 */
public interface Context {
    /**
     * @return The configuration currently in use.
     */
    Configuration getConfiguration();

    /**
     * @return The JDBC connection being used. Transactions are managed by MigrateDB. When the context is passed to the
     * migrate method, a transaction will already have been started if required and will be automatically committed or
     * rolled back afterward, unless the canExecuteInTransaction method has been implemented to return false.
     */
    Connection getConnection();

    /**
     * Executes a script on the connection returned by {@link #getConnection()}.
     */
    void runScript(Reader script);
}
