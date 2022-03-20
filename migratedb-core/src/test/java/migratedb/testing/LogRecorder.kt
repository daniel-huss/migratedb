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

package migratedb.testing

import migratedb.core.api.logging.LogSystem
import java.util.*

open class LogRecorder(entries: MutableList<LogEntry>? = null, val name: String = "") : LogSystem {
    class LogEntry(val logName: String, val message: String?, val exception: Exception?, val thread: Thread)

    val entries: MutableList<LogEntry> = entries ?: Collections.synchronizedList(mutableListOf())

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
