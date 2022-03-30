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
package migratedb.core.internal.jdbc;

import java.util.concurrent.Callable;
import migratedb.core.api.internal.database.base.Table;
import migratedb.core.api.internal.jdbc.ExecutionTemplate;

public class TableLockingExecutionTemplate implements ExecutionTemplate {
    private final Table table;
    private final ExecutionTemplate executionTemplate;

    TableLockingExecutionTemplate(Table table, ExecutionTemplate executionTemplate) {
        this.table = table;
        this.executionTemplate = executionTemplate;
    }

    @Override
    public <T> T execute(Callable<T> callback) {
        return executionTemplate.execute(new Callable<T>() {
            @Override
            public T call() throws Exception {
                try {
                    table.lock();
                    return callback.call();
                } finally {
                    table.unlock();
                }
            }
        });
    }
}
