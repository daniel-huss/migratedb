/*
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
package migratedb.v1.commandline;

import migratedb.v1.core.api.ErrorCode;
import migratedb.v1.core.api.output.OperationResult;
import migratedb.v1.core.internal.info.BuildInfo;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = { TypeUseLocation.FIELD, TypeUseLocation.PARAMETER })
public class ErrorOutput extends OperationResult {
    public static class ErrorOutputItem {
        public ErrorCode errorCode;
        public String message;
        public String stackTrace;

        ErrorOutputItem(ErrorCode errorCode, String message, String stackTrace) {
            this.errorCode = errorCode;
            this.message = message;
            this.stackTrace = stackTrace;
        }
    }

    public ErrorOutputItem error;

    public ErrorOutput(ErrorCode errorCode,
                       String message,
                       String stackTrace,
                       String operation) {
        this.error = new ErrorOutputItem(errorCode, message, stackTrace);
        this.operation = operation;
        this.migratedbVersion = BuildInfo.VERSION;
    }
}
