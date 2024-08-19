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

package migratedb.v1.core.api.internal.database.base;

public interface Table extends SchemaObject {
    boolean exists();

    /**
     * Locks this table in this schema using a read/write pessimistic lock until the end of the current transaction.
     * Note that {@code unlock()} still needs to be called even if your database unlocks the table implicitly (in which
     * case {@code doUnlock()} may be a no-op) in order to maintain the lock count correctly.
     */
    void lock();

    /**
     * For databases that require an explicit unlocking, not an implicit end-of-transaction one.
     */
    void unlock();
}
