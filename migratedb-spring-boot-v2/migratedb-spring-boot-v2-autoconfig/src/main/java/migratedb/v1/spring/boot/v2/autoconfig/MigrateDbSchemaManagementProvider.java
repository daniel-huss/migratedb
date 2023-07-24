/*
 * Copyright 2012-2023 the original author or authors.
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

package migratedb.v1.spring.boot.v2.autoconfig;

import migratedb.v1.core.MigrateDb;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.jdbc.SchemaManagementProvider;

import javax.sql.DataSource;
import java.util.Objects;

/**
 * Determines if the schema is managed by looking at available {@link MigrateDb} instances.
 *
 * @author Stephane Nicoll
 * @author Daniel Huss
 */
public class MigrateDbSchemaManagementProvider implements SchemaManagementProvider {
    private final ObjectProvider<MigrateDb> migrateDbManagedDataSources;

    public MigrateDbSchemaManagementProvider(ObjectProvider<MigrateDb> migrateDbManagedDataSources) {
        this.migrateDbManagedDataSources = migrateDbManagedDataSources;
    }

    @Override
    public SchemaManagement getSchemaManagement(DataSource dataSource) {
        return migrateDbManagedDataSources.orderedStream()
                .map(it -> it.getConfiguration().getExtensionConfig(SpringIntegration.class))
                .filter(Objects::nonNull)
                .map(SpringIntegration::getManagedDataSource)
                .filter(Objects::nonNull)
                .filter(dataSource::equals)
                .findFirst()
                .map((managedDataSource) -> SchemaManagement.MANAGED)
                .orElse(SchemaManagement.UNMANAGED);
    }
}
