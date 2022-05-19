/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.core.api.output;

import java.util.LinkedList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = { TypeUseLocation.FIELD, TypeUseLocation.PARAMETER })
public abstract class OperationResult {
    public String migratedbVersion;
    public String database;
    public List<String> warnings;
    public String operation;

    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new LinkedList<>();
        }
        warnings.add(warning);
    }
}
