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
package migratedb.core.api.logging

import io.kotest.matchers.collections.shouldContainExactly
import migratedb.core.MigrateDb
import migratedb.core.api.callback.Callback
import migratedb.core.api.callback.Context
import migratedb.core.api.callback.Event
import migratedb.core.api.migration.BaseJavaMigration
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal class LogTest {
    @Test
    fun `withLogSystem binds LogSystem to current thread`() {
        // given
        val loggerByThread = (1..20).associate { i ->
            // Each thread gets its own log recorder
            val logger = LogRecorder(name = "$i")
            val thread = Thread {
                MigrateDb.configure()
                    .callbacks(LogMessageProducer())
                    .javaMigrations(V000__Do_Nothing())
                    .dataSource(JdbcDataSource().apply { setUrl("jdbc:h2:mem:") })
                    .logger(logger)
                    .load().also {
                        it.info()
                        it.migrate()
                    }
            }
            thread.name = "$i"
            thread.isDaemon = true
            thread to logger
        }

        // when
        loggerByThread.keys.forEach { it.start() }
        loggerByThread.keys.forEach { it.join(10_000) }

        // then
        loggerByThread.forEach { (expectedThread, logger) ->
            val seenThreads = logger.entries.asSequence().map { it.thread.name.toInt() }.toSortedSet()
            seenThreads.shouldContainExactly(expectedThread.name.toInt())
        }
    }

    @Suppress("ClassName")
    class V000__Do_Nothing : BaseJavaMigration() {
        override fun migrate(context: migratedb.core.api.migration.Context) {}
    }

    class LogMessageProducer : Callback {
        private val log = Log.getLog(LogMessageProducer::class.java)
        override fun supports(event: Event?, context: Context?) = true
        override fun canHandleInTransaction(event: Event?, context: Context?) = true
        override fun handle(event: Event?, context: Context?) = log.info("$event")
        override fun getCallbackName() = "Log message producer"
    }

    data class LogEntry(val logName: String, val message: String?, val exception: Exception?, val thread: Thread)

    class LogRecorder(entries: MutableSet<LogEntry>? = null, val name: String = "") : LogSystem {
        val entries: MutableSet<LogEntry> = entries ?: Collections.newSetFromMap(ConcurrentHashMap())

        private fun record(logName: String, message: String?, exception: Exception? = null) {
            entries.add(LogEntry(logName, message, exception, Thread.currentThread()))
        }

        override fun isDebugEnabled(logName: String) = true

        override fun debug(logName: String, message: String) = record(logName, message)
        override fun info(logName: String, message: String) = record(logName, message)
        override fun warn(logName: String, message: String) = record(logName, message)
        override fun error(logName: String, message: String) = record(logName, message)
        override fun error(logName: String, message: String, e: Exception?) = record(logName, message, e)
    }
}
