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

package migratedb.v1.commandline.testing.migration;

import migratedb.v1.core.api.migration.BaseJavaMigration;
import migratedb.v1.core.api.migration.Context;

public class V1__First_Migration extends BaseJavaMigration {
    public static final String CREATED_TABLE = "_table_from_code_1";

    @Override
    public void migrate(Context context) throws Exception {
        try (var s = context.getConnection().createStatement()) {
            s.executeUpdate("create table " + CREATED_TABLE + "(id int)");
        }
    }
}
