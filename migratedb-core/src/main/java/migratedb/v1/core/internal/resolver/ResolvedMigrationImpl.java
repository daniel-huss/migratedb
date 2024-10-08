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
package migratedb.v1.core.internal.resolver;

import migratedb.v1.core.api.Checksum;
import migratedb.v1.core.api.MigrationType;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.executor.MigrationExecutor;
import migratedb.v1.core.api.resolver.ResolvedMigration;

import java.util.Objects;

/**
 * A migration available on the classpath.
 */
public class ResolvedMigrationImpl implements ResolvedMigration {
    /**
     * The name of the script to execute for this migration.
     */
    private final String script;
    /**
     * The equivalent checksum of the migration. For versioned migrations, this is the same as the checksum. For
     * repeatable migrations, it is the checksum calculated prior to placeholder replacement.
     */
    private final Checksum equivalentChecksum;
    private final Checksum checksum;
    private final Version version;
    private final String description;
    private final MigrationType type;
    private final String locationDescription;
    private final MigrationExecutor executor;

    public ResolvedMigrationImpl(Version version,
                                 String description,
                                 String script,
                                 Checksum checksum,
                                 Checksum equivalentChecksum,
                                 MigrationType type,
                                 String locationDescription,
                                 MigrationExecutor executor) {
        this.version = version;
        this.description = description;
        this.script = script;
        this.checksum = checksum;
        this.equivalentChecksum = equivalentChecksum;
        this.type = type;
        this.locationDescription = locationDescription;
        this.executor = executor;
    }

    @Override
    public Version getVersion() {
        return version;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getScript() {
        return script;
    }

    @Override
    public Checksum getChecksum() {
        return checksum == null ? equivalentChecksum : checksum;
    }

    @Override
    public MigrationType getType() {
        return type;
    }

    @Override
    public String getLocationDescription() {
        return locationDescription;
    }

    @Override
    public MigrationExecutor getExecutor() {
        return executor;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ResolvedMigrationImpl)) {
            return false;
        }
        var other = (ResolvedMigrationImpl) o;
        return Objects.equals(checksum, other.checksum) &&
               Objects.equals(equivalentChecksum, other.equivalentChecksum) &&
               Objects.equals(description, other.description) &&
               Objects.equals(script, other.script) &&
               Objects.equals(type, other.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksum, equivalentChecksum, description, script, type);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
               "version=" + version +
               ", description='" + description + '\'' +
               ", script='" + script + '\'' +
               ", checksum=" + getChecksum() +
               ", type=" + type +
               ", locationDescription='" + locationDescription + '\'' +
               ", executor=" + executor +
               '}';
    }

    @Override
    public boolean checksumMatches(Checksum checksum) {
        return Objects.equals(checksum, this.checksum) ||
               (Objects.equals(checksum, equivalentChecksum) && equivalentChecksum != null);
    }
}
