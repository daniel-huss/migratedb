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
package migratedb.core.testing

import migratedb.core.api.ClassProvider
import migratedb.core.api.ResourceProvider
import migratedb.core.api.Version
import migratedb.core.api.callback.Callback
import migratedb.core.api.callback.Context
import migratedb.core.api.callback.Event
import migratedb.core.api.logging.LogSystem
import migratedb.core.api.migration.JavaMigration
import migratedb.core.api.resolver.MigrationResolver
import migratedb.core.api.resolver.ResolvedMigration
import migratedb.core.api.resource.Resource
import migratedb.core.internal.logging.NoLogSystem
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.sql.Driver
import java.util.function.Supplier

/**
 * Universal instantiable class that can be used when one of its interfaces is required. Its actions do nothing.
 */
class UniversalDummy : Callback,
    MigrationResolver,
    ResourceProvider,
    JavaMigration,
    LogSystem by NoLogSystem.INSTANCE,
    ClassProvider<JavaMigration>,
    Supplier<OutputStream>,
    Driver by org.h2.Driver() {
    override fun getClasses(): Collection<Class<JavaMigration>> = emptyList()

    override fun get(): OutputStream {
        return ByteArrayOutputStream(1)
    }

    override fun getVersion(): Version {
        return Version.parse("1.0")
    }

    override fun getDescription(): String {
        return "My Java Migration"
    }

    override fun getChecksum(): Int {
        return 1
    }

    override fun isBaselineMigration(): Boolean {
        return false
    }

    override fun canExecuteInTransaction(): Boolean {
        return true
    }

    override fun migrate(context: migratedb.core.api.migration.Context?) {
    }

    override fun getResource(name: String?): Resource? {
        return null
    }

    override fun getResources(prefix: String?, vararg suffixes: String?): Collection<Resource> {
        return emptyList()
    }

    override fun resolveMigrations(context: migratedb.core.api.resolver.Context?): Collection<ResolvedMigration> {
        return emptyList()
    }

    override fun supports(event: Event?, context: Context?): Boolean {
        return true
    }

    override fun canHandleInTransaction(event: Event?, context: Context?): Boolean {
        return true
    }

    override fun handle(event: Event?, context: Context?) {
    }

    override fun getCallbackName(): String {
        return "MyCallback"
    }

    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
