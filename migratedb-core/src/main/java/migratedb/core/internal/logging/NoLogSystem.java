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

package migratedb.core.internal.logging;

import migratedb.core.api.logging.LogAdapter;
import migratedb.core.api.logging.LogSystem;

public enum NoLogSystem implements LogSystem {
    INSTANCE;

    private static final Adapter ADAPTER_INSTANCE = new Adapter();

    @Override
    public LogAdapter createLogAdapter(String logName) {
        return ADAPTER_INSTANCE;
    }

    private static final class Adapter implements LogAdapter {

        @Override
        public boolean isDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String message) {
        }

        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Exception e) {
        }
    }
}
