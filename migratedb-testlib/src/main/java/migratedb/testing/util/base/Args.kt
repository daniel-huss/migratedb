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

package migratedb.testing.util.base

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import java.util.stream.Stream
import kotlin.streams.asStream

abstract class Args private constructor(
    private val args: () -> Sequence<List<Any?>>
) : ArgumentsProvider {

    enum class OneParam {
        YES
    }

    constructor(vararg args: List<Any?>) : this({ args.asSequence() })

    @Suppress("UNUSED_PARAMETER")
    constructor(functionHasOneParameter: OneParam, vararg args: Any?) : this({ args.asSequence().map { listOf(it) } })


    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return args().asStream().map { Arguments.arguments(*it.toTypedArray()) }
    }
}
