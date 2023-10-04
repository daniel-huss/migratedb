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
package migratedb.v1.core.api.logging;

import migratedb.v1.core.internal.util.ClassUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Logging frontend for MigrateDB components and extensions.
 */
public final class Log {
    private static volatile LogSystem defaultLogSystem = LogSystems.autoDetect(ClassUtils.defaultClassLoader(), null);

    // Trade-off: Logging performance will degrade proportionally to the number of concurrent withLogSystem invocations.
    // Anything < 10 should be hardly noticeable.
    private static final List<Map.Entry<Thread, LogSystem>> overrides = new CopyOnWriteArrayList<>();

    public static void setDefaultLogSystem(LogSystem defaultLogSystem) {
        Log.defaultLogSystem = defaultLogSystem;
    }

    /**
     * Runs {@code action} using the given {@code newLogSystem} instead of the default log system for the current
     * thread. The {@code action} must be single-threaded.
     */
    public static <T> T withLogSystem(LogSystem newLogSystem, Supplier<T> action) {
        var override = Map.entry(Thread.currentThread(), newLogSystem);
        try {
            overrides.add(override);
            return action.get();
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

    private final String logName;

    private Log(String logName) {
        this.logName = logName;
    }

    public boolean isDebugEnabled() {
        return getLogSystemForCurrentThread().isDebugEnabled(logName);
    }

    public void debug(@Nullable String message) {
        getLogSystemForCurrentThread().debug(logName, String.valueOf(message));
    }

    public void info(@Nullable String message) {
        getLogSystemForCurrentThread().info(logName, String.valueOf(message));
    }

    public void warn(@Nullable String message) {
        getLogSystemForCurrentThread().warn(logName, String.valueOf(message));
    }

    public void error(@Nullable String message) {
        getLogSystemForCurrentThread().error(logName, String.valueOf(message));
    }

    public void error(@Nullable String message, @Nullable Exception e) {
        if (e == null) {
            getLogSystemForCurrentThread().error(logName, String.valueOf(message));
        } else {
            getLogSystemForCurrentThread().error(logName, String.valueOf(message), e);
        }
    }
}
