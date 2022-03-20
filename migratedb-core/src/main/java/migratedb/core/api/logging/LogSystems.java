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

import java.util.LinkedHashSet;
import java.util.Set;
import migratedb.core.internal.logging.AndroidLogSystem;
import migratedb.core.internal.logging.ApacheCommonsLogSystem;
import migratedb.core.internal.logging.JavaUtilLogSystem;
import migratedb.core.internal.logging.MultiLogSystem;
import migratedb.core.internal.logging.NoLogSystem;
import migratedb.core.internal.logging.Slf4jLogSystem;
import migratedb.core.internal.util.ClassUtils;
import migratedb.core.internal.util.FeatureDetector;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class LogSystems {
    public static final String ANDROID = "android";
    public static final String APACHE_COMMONS = "apache-commons";
    public static final String AUTO_DETECT = "auto";
    public static final String JAVA_UTIL = "jul";
    public static final String SLF4J = "slf4j";
    public static final String CONSOLE = "console";
    public static final String NONE = "none";

    public static LogSystem parse(Set<String> logSystemNames, ClassLoader classLoader, @Nullable LogSystem fallback) {
        var logSystems = new LinkedHashSet<LogSystem>();
        for (var logSystemName : logSystemNames) {
            switch (logSystemName) {
                case ANDROID:
                    logSystems.add(AndroidLogSystem.INSTANCE);
                    break;
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

    public static LogSystem custom(String className, ClassLoader classLoader) {
        return ClassUtils.instantiate(className, classLoader);
    }

    public static LogSystem autoDetect(ClassLoader classLoader, @Nullable LogSystem fallback) {
        FeatureDetector featureDetector = new FeatureDetector(classLoader);
        if (featureDetector.isAndroidAvailable()) {
            return AndroidLogSystem.INSTANCE;
        }
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
