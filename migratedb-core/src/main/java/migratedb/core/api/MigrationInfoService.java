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
package migratedb.core.api;

/**
 * Info about all migrations, including applied, current and pending with details and status.
 */
public interface MigrationInfoService extends InfoOutputProvider {
    /**
     * Retrieves the full set of infos about applied, current and future migrations.
     *
     * @return The full set of infos. An empty array if none.
     */
    MigrationInfo[] all();

    /**
     * Retrieves the information of the current applied migration, if any.
     *
     * @return The info. {@code null} if no migrations have been applied yet.
     */
    MigrationInfo current();

    /**
     * Retrieves the information of the next pending migration, if any.
     *
     * @return The info. {@code null} if no migration is pending.
     */
    default MigrationInfo next() {
        var pending = pending();
        return pending.length == 0 ? null : pending[0];
    }

    /**
     * Retrieves the full set of infos about pending migrations, available locally, but not yet applied to the DB.
     *
     * @return The pending migrations, ordered by version. An empty array if none.
     */
    MigrationInfo[] pending();

    /**
     * Retrieves the full set of infos about the migrations applied to the DB.
     *
     * @return The applied migrations. An empty array if none.
     */
    MigrationInfo[] applied();

    /**
     * @return The resolved migrations. An empty array if none.
     */
    MigrationInfo[] resolved();

    /**
     * @return The failed migrations. An empty array if none.
     */
    MigrationInfo[] failed();

    /**
     * @return The future migrations. An empty array if none.
     */
    MigrationInfo[] future();

    /**
     * @return The out of order migrations. An empty array if none.
     */
    MigrationInfo[] outOfOrder();
}
