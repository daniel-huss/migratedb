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
package migratedb.v1.core.internal.database.bigquery;

import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.internal.database.base.BaseSession;
import migratedb.v1.core.internal.util.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BigQuerySession extends BaseSession {
    /*
     *   BigQuery has no concept of a default dataset, but the JDBC driver does (albeit not advertised through the
     *   normal metadata means) - so we can parse it out of the URL
     */
    private static final Pattern DEFAULT_DATASET_PATTERN = Pattern.compile("DefaultDataset=([a-zA-Z\\d]*);");

    BigQuerySession(BigQueryDatabase database, Connection connection) {
        super(database, connection);
        this.jdbcTemplate = new BigQueryJdbcTemplate(connection, database.getDatabaseType());
    }

    @Override
    protected String getCurrentSchemaNameOrSearchPath() throws SQLException {
        // BigQuery has no concept of current schema, return DefaultDataset if it is set in JDBC, otherwise null.
        String defaultDataset = getJdbcClientOption("DefaultDataset");
        return StringUtils.hasText(defaultDataset) ? defaultDataset.trim() : null;
    }

    @Override
    public void changeCurrentSchemaTo(Schema schema) {
        // BigQuery has no concept of current schema, do nothing.
    }

    @Override
    public void doChangeCurrentSchemaOrSearchPathTo(String schema) {
        // BigQuery has no concept of current schema, do nothing.
    }

    @Override
    public Schema doGetCurrentSchema() throws SQLException {
        // BigQuery has no concept of current schema, return DefaultDataset if it is set in JDBC, otherwise null.
        // We would expect to be able to call this: getJdbcClientOption("DefaultDataset"); but we always get
        // null for any ClientInfo() with driver google-cloud-bigquery-1.126.6.jar
        String defaultDataset = parseDefaultDatasetFromUrl();
        return StringUtils.hasText(defaultDataset) ? getSchema(defaultDataset.trim()) : null;
    }

    private String parseDefaultDatasetFromUrl() throws SQLException {
        String url = getJdbcConnection().getMetaData().getURL();
        Matcher matcher = DEFAULT_DATASET_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String getJdbcClientOption(String option) throws SQLException {
        return getJdbcConnection().getClientInfo(option);
    }

    @Override
    public BigQuerySchema getSchema(String name) {
        return new BigQuerySchema(jdbcTemplate, getDatabase(), name);
    }

    @Override
    public BigQueryDatabase getDatabase() {
        return (BigQueryDatabase) super.getDatabase();
    }
}
