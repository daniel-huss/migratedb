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

package migratedb.v1.spring.boot.v4.autoconfig

import java.util.concurrent.atomic.AtomicLong

class Invocation private constructor(
    private val order: Long, val receiver: Any, val function: Function<*>, val args: List<Any?>
) {

    constructor(receiver: Any, function: Function<*>, vararg args: Any?) : this(
        nextInvocation(),
        receiver,
        function,
        args.toList()
    )

    fun wasBefore(other: Invocation) = other.order > order
    fun wasAfter(other: Invocation) = other.order < order

    companion object {

        private fun nextInvocation() = count.getAndIncrement()

        private val count = AtomicLong()
    }
}
