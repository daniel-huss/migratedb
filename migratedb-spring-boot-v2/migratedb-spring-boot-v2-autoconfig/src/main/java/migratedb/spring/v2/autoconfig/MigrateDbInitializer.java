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

package migratedb.spring.v2.autoconfig;

import migratedb.core.MigrateDb;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;

import java.util.Objects;

/**
 * This is reported by {@link MigrateDbInitializerDetector} to be a database initializer. It executes MigrateDB actions
 * as part of database initialization.
 *
 * @author Phillip Webb
 * @author Daniel Huss
 * @since 1.3.0
 */
public class MigrateDbInitializer implements InitializingBean, Ordered {
    private final MigrateDb migrateDb;
    private final MigrateDbExecution migrateDbExecution;
    private int order = 0;

    public MigrateDbInitializer(MigrateDb migrateDb, MigrateDbExecution migrateDbExecution) {
        this.migrateDb = Objects.requireNonNull(migrateDb);
        this.migrateDbExecution = Objects.requireNonNull(migrateDbExecution);
    }

    @Override
    public void afterPropertiesSet() {
        migrateDbExecution.run(migrateDb);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
