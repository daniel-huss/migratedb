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

import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;

import java.util.Set;

/**
 * Tells Spring that MigrateDB is part of the database initialization.
 *
 * @author Andy Wilkinson
 * @author Daniel Huss
 */
public class MigrateDbInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {
    @Override
    protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
        return Set.of(MigrateDbInitializer.class);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
