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
package migratedb.v1.core.internal.logging;

import migratedb.v1.core.api.logging.LogSystem;

public enum ApacheCommonsLogSystem implements LogSystem {
    INSTANCE;

    private org.apache.commons.logging.Log logger(String logName) {
        // No need to double-cache these
        return org.apache.commons.logging.LogFactory.getLog(logName);
    }

    @Override
    public boolean isDebugEnabled(String logName) {
        return logger(logName).isDebugEnabled();
    }

    @Override
    public void debug(String logName, String message) {
        logger(logName).debug(message);
    }

    @Override
    public void info(String logName, String message) {
        logger(logName).info(message);
    }

    @Override
    public void warn(String logName, String message) {
        logger(logName).warn(message);
    }

    @Override
    public void error(String logName, String message) {
        logger(logName).error(message);
    }

    @Override
    public void error(String logName, String message, Exception e) {
        logger(logName).error(message, e);
    }

    @Override
    public String toString() {
        return "org.apache.commons.logging";
    }
}
