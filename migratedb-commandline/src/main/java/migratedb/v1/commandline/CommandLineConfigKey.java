/*
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

package migratedb.v1.commandline;

import java.util.HashMap;
import java.util.Map;

final class CommandLineConfigKey {
    static final String JAR_DIRS = "migratedb.jarDirs";
    static final String CONFIG_FILES = "migratedb.configFiles";
    static final String CONFIG_FILE_ENCODING = "migratedb.configFileEncoding";
    static final String URL = "migratedb.url";
    static final String USER = "migratedb.user";
    static final String PASSWORD = "migratedb.password";
    static final String DRIVER = "migratedb.driver";
    static final String JDBC_PROPERTIES_PREFIX = "migratedb.jdbcProperties.";

    static Map<String, String> getJdbcProperties(Map<String, String> configuration) {
        var result = new HashMap<String, String>();
        for (var entry : configuration.entrySet()) {
            if (entry.getKey().startsWith(JDBC_PROPERTIES_PREFIX)) {
                result.put(entry.getKey().substring(JDBC_PROPERTIES_PREFIX.length()), entry.getValue());
            }
        }
        return result;
    }
}
