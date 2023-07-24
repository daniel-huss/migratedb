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
package migratedb.v1.core.internal.util

import io.kotest.matchers.shouldBe
import migratedb.v1.core.internal.util.StringUtils.trimChar
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class StringUtilsTest {
    @ParameterizedTest
    @ValueSource(
        strings = [
            "''                      ' '          -> ''",
            "'/'                     '/'          -> ''",
            "'//'                    '/'          -> ''",
            "'///'                   '/'          -> ''",
            "'  /  '                 '/'          -> '  /  '",
            "'/foo/bar'              '/'          -> 'foo/bar'",
            "'bar/foo/'              '/'          -> 'bar/foo'",
            "'baz/bar'               '/'          -> 'baz/bar'",
            "'///foo/bar///'         '/'          -> 'foo/bar'",
        ]
    )
    fun `trimChar() works`(spec: String) {
        val match = trimCharSpec.matchEntire(spec) ?: throw IllegalArgumentException(spec)
        val (input, trimmed, expected) = match.groupValues.drop(1)
        trimChar(input, trimmed.single()).shouldBe(expected)
    }

    private companion object {
        // "'input' 'char' 'expected'", with optional unquoted stuff in-between
        val trimCharSpec = Regex("'([^']*)'[^']*'([^'])'[^']*'([^']*)'")
    }
}
