/*
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

package migratedb.commandline;

import static migratedb.commandline.CommandLineConfigKey.CONFIG_FILES;
import static migratedb.commandline.CommandLineConfigKey.CONFIG_FILE_ENCODING;
import static migratedb.commandline.CommandLineConfigKey.JAR_DIRS;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_DESCRIPTION;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_MIGRATION_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_ON_MIGRATE;
import static migratedb.core.api.configuration.PropertyNames.BASELINE_VERSION;
import static migratedb.core.api.configuration.PropertyNames.BATCH;
import static migratedb.core.api.configuration.PropertyNames.CALLBACKS;
import static migratedb.core.api.configuration.PropertyNames.CHERRY_PICK;
import static migratedb.core.api.configuration.PropertyNames.CLEAN_DISABLED;
import static migratedb.core.api.configuration.PropertyNames.CLEAN_ON_VALIDATION_ERROR;
import static migratedb.core.api.configuration.PropertyNames.CONNECT_RETRIES;
import static migratedb.core.api.configuration.PropertyNames.CONNECT_RETRIES_INTERVAL;
import static migratedb.core.api.configuration.PropertyNames.CREATE_SCHEMAS;
import static migratedb.core.api.configuration.PropertyNames.DEFAULT_SCHEMA;
import static migratedb.core.api.configuration.PropertyNames.DRIVER;
import static migratedb.core.api.configuration.PropertyNames.DRYRUN_OUTPUT;
import static migratedb.core.api.configuration.PropertyNames.ENCODING;
import static migratedb.core.api.configuration.PropertyNames.ERROR_OVERRIDES;
import static migratedb.core.api.configuration.PropertyNames.FAIL_ON_MISSING_LOCATIONS;
import static migratedb.core.api.configuration.PropertyNames.GROUP;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_FUTURE_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_IGNORED_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_MIGRATION_PATTERNS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_MISSING_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.IGNORE_PENDING_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.INIT_SQL;
import static migratedb.core.api.configuration.PropertyNames.INSTALLED_BY;
import static migratedb.core.api.configuration.PropertyNames.JDBC_PROPERTIES_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.LOCATIONS;
import static migratedb.core.api.configuration.PropertyNames.LOCK_RETRY_COUNT;
import static migratedb.core.api.configuration.PropertyNames.LOGGER;
import static migratedb.core.api.configuration.PropertyNames.MIXED;
import static migratedb.core.api.configuration.PropertyNames.OUTPUT_QUERY_RESULTS;
import static migratedb.core.api.configuration.PropertyNames.OUT_OF_ORDER;
import static migratedb.core.api.configuration.PropertyNames.PASSWORD;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDERS_PROPERTY_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDER_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDER_REPLACEMENT;
import static migratedb.core.api.configuration.PropertyNames.PLACEHOLDER_SUFFIX;
import static migratedb.core.api.configuration.PropertyNames.REPEATABLE_SQL_MIGRATION_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.RESOLVERS;
import static migratedb.core.api.configuration.PropertyNames.SCHEMAS;
import static migratedb.core.api.configuration.PropertyNames.SCRIPT_PLACEHOLDER_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.SCRIPT_PLACEHOLDER_SUFFIX;
import static migratedb.core.api.configuration.PropertyNames.SKIP_DEFAULT_CALLBACKS;
import static migratedb.core.api.configuration.PropertyNames.SKIP_DEFAULT_RESOLVERS;
import static migratedb.core.api.configuration.PropertyNames.SKIP_EXECUTING_MIGRATIONS;
import static migratedb.core.api.configuration.PropertyNames.SQL_MIGRATION_PREFIX;
import static migratedb.core.api.configuration.PropertyNames.SQL_MIGRATION_SEPARATOR;
import static migratedb.core.api.configuration.PropertyNames.SQL_MIGRATION_SUFFIXES;
import static migratedb.core.api.configuration.PropertyNames.TABLE;
import static migratedb.core.api.configuration.PropertyNames.TABLESPACE;
import static migratedb.core.api.configuration.PropertyNames.TARGET;
import static migratedb.core.api.configuration.PropertyNames.URL;
import static migratedb.core.api.configuration.PropertyNames.USER;
import static migratedb.core.api.configuration.PropertyNames.VALIDATE_MIGRATION_NAMING;
import static migratedb.core.api.configuration.PropertyNames.VALIDATE_ON_MIGRATE;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_KERBEROS_CACHE_FILE;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_KERBEROS_CONFIG_FILE;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_SQLPLUS;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_SQLPLUS_WARN;
import static migratedb.core.internal.database.oracle.OracleConfig.ORACLE_WALLET_LOCATION;

import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

final class EnvironmentMapper {
    static @Nullable String convertKey(String key) {
        if ("MIGRATEDB_BASELINE_DESCRIPTION".equals(key)) {
            return BASELINE_DESCRIPTION;
        }
        if ("MIGRATEDB_BASELINE_ON_MIGRATE".equals(key)) {
            return BASELINE_ON_MIGRATE;
        }
        if ("MIGRATEDB_BASELINE_VERSION".equals(key)) {
            return BASELINE_VERSION;
        }
        if ("MIGRATEDB_BATCH".equals(key)) {
            return BATCH;
        }
        if ("MIGRATEDB_CALLBACKS".equals(key)) {
            return CALLBACKS;
        }
        if ("MIGRATEDB_CLEAN_DISABLED".equals(key)) {
            return CLEAN_DISABLED;
        }
        if ("MIGRATEDB_CLEAN_ON_VALIDATION_ERROR".equals(key)) {
            return CLEAN_ON_VALIDATION_ERROR;
        }
        if ("MIGRATEDB_CONFIG_FILE_ENCODING".equals(key)) {
            return CONFIG_FILE_ENCODING;
        }
        if ("MIGRATEDB_CONFIG_FILES".equals(key)) {
            return CONFIG_FILES;
        }
        if ("MIGRATEDB_CONNECT_RETRIES".equals(key)) {
            return CONNECT_RETRIES;
        }
        if ("MIGRATEDB_CONNECT_RETRIES_INTERVAL".equals(key)) {
            return CONNECT_RETRIES_INTERVAL;
        }
        if ("MIGRATEDB_DEFAULT_SCHEMA".equals(key)) {
            return DEFAULT_SCHEMA;
        }
        if ("MIGRATEDB_DRIVER".equals(key)) {
            return DRIVER;
        }
        if ("MIGRATEDB_DRYRUN_OUTPUT".equals(key)) {
            return DRYRUN_OUTPUT;
        }
        if ("MIGRATEDB_ENCODING".equals(key)) {
            return ENCODING;
        }
        if ("MIGRATEDB_ERROR_OVERRIDES".equals(key)) {
            return ERROR_OVERRIDES;
        }
        if ("MIGRATEDB_GROUP".equals(key)) {
            return GROUP;
        }
        if ("MIGRATEDB_IGNORE_FUTURE_MIGRATIONS".equals(key)) {
            return IGNORE_FUTURE_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_MISSING_MIGRATIONS".equals(key)) {
            return IGNORE_MISSING_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_IGNORED_MIGRATIONS".equals(key)) {
            return IGNORE_IGNORED_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_PENDING_MIGRATIONS".equals(key)) {
            return IGNORE_PENDING_MIGRATIONS;
        }
        if ("MIGRATEDB_IGNORE_MIGRATION_PATTERNS".equals(key)) {
            return IGNORE_MIGRATION_PATTERNS;
        }
        if ("MIGRATEDB_INIT_SQL".equals(key)) {
            return INIT_SQL;
        }
        if ("MIGRATEDB_INSTALLED_BY".equals(key)) {
            return INSTALLED_BY;
        }
        if ("MIGRATEDB_LOCATIONS".equals(key)) {
            return LOCATIONS;
        }
        if ("MIGRATEDB_MIXED".equals(key)) {
            return MIXED;
        }
        if ("MIGRATEDB_OUT_OF_ORDER".equals(key)) {
            return OUT_OF_ORDER;
        }
        if ("MIGRATEDB_SKIP_EXECUTING_MIGRATIONS".equals(key)) {
            return SKIP_EXECUTING_MIGRATIONS;
        }
        if ("MIGRATEDB_OUTPUT_QUERY_RESULTS".equals(key)) {
            return OUTPUT_QUERY_RESULTS;
        }
        if ("MIGRATEDB_PASSWORD".equals(key)) {
            return PASSWORD;
        }
        if ("MIGRATEDB_LOCK_RETRY_COUNT".equals(key)) {
            return LOCK_RETRY_COUNT;
        }
        if ("MIGRATEDB_PLACEHOLDER_PREFIX".equals(key)) {
            return PLACEHOLDER_PREFIX;
        }
        if ("MIGRATEDB_PLACEHOLDER_REPLACEMENT".equals(key)) {
            return PLACEHOLDER_REPLACEMENT;
        }
        if ("MIGRATEDB_PLACEHOLDER_SUFFIX".equals(key)) {
            return PLACEHOLDER_SUFFIX;
        }
        if ("MIGRATEDB_SCRIPT_PLACEHOLDER_PREFIX".equals(key)) {
            return SCRIPT_PLACEHOLDER_PREFIX;
        }
        if ("MIGRATEDB_SCRIPT_PLACEHOLDER_SUFFIX".equals(key)) {
            return SCRIPT_PLACEHOLDER_SUFFIX;
        }
        if (key.matches("MIGRATEDB_PLACEHOLDERS_.+")) {
            return PLACEHOLDERS_PROPERTY_PREFIX +
                   key.substring("MIGRATEDB_PLACEHOLDERS_".length()).toLowerCase(Locale.ROOT);
        }

        if (key.matches("MIGRATEDB_JDBC_PROPERTIES_.+")) {
            return JDBC_PROPERTIES_PREFIX + key.substring("MIGRATEDB_JDBC_PROPERTIES_".length());
        }

        if ("MIGRATEDB_REPEATABLE_SQL_MIGRATION_PREFIX".equals(key)) {
            return REPEATABLE_SQL_MIGRATION_PREFIX;
        }
        if ("MIGRATEDB_RESOLVERS".equals(key)) {
            return RESOLVERS;
        }
        if ("MIGRATEDB_SCHEMAS".equals(key)) {
            return SCHEMAS;
        }
        if ("MIGRATEDB_SKIP_DEFAULT_CALLBACKS".equals(key)) {
            return SKIP_DEFAULT_CALLBACKS;
        }
        if ("MIGRATEDB_SKIP_DEFAULT_RESOLVERS".equals(key)) {
            return SKIP_DEFAULT_RESOLVERS;
        }
        if ("MIGRATEDB_SQL_MIGRATION_PREFIX".equals(key)) {
            return SQL_MIGRATION_PREFIX;
        }
        if ("MIGRATEDB_SQL_MIGRATION_SEPARATOR".equals(key)) {
            return SQL_MIGRATION_SEPARATOR;
        }
        if ("MIGRATEDB_SQL_MIGRATION_SUFFIXES".equals(key)) {
            return SQL_MIGRATION_SUFFIXES;
        }
        if ("MIGRATEDB_BASELINE_MIGRATION_PREFIX".equals(key)) {
            return BASELINE_MIGRATION_PREFIX;
        }
        if ("MIGRATEDB_TABLE".equals(key)) {
            return TABLE;
        }
        if ("MIGRATEDB_TABLESPACE".equals(key)) {
            return TABLESPACE;
        }
        if ("MIGRATEDB_TARGET".equals(key)) {
            return TARGET;
        }
        if ("MIGRATEDB_CHERRY_PICK".equals(key)) {
            return CHERRY_PICK;
        }
        if ("MIGRATEDB_LOGGERS".equals(key)) {
            return LOGGER;
        }
        if ("MIGRATEDB_URL".equals(key)) {
            return URL;
        }
        if ("MIGRATEDB_USER".equals(key)) {
            return USER;
        }
        if ("MIGRATEDB_VALIDATE_ON_MIGRATE".equals(key)) {
            return VALIDATE_ON_MIGRATE;
        }
        if ("MIGRATEDB_VALIDATE_MIGRATION_NAMING".equals(key)) {
            return VALIDATE_MIGRATION_NAMING;
        }
        if ("MIGRATEDB_CREATE_SCHEMAS".equals(key)) {
            return CREATE_SCHEMAS;
        }
        if ("MIGRATEDB_FAIL_ON_MISSING_LOCATIONS".equals(key)) {
            return FAIL_ON_MISSING_LOCATIONS;
        }

        // Oracle-specific
        if ("MIGRATEDB_ORACLE_SQLPLUS".equals(key)) {
            return ORACLE_SQLPLUS;
        }
        if ("MIGRATEDB_ORACLE_SQLPLUS_WARN".equals(key)) {
            return ORACLE_SQLPLUS_WARN;
        }
        if ("MIGRATEDB_ORACLE_KERBEROS_CONFIG_FILE".equals(key)) {
            return ORACLE_KERBEROS_CONFIG_FILE;
        }
        if ("MIGRATEDB_ORACLE_KERBEROS_CACHE_FILE".equals(key)) {
            return ORACLE_KERBEROS_CACHE_FILE;
        }
        if ("MIGRATEDB_ORACLE_WALLET_LOCATION".equals(key)) {
            return ORACLE_WALLET_LOCATION;
        }

        // Command-line specific
        if ("MIGRATEDB_JAR_DIRS".equals(key)) {
            return JAR_DIRS;
        }
        return null;
    }
}
