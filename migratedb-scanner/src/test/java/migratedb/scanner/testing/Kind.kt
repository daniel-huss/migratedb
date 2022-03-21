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

package migratedb.scanner.testing

import org.objectweb.asm.Opcodes

enum class Kind(val opcode: Int) {
    PLAIN_CLASS(0), ENUM(Opcodes.ACC_ENUM), ANNOTATION(Opcodes.ACC_ANNOTATION), INTERFACE(Opcodes.ACC_INTERFACE);
}
