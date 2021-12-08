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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import migratedb.core.api.logging.LogAdapter;
import migratedb.core.api.logging.LogSystem;

public enum JavaUtilLogSystem implements LogSystem {
    INSTANCE;

    @Override
    public String toString() {
        return "java.util.logging";
    }

    @Override
    public LogAdapter createLogAdapter(String logName) {
        return new Adapter(Logger.getLogger(logName));
    }

    private static final class Adapter implements LogAdapter {

        private final Logger logger;

        /**
         * Creates a new wrapper around this logger.
         *
         * @param logger The original java.util Logger.
         */
        Adapter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isLoggable(Level.FINE);
        }

        public void debug(String message) {
            log(Level.FINE, message, null);
        }

        public void info(String message) {
            log(Level.INFO, message, null);
        }

        public void warn(String message) {
            log(Level.WARNING, message, null);
        }

        public void error(String message) {
            log(Level.SEVERE, message, null);
        }

        public void error(String message, Exception e) {
            log(Level.SEVERE, message, e);
        }

        /**
         * Log the message at the specified level with the specified exception if any.
         */
        private void log(Level level, String message, Exception e) {
            LogRecord record = new LogRecord(level, message);
            record.setLoggerName(logger.getName());
            record.setThrown(e);
            record.setSourceClassName(logger.getName());
            record.setSourceMethodName(getMethodName());
            logger.log(record);
        }

        /**
         * Computes the source method name for the log output.
         */
        private String getMethodName() {
            StackTraceElement[] steArray = new Throwable().getStackTrace();

            for (StackTraceElement stackTraceElement : steArray) {
                if (logger.getName().equals(stackTraceElement.getClassName())) {
                    return stackTraceElement.getMethodName();
                }
            }

            return null;
        }
    }
}
