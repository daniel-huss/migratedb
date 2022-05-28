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

package migratedb.integrationtest.util.base

import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.SingleConnectionDataSource
import java.sql.Connection
import java.sql.SQLException
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

val leakedConnections: MutableMap<Any, String> = Collections.synchronizedMap(IdentityHashMap())

fun trackConnectionLeaks(ds: DataSource): DataSource {
    return object : DataSource by ds {
        fun stack(): String {
            val stack = mutableListOf<String>()
            StackWalker.getInstance().forEach {
                if (it.className.startsWith("migratedb")) {
                    stack.add(it.className + "::" + it.methodName + "::" + it.lineNumber)
                }
            }
            return stack.drop(2).joinToString("\n  ")
        }

        override fun getConnection(): Connection {
            val delegate = ds.connection
            leakedConnections[delegate] = stack()
            return object : Connection by delegate {
                override fun close() {
                    try {
                        delegate.close()
                    } finally {
                        leakedConnections.remove(delegate)
                    }
                }
            }
        }
    }
}

fun DataAccessException.rethrowUnless(matcher: (SQLException) -> Boolean) {
    if (!matcher((cause as? SQLException) ?: throw this)) {
        throw this
    }
}

fun <T> Connection.work(schema: CharSequence? = null, commit: Boolean = true, action: (JdbcTemplate) -> T): T {
    var oldSchema: String? = null
    schema?.let {
        oldSchema = this.schema
        this.schema = it.toString()
    }
    try {
        return action(JdbcTemplate(SingleConnectionDataSource(this, true))).also {
            if (commit && !autoCommit) commit()
        }
    } finally {
        oldSchema?.let {
            this.schema = it
        }
    }
}

fun <T> DataSource.work(
    connectTimeout: Duration = Duration.ofSeconds(10),
    schema: CharSequence? = null,
    action: (JdbcTemplate) -> T
): T {
    return awaitConnectivity(connectTimeout).use { it.work(schema, true, action) }
}

fun DataSource.awaitConnectivity(timeout: Duration = Duration.ofSeconds(10)): Connection {
    val delays = arrayOf(0, 100, 200, 500) // milliseconds
    var i = 0
    val timeoutNanos = timeout.toNanos()
    val start = System.nanoTime()
    var conn: Connection? = null
    while (true) {
        try {
            conn = connection
            if (!conn.isValid(1)) throw SQLException()
            return conn
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            runCatching {
                conn?.close()
            }.exceptionOrNull()?.let {
                if (it is InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                e.addSuppressed(it)
            }
            if (e !is SQLException) throw e
            val delay = delays[i.coerceAtMost(delays.size - 1)].toLong()
            val elapsed = System.nanoTime() - start
            if (elapsed + TimeUnit.MILLISECONDS.toNanos(delay) < timeoutNanos) {
                Thread.sleep(delay)
                i++
            } else {
                throw IllegalStateException("Timeout waiting for connectivity", e)
            }
        }
    }
}
