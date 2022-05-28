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

import com.google.common.base.Stopwatch
import kotlin.time.Duration

fun eventually(duration: Duration, poll: Duration, block: () -> Unit) {
    val start = System.nanoTime()
    var error: Throwable?
    var wait: Boolean
    Stopwatch.createStarted().let {
        do {
            wait = false
            try {
                block()
                return
            } catch (e: InterruptedException) {
                throw e
            } catch (e: Exception) {
                wait = true
                error = e
            } catch (e: AssertionError) {
                wait = true
                error = e
            }
            if (wait) {
                Thread.sleep(poll.inWholeMilliseconds)
            }
        } while (System.nanoTime() - start < duration.inWholeNanoseconds)
    }
    throw IllegalStateException("Timeout after $duration", error)
}
