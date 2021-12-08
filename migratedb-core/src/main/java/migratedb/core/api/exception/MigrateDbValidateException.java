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
package migratedb.core.api.exception;

import migratedb.core.api.ErrorDetails;
import migratedb.core.api.MigrateDbException;
import migratedb.core.internal.util.MigrateDbWebsiteLinks;

/**
 * Exception thrown when MigrateDB encounters a problem with Validate.
 */
public class MigrateDbValidateException extends MigrateDbException {

    public MigrateDbValidateException(ErrorDetails errorDetails, String allValidateMessages) {
        super("Validate failed: " + errorDetails.errorMessage + "\n" + allValidateMessages +
              "\nNeed more flexibility with validation rules? Learn more: " +
              MigrateDbWebsiteLinks.CUSTOM_VALIDATE_RULES, errorDetails.errorCode);
    }

}
