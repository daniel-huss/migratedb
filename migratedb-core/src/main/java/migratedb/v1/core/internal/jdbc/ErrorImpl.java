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
package migratedb.v1.core.internal.jdbc;

import migratedb.v1.core.api.callback.Error;

public class ErrorImpl implements Error {
    private final int code;
    private final String state;
    private final String message;
    private boolean handled;

    /**
     * An error that occurred while executing a statement.
     *
     * @param code    The error code.
     * @param state   The error state.
     * @param message The error message.
     */
    public ErrorImpl(int code, String state, String message) {
        this.code = code;
        this.state = state;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public boolean isHandled() {
        return handled;
    }

    @Override
    public void setHandled(boolean handled) {
        this.handled = handled;
    }
}
