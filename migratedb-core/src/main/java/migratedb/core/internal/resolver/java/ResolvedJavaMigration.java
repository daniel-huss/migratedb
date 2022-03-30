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
package migratedb.core.internal.resolver.java;

import migratedb.core.api.MigrationType;
import migratedb.core.api.migration.JavaMigration;
import migratedb.core.internal.resolver.ResolvedMigrationImpl;
import migratedb.core.internal.util.ClassUtils;

public class ResolvedJavaMigration extends ResolvedMigrationImpl {
    /**
     * Creates a new ResolvedJavaMigration based on this JavaMigration.
     *
     * @param javaMigration The JavaMigration to use.
     */
    public ResolvedJavaMigration(JavaMigration javaMigration) {
        super(javaMigration.getVersion(),
              javaMigration.getDescription(),
              javaMigration.getClass().getName(),
              javaMigration.getChecksum(),
              null,
              javaMigration.isBaselineMigration() ? MigrationType.JDBC_BASELINE : MigrationType.JDBC,
              ClassUtils.guessLocationOnDisk(javaMigration.getClass()),
              new JavaMigrationExecutor(javaMigration)
        );
    }
}
