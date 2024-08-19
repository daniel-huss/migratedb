/*
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

package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;
import migratedb.v1.core.internal.util.ClassUtils;

import java.util.Properties;

@AutoService(DriverSupport.class)
public class MySQLDriverSupport implements DriverSupport {
    private static final String MYSQL_LEGACY_JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String MARIADB_JDBC_DRIVER = "org.mariadb.jdbc.Driver";

    @Override
    public String getName() {
        return "MySQL";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:google:") ||
               url.startsWith("jdbc:p6spy:mysql:") || url.startsWith("jdbc:p6spy:google:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {

        if (url.startsWith("jdbc:p6spy:mysql:") || url.startsWith("jdbc:p6spy:google:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        if (url.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else {
            return "com.mysql.jdbc.GoogleDriver";
        }
    }

    @Override
    public String getBackupDriverClass(String url, ClassLoader classLoader) {
        if (ClassUtils.isPresent(MYSQL_LEGACY_JDBC_DRIVER, classLoader)) {
            return MYSQL_LEGACY_JDBC_DRIVER;
        }

        if (ClassUtils.isPresent(MARIADB_JDBC_DRIVER, classLoader)) {
            return MARIADB_JDBC_DRIVER;
        }

        return null;
    }

    @Override
    public void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("connectionAttributes", "program_name:" + APPLICATION_NAME);
    }
}
