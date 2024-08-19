/*
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

package migratedb.v1.spring.boot.v3.autoconfig;

import migratedb.v1.core.MigrateDb;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DefaultMigrateDbExecution implements MigrateDbExecution {
    private final @Nullable MigrateDbProperties migrateDbProperties;

    public DefaultMigrateDbExecution(@Nullable MigrateDbProperties migrateDbProperties) {
        this.migrateDbProperties = migrateDbProperties;
    }

    @Override
    public void run(MigrateDb migrateDb) {
        if (migrateDbProperties == null || migrateDbProperties.isRepairOnMigrate()) {
            migrateDb.repair();
        }
        migrateDb.migrate();
    }
}
