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
package migratedb.core.api.logging;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import migratedb.core.internal.util.ClassUtils;

/**
 * Logging frontend for MigrateDB components and extensions.
 */
public final class Log {
    private static volatile LogSystem defaultLogSystem = LogSystems.autoDetect(ClassUtils.defaultClassLoader(), null);

    // Grows proportionally to the number of concurrent withLogSystem invocations.
    private static final List<Map.Entry<Thread, LogSystem>> overrides = new CopyOnWriteArrayList<>();

    public static void setDefaultLogSystem(LogSystem defaultLogSystem) {
        Log.defaultLogSystem = defaultLogSystem;
    }

    /**
     * Runs {@code action} using the given {@code newLogSystem} instead of the default log system for the current
     * thread. The {@code action} must be single-threaded.
     */
    public static void withLogSystem(LogSystem newLogSystem, Runnable action) {
        var override = Map.entry(Thread.currentThread(), newLogSystem);
        try {
            overrides.add(override);
            action.run();
        } finally {
            overrides.removeIf(it -> it == override);
        }
    }

    private LogSystem getLogSystemForCurrentThread() {
        var currentThread = Thread.currentThread();
        for (var override : overrides) {
            if (override.getKey() == currentThread) {
                return override.getValue();
            }
        }
        return defaultLogSystem;
    }

    public static Log getLog(Class<?> klass) {
        return new Log(klass.getName());
    }

    private volatile LogAdapter delegate;
    private volatile LogSystem logSystem;
    private final String logName;

    private Log(String logName) {
        this.logName = logName;
    }

    private void checkIfLogSystemHasChanged() {
        LogSystem currentLogSystem = getLogSystemForCurrentThread();
        if (delegate == null || logSystem != currentLogSystem) {
            delegate = currentLogSystem.createLogAdapter(logName);
            logSystem = currentLogSystem;
        }
        assert delegate != null;
    }

    public boolean isDebugEnabled() {
        checkIfLogSystemHasChanged();
        return delegate.isDebugEnabled();
    }

    public void debug(String message) {
        checkIfLogSystemHasChanged();
        delegate.debug(message);
    }

    public void info(String message) {
        checkIfLogSystemHasChanged();
        delegate.info(message);
    }

    public void warn(String message) {
        checkIfLogSystemHasChanged();
        delegate.warn(message);
    }

    public void error(String message) {
        checkIfLogSystemHasChanged();
        delegate.error(message);
    }

    public void error(String message, Exception e) {
        checkIfLogSystemHasChanged();
        delegate.error(message, e);
    }
}