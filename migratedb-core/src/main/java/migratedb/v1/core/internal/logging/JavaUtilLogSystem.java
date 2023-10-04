/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public enum JavaUtilLogSystem implements LogSystem {
    INSTANCE;

    private Logger logger(String logName) {
        // No need to double-cache
        return Logger.getLogger(logName);
    }

    @Override
    public boolean isDebugEnabled(String logName) {
        return logger(logName).isLoggable(Level.FINE);
    }

    @Override
    public void debug(String logName, String message) {
        log(logger(logName), Level.FINE, message, null);
    }

    @Override
    public void info(String logName, String message) {
        log(logger(logName), Level.INFO, message, null);
    }

    @Override
    public void warn(String logName, String message) {
        log(logger(logName), Level.WARNING, message, null);
    }

    @Override
    public void error(String logName, String message) {
        log(logger(logName), Level.SEVERE, message, null);
    }

    @Override
    public void error(String logName, String message, Exception e) {
        log(logger(logName), Level.SEVERE, message, e);
    }

    /**
     * Log the message at the specified level with the specified exception if any.
     */
    private void log(Logger logger, Level level, String message, @Nullable Exception e) {
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(logger.getName());
        record.setThrown(e);
        record.setSourceClassName(logger.getName());
        record.setSourceMethodName(getMethodName(logger));
        logger.log(record);
    }

    /**
     * Computes the source method name for the log output.
     */
    private @Nullable String getMethodName(Logger logger) {
        StackTraceElement[] steArray = new Throwable().getStackTrace();

        for (StackTraceElement stackTraceElement : steArray) {
            if (logger.getName().equals(stackTraceElement.getClassName())) {
                return stackTraceElement.getMethodName();
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "java.util.logging";
    }
}
