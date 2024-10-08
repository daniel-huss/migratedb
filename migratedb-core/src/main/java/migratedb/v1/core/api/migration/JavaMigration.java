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

import migratedb.v1.core.api.Checksum;
import migratedb.v1.core.api.Version;
import migratedb.v1.core.api.configuration.Configuration;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigInteger;

/**
 * Interface to be implemented by Java-based Migrations.
 *
 * <p>Java-based migrations are a great fit for all changes that can not easily be expressed using SQL.</p>
 *
 * <p>These would typically be things like</p>
 * <ul>
 *     <li>BLOB &amp; CLOB changes</li>
 *     <li>Advanced bulk data changes (Recalculations, advanced format changes, …)</li>
 * </ul>
 *
 * <p>Migration classes implementing this interface will be
 * automatically discovered when placed in a location on the classpath.</p>
 *
 * <p>Most users will be better served by subclassing subclass {@link BaseJavaMigration} instead of implementing this
 * interface directly, as {@link BaseJavaMigration} encourages the use of MigrateDB 's default naming convention and
 * comes with sensible default implementations of all methods (except migrate of course) while at the same time also
 * providing better isolation against future additions to this interface.</p>
 */
public interface JavaMigration {
    /**
     * @return The version of the schema after the migration is complete. {@code null} for repeatable migrations.
     */
    @Nullable Version getVersion();

    /**
     * @return The description of this migration for the migration history. Never {@code null}.
     */
    String getDescription();

    /**
     * @return The checksum of this migration.
     * @deprecated Implement {@link #getChecksum(Configuration)} instead.
     */
    @Deprecated(forRemoval = true)
    default @Nullable Integer getChecksum() {
        return null;
    }

    /**
     * @return The checksum of this migration, {@code null} to disable checksum verification.
     */
    default @Nullable Checksum getChecksum(Configuration configuration) {
        var oldChecksum = getChecksum();
        return Checksum.builder()
                       .addNumber(oldChecksum == null ? null : BigInteger.valueOf(oldChecksum))
                       .build();
    }

    /**
     * Whether this is a baseline migration.
     *
     * @return {@code true} if it is, {@code false} if not.
     */
    boolean isBaselineMigration();

    /**
     * Whether the execution should take place inside a transaction. Almost all implementations should return
     * {@code true}. This however makes it possible to execute certain migrations outside a transaction. This is useful
     * for databases like PostgreSQL and SQL Server where certain statement can only execute outside a transaction.
     *
     * @return {@code true} if a transaction should be used (highly recommended), or {@code false} if not.
     */
    boolean canExecuteInTransaction();

    /**
     * Executes this migration. The execution will automatically take place within a transaction, when the underlying
     * database supports it and the canExecuteInTransaction returns {@code true}.
     *
     * @param context The context relevant for this migration, containing things like the JDBC connection to use and the
     *                current MigrateDB configuration.
     * @throws Exception when the migration failed.
     */
    void migrate(Context context) throws Exception;
}
