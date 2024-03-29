/*
 * Copyright 2012-2019 the original author or authors.
 * Copyright 2023 The MigrateDB contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package migratedb.v1.spring.boot.v3.autoconfig;

import migratedb.v1.core.api.configuration.DefaultConfiguration;

/**
 * Implemented by configuration beans that wish to modify the MigrateDB configuration.
 *
 * @author Stephane Nicoll
 * @author Daniel Huss
 */
public interface MigrateDbConfigurationCustomizer {
    void customize(DefaultConfiguration configuration);
}
