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
package migratedb.core.api.internal.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import migratedb.core.api.callback.Error;
import migratedb.core.api.callback.Warning;
import migratedb.core.internal.jdbc.Result;

/**
 * Container for all results, warnings, errors and remaining side-effects of a sql statement.
 */
public class Results {
    public static final Results EMPTY_RESULTS = new Results();

    private final List<Result> results = new ArrayList<>();
    private final List<Warning> warnings = new ArrayList<>();
    private final List<Error> errors = new ArrayList<>();
    private SQLException exception = null;

    public void addResult(Result result) {
        results.add(result);
    }

    public void addWarning(Warning warning) {
        warnings.add(warning);
    }

    public void addError(Error error) {
        errors.add(error);
    }

    public List<Warning> getWarnings() {
        return warnings;
    }

    public List<Error> getErrors() {
        return errors;
    }

    public List<Result> getResults() {
        return results;
    }

    public SQLException getException() {
        return exception;
    }

    public void setException(SQLException exception) {
        this.exception = exception;
    }
}