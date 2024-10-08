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

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.internal.resolver.MigrationInfoHelper;

/**
 * <p>This is the recommended class to extend for implementing Java-based Migrations.</p>
 * <p>Subclasses should follow the default MigrateDB naming convention of having a class name with the following
 * structure:</p>
 * <ul>
 * <li><strong>Versioned Migrations:</strong> V2__Add_new_table</li>
 * <li><strong>Repeatable Migrations:</strong> R__Add_new_table</li>
 * <li><strong>Baseline Migrations:</strong> B2__Add_new_table</li>
 * </ul>
 *
 * <p>The file name consists of the following parts:</p>
 * <ul>
 * <li><strong>Prefix:</strong> V for versioned migrations, R for repeatable migrations, B for
 * baseline migrations</li>
 * <li><strong>Version:</strong> Underscores (automatically replaced by dots at runtime) separate as many parts as
 * you like (Not for repeatable migrations)</li>
 * <li><strong>Separator:</strong> __ (two underscores)</li>
 * <li><strong>Description:</strong> Underscores (automatically replaced by spaces at runtime) separate the words</li>
 * </ul>
 * <p>If you need more control over the class name, you can override the default convention by implementing the
 * JavaMigration interface directly. This will allow you to name your class as you wish. Version, description and
 * migration category are provided by implementing the respective methods.</p>
 */
public abstract class BaseJavaMigration implements JavaMigration {
    private final boolean isBaseline;
    private final boolean isRepeatable;
    private final Version version;
    private final String description;

    /**
     * Creates a new instance of a Java-based migration following MigrateDB's default naming convention.
     */
    public BaseJavaMigration() {
        String shortName = getClass().getSimpleName();
        String prefix = null;

        isRepeatable = shortName.startsWith("R");
        isBaseline = shortName.startsWith("B");

        if (shortName.startsWith("V") || isBaseline || isRepeatable) {
            prefix = shortName.substring(0, 1);
        }
        if (prefix == null) {
            throw new MigrateDbException("Invalid Java-based migration class name: " + getClass().getName() +
                                         " => ensure it starts with V, R, B" +

                                         " or implement JavaMigration directly for " +
                                         "non-default naming");
        }

        var info = MigrationInfoHelper.extractVersionAndDescription(shortName, prefix, "__", isRepeatable);
        this.version = info.version;
        this.description = info.description;
    }

    @Override
    public final Version getVersion() {
        return version;
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final boolean isBaselineMigration() {
        return isBaseline;
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }
}
