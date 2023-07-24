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

package migratedb.v1.core.api.logging;

import migratedb.v1.core.internal.logging.*;
import migratedb.v1.core.internal.util.ClassUtils;
import migratedb.v1.core.internal.util.FeatureDetector;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Pre-defined log system names and auto-detection.
 */
public final class LogSystems {
    /**
     * {@code org.apache.commons.logging} system.
     */
    public static final String APACHE_COMMONS = "apache-commons";
    /**
     * Auto-detect based on environment. See {@link #autoDetect(ClassLoader, LogSystem)}.
     */
    public static final String AUTO_DETECT = "auto";
    /**
     * {@code java.util.logging} system.
     */
    public static final String JAVA_UTIL = "jul";
    /**
     * {@code org.slf4j} system.
     */
    public static final String SLF4J = "slf4j";
    /**
     * An alias that means the fallback logger will be used (this library has no opinion on what a "console" log should
     * be).
     */
    public static final String CONSOLE = "console";
    /**
     * Suppresses all log messages.
     */
    public static final String NONE = "none";

    /**
     * Creates a log system that forwards logging calls to all log system names in {@code logSystemNames}. Each log
     * system name must either be one of the string constants in this class or the fully qualified name of a class that
     * implements {@link LogSystem} and has a public no-arg constructor.
     *
     * @param logSystemNames The log system names to parse
     * @param classLoader    Used to instantiate classes by name
     * @param fallback       Used for {@link #AUTO_DETECT} and {@link #CONSOLE} (the latter being a direct alias for
     *                       it)
     * @return Log system that delegates to all log systems identified by {@code logSystemNames}.
     */
    public static LogSystem fromStrings(Set<String> logSystemNames, ClassLoader classLoader,
                                        @Nullable LogSystem fallback) {
        var logSystems = new LinkedHashSet<LogSystem>();
        for (var logSystemName : logSystemNames) {
            switch (logSystemName.toLowerCase(Locale.ROOT)) {
                case APACHE_COMMONS:
                    logSystems.add(ApacheCommonsLogSystem.INSTANCE);
                    break;
                case AUTO_DETECT:
                    logSystems.add(autoDetect(classLoader, fallback));
                    break;
                case JAVA_UTIL:
                    logSystems.add(JavaUtilLogSystem.INSTANCE);
                    break;
                case NONE:
                    break;
                case SLF4J:
                    logSystems.add(Slf4jLogSystem.INSTANCE);
                    break;
                case CONSOLE:
                    if (fallback != null) {
                        logSystems.add(fallback);
                    }
                    break;
                default:
                    logSystems.add(custom(logSystemName, classLoader));
                    break;
            }
        }
        switch (logSystems.size()) {
            case 0:
                return NoLogSystem.INSTANCE;
            case 1:
                return logSystems.iterator().next();
            default:
                return new MultiLogSystem(logSystems);
        }
    }

    /**
     * Instantiates a class that implements {@link LogSystem}.
     *
     * @param className   Fully qualified name of class to instantiate. Class must have a public no-arg constructor.
     * @param classLoader Class loader to use.
     * @return The custom log system.
     */
    public static LogSystem custom(String className, ClassLoader classLoader) {
        return ClassUtils.instantiate(className, classLoader);
    }

    /**
     * Auto-detects the "best" available log system and returns an instance of it. The order of precedence is:
     * <ol>
     *     <li>If one of the supported logging libraries is found via {@code classLoader}, use it.</li>
     *     <li>If {@code fallback} is non-null, use that.</li>
     *     <li>Use {@code java.util.logging}.</li>
     * </ol>
     *
     * @param classLoader Used to check for the presence of supported logging libraries.
     * @param fallback    Log system to use over {@code java.util.logging}
     * @return Auto-detected log system.
     */
    public static LogSystem autoDetect(ClassLoader classLoader, @Nullable LogSystem fallback) {
        FeatureDetector featureDetector = new FeatureDetector(classLoader);
        if (featureDetector.isSlf4jAvailable()) {
            return Slf4jLogSystem.INSTANCE;
        }
        if (featureDetector.isApacheCommonsLoggingAvailable()) {
            return ApacheCommonsLogSystem.INSTANCE;
        }
        if (fallback == null) {
            return JavaUtilLogSystem.INSTANCE;
        }
        return fallback;
    }
}
