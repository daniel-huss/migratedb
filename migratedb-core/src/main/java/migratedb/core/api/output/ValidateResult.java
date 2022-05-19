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

import java.util.List;
import java.util.stream.Collectors;
import migratedb.core.api.ErrorDetails;

public class ValidateResult extends OperationResult {
    /**
     * @deprecated Will be removed in MigrateDb V8. Use {@link #errorDetails} instead
     */
    @Deprecated
    public String validationError;

    public final ErrorDetails errorDetails;
    public final List<ValidateOutput> invalidMigrations;
    public final boolean validationSuccessful;
    public final int validateCount;

    public ValidateResult(
        String migratedbVersion,
        String database,
        ErrorDetails errorDetails,
        boolean validationSuccessful,
        int validateCount,
        List<ValidateOutput> invalidMigrations,
        List<String> warnings,
        String validationError) {
        this.migratedbVersion = migratedbVersion;
        this.database = database;
        this.errorDetails = errorDetails;
        this.validationSuccessful = validationSuccessful;
        this.validateCount = validateCount;
        this.invalidMigrations = invalidMigrations;
        this.validationError = validationError;
        this.operation = "validate";
        warnings.forEach(this::addWarning);
    }

    /**
     * @deprecated Will be removed in MigrateDb V8. Use {@link #ValidateResult(String, String, ErrorDetails, boolean,
     * int, List, List, String)} instead
     */
    public ValidateResult(
        String migratedbVersion,
        String database,
        String validationError,
        boolean validationSuccessful,
        int validateCount,
        List<String> warnings) {
        this(migratedbVersion, database, null, validationSuccessful, validateCount, null, warnings, validationError);
    }

    public String getAllErrorMessages() {
        return invalidMigrations.stream().map(m -> m.errorDetails.errorMessage).collect(Collectors.joining("\n"));
    }
}
