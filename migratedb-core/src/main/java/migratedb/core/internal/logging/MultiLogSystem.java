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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import migratedb.core.api.logging.LogAdapter;
import migratedb.core.api.logging.LogSystem;

/**
 * Log implementation that forwards method calls to multiple implementations.
 */
public class MultiLogSystem implements LogSystem {

    private final List<LogSystem> delegates;

    public MultiLogSystem(Collection<LogSystem> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public String toString() {
        return delegates.toString();
    }

    @Override
    public LogAdapter createLogAdapter(String logName) {
        return new Adpater(delegates.stream()
                                    .map(it -> it.createLogAdapter(logName))
                                    .collect(Collectors.toUnmodifiableList()));
    }

    private static final class Adpater implements LogAdapter {
        private final List<LogAdapter> logAdapters;

        Adpater(List<LogAdapter> logAdapters) {
            this.logAdapters = logAdapters;
        }

        @Override
        public boolean isDebugEnabled() {
            for (var log : logAdapters) {
                if (!log.isDebugEnabled()) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void debug(String message) {
            for (var log : logAdapters) {
                log.debug(message);
            }
        }

        @Override
        public void info(String message) {
            for (var log : logAdapters) {
                log.info(message);
            }
        }

        @Override
        public void warn(String message) {
            for (var log : logAdapters) {
                log.warn(message);
            }
        }

        @Override
        public void error(String message) {
            for (var log : logAdapters) {
                log.error(message);
            }
        }

        @Override
        public void error(String message, Exception e) {
            for (var log : logAdapters) {
                log.error(message, e);
            }
        }
    }
}
