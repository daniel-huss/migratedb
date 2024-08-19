/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2024 The MigrateDB contributors
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
package migratedb.v1.core.api.output;

import migratedb.v1.core.api.ErrorDetails;

public class ValidateOutput {
    public final String version;
    public final String description;
    public final String filepath;
    public final ErrorDetails errorDetails;

    public ValidateOutput(String version, String description, String filepath, ErrorDetails errorDetails) {
        this.version = version;
        this.description = description;
        this.filepath = filepath;
        this.errorDetails = errorDetails;
    }
}
