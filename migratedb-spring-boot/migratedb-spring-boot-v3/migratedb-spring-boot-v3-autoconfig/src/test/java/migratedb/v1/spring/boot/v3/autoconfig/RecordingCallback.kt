/*
 * Copyright 2022-2026 The MigrateDB contributors
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

package migratedb.v1.spring.boot.v3.autoconfig

import migratedb.v1.core.api.callback.Callback
import migratedb.v1.core.api.callback.Context
import migratedb.v1.core.api.callback.Event

class RecordingCallback(private val name: String) : Callback {

    private val invocations = mutableListOf<Invocation>()

    fun invocations(): List<Invocation> {
        return synchronized(invocations) {
            invocations.toList()
        }
    }

    override fun supports(event: Event, context: Context): Boolean {
        return true
    }

    override fun canHandleInTransaction(
        event: Event, context: Context
    ): Boolean {
        synchronized(invocations) {
            invocations.add(
                Invocation(
                    this,
                    RecordingCallback::canHandleInTransaction,
                    event,
                    context
                )
            )
        }
        return true
    }

    override fun getCallbackName(): String = name

    override fun handle(
        event: Event, context: Context
    ) {
        synchronized(invocations) {
            invocations.add(
                Invocation(
                    this,
                    RecordingCallback::canHandleInTransaction,
                    event,
                    context
                )
            )
        }
    }
}
