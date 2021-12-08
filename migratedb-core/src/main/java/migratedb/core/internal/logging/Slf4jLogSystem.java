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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Slf4jLogSystem implements LogSystem {
    INSTANCE;

    @Override
    public LogAdapter createLogAdapter(String logName) {
        return new Adapter(LoggerFactory.getLogger(logName));
    }

    @Override
    public String toString() {
        return "org.slf4j";
    }

    private static final class Adapter implements LogAdapter {

        private final Logger logger;

        /**
         * Creates a new wrapper around this logger.
         *
         * @param logger The original Slf4j Logger.
         */
        Adapter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isDebugEnabled();
        }

        public void debug(String message) {
            logger.debug(message);
        }

        public void info(String message) {
            logger.info(message);
        }

        public void warn(String message) {
            logger.warn(message);
        }

        public void error(String message) {
            logger.error(message);
        }

        public void error(String message, Exception e) {
            logger.error(message, e);
        }
    }
}
