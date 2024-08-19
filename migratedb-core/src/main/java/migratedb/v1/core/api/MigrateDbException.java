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
package migratedb.v1.core.api;

/**
 * Exception thrown when MigrateDB encounters a problem.
 */
public class MigrateDbException extends RuntimeException {

    private ErrorCode errorCode = ErrorCode.ERROR;

    /**
     * Creates a new MigrateDbException with this message, cause, and error code.
     *
     * @param message   The exception message.
     * @param cause     The exception cause.
     * @param errorCode The error code.
     */
    public MigrateDbException(String message, Throwable cause, ErrorCode errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Creates a new MigrateDbException with this message and error code
     *
     * @param message   The exception message.
     * @param errorCode The error code.
     */
    public MigrateDbException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Creates a new MigrateDbException with this message and this cause.
     *
     * @param message The exception message.
     * @param cause   The exception cause.
     */
    public MigrateDbException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new MigrateDbException with this cause. For use in subclasses that override getMessage().
     *
     * @param cause The exception cause.
     */
    public MigrateDbException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new MigrateDbException with this message.
     *
     * @param message The exception message.
     */
    public MigrateDbException(String message) {
        super(message);
    }

    /**
     * Creates a new MigrateDbException. For use in subclasses that override getMessage().
     */
    public MigrateDbException() {
        super();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
