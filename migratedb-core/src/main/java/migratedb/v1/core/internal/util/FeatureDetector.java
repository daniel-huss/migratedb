/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2024 The MigrateDB contributors
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
package migratedb.v1.core.internal.util;

public final class FeatureDetector {
    private final ClassLoader classLoader;
    private Boolean apacheCommonsLoggingAvailable;
    private Boolean slf4jAvailable;

    public FeatureDetector(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public boolean isApacheCommonsLoggingAvailable() {
        if (apacheCommonsLoggingAvailable == null) {
            apacheCommonsLoggingAvailable = ClassUtils.isPresent("org.apache.commons.logging.Log", classLoader);
        }

        return apacheCommonsLoggingAvailable;
    }

    public boolean isSlf4jAvailable() {
        if (slf4jAvailable == null) {
            // We need to ensure there's an actual implementation; AWS SDK pulls in the Logger interface but doesn't
            // provide any implementation, causing SLF4J to drop what we want to be console output on the floor.
            // Versions up to 1.7 have a StaticLoggerBinder
            slf4jAvailable = ClassUtils.isPresent("org.slf4j.Logger", classLoader)
                    && ClassUtils.isPresent("org.slf4j.impl.StaticLoggerBinder", classLoader);
            // Versions 1.8 and later use a ServiceLocator to bind to the implementation
            slf4jAvailable |= ClassUtils.isImplementationPresent("org.slf4j.spi.SLF4JServiceProvider", classLoader);
        }

        return slf4jAvailable;
    }
}
