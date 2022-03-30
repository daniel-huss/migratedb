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

package migratedb.integrationtest.database

import io.kotest.assertions.throwables.shouldThrow
import migratedb.core.api.internal.database.base.DatabaseType
import migratedb.core.internal.database.DatabaseTypeRegisterImpl
import migratedb.core.internal.database.derby.DerbyDatabaseType
import migratedb.core.internal.jdbc.DriverDataSource
import migratedb.integrationtest.database.mutation.DerbyCreateTableMutation
import migratedb.integrationtest.database.mutation.IndependentDatabaseMutation
import migratedb.integrationtest.util.base.Names
import migratedb.integrationtest.util.base.SafeIdentifier
import migratedb.integrationtest.util.base.work
import migratedb.integrationtest.util.container.SharedResources
import migratedb.testing.util.dependencies.DependencyResolver
import migratedb.testing.util.dependencies.DependencyResolver.toClassLoader
import migratedb.testing.util.io.newTempDir
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.sql.Driver
import java.util.*
import javax.management.MBeanServerDelegate
import javax.management.MBeanServerNotification
import javax.management.MBeanServerNotification.REGISTRATION_NOTIFICATION
import javax.management.relation.MBeanServerNotificationFilter
import javax.sql.DataSource
import kotlin.io.path.exists
import kotlin.streams.asSequence


enum class Derby : DbSystem {
    V10_15_2_0,
    V10_14_2_0,
    V10_13_1_1,
    V10_12_1_1
    ;

    // Relevant idiosyncracies:
    //  - Does not normalize database names because it's just a file name

    companion object {
        private val derbyBootLock = Any()
        private val databaseType = DerbyDatabaseType()
        private val databaseTypeRegister = DatabaseTypeRegisterImpl().also {
            it.registerDatabaseTypes(listOf(databaseType))
        }

        init {
            System.setProperty("derby.stream.error.file", newTempDir("derby").resolve("derby.log").toString())
            System.setProperty("derby.infolog.append", "true")
            disableMbeanRegistration()
        }

        private fun disableMbeanRegistration() {
            // Derby tries to register its MBean on startup and this cannot be disabled. Failing to do so
            // (because the MBean is already registered) will lead to a NullPointerException on connect() because
            // the Derby "boot" process is interrupted by the MBean registration exception and they have no
            // error handling. To solve this, we just unregister any MBean immediately after registration.
            // To work reliably, this and the code that triggers "booting" Derby have to synchronize- on the same lock.
            val server = ManagementFactory.getPlatformMBeanServer()
            val filter = MBeanServerNotificationFilter()
            filter.enableAllObjectNames()
            server.addNotificationListener(
                MBeanServerDelegate.DELEGATE_NAME,
                { notification, _ ->
                    synchronized(derbyBootLock) {
                        (notification as? MBeanServerNotification)?.let {
                            when (it.type) {
                                REGISTRATION_NOTIFICATION -> server.unregisterMBean(it.mBeanName)
                                else -> {}
                            }
                        }
                    }
                },
                filter, null
            )
        }
    }

    private val driverCoordinates = "org.apache.derby:derby:${name.drop(1).replace('_', '.')}"
    private val classLoader: ClassLoader by lazy {
        DependencyResolver.resolve(driverCoordinates).toClassLoader()
    }
    private val driverClass = ServiceLoader.load(Driver::class.java, classLoader)
        .stream().asSequence()
        .map { it.type() }
        .filter { it.name.startsWith("org.apache.derby") }
        .first().name.also {
            // Check that Derby is not on the test class path (because our custom class loader delegates to its parent)
            shouldThrow<ClassNotFoundException> { Class.forName(it) }
        }

    override fun toString() = "Derby ${name.replace('_', '.')}"

    override fun get(sharedResources: SharedResources): DbSystem.Handle {
        return Handle()
    }

    private inner class Handle : DbSystem.Handle {
        override val type: DatabaseType get() = Companion.databaseType
        private val dataDir = newTempDir("derby-$name", deleteOnExit = false)

        override fun createNamespaceIfNotExists(namespace: SafeIdentifier): SafeIdentifier {
            val dbPath = dbPath(namespace)
            if (!dbPath.exists()) {
                dataSource(dbPath, create = true).work {
                    it.execute("create schema $namespace")
                }
            }
            return namespace
        }

        override fun dropNamespaceIfExists(namespace: SafeIdentifier) {
            val dbPath = dbPath(namespace)
            if (dbPath.exists()) {
                dbPath.toFile().deleteRecursively()
            }
        }

        override fun newAdminConnection(namespace: SafeIdentifier): DataSource {
            return dataSource(dbPath(namespace))
        }

        private fun dataSource(dbPath: Path, create: Boolean = false): DataSource {
            val url = "jdbc:derby:${dbPath.toAbsolutePath()}"
            return object : DriverDataSource(
                classLoader,
                driverClass,
                url,
                "",
                "password",
                mapOf(
                    "create" to "$create",
                    "shutdown" to "false"
                ),
                databaseTypeRegister
            ) {
                override fun getConnection() = synchronized(derbyBootLock) {
                    super.getConnection()
                }

                @Suppress("UsePropertyAccessSyntax")
                override fun getConnection(username: String?, password: String?) = getConnection()
            }
        }

        private fun dbPath(namespace: SafeIdentifier): Path {
            return dataDir.resolve(normalizeCase(namespace).toString()).toAbsolutePath()
        }

        override fun nextMutation(schema: SafeIdentifier?): IndependentDatabaseMutation {
            return DerbyCreateTableMutation(schema?.let(this::normalizeCase), normalizeCase(Names.nextTable()))
        }

        override fun close() {
            dataDir.toFile().deleteRecursively()
        }
    }
}
