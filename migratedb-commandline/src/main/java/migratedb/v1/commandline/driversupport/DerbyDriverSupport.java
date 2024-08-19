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
import migratedb.v1.core.api.logging.Log;

import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

@AutoService(DriverSupport.class)
public class DerbyDriverSupport implements DriverSupport {
    private static final Log LOG = Log.getLog(DerbyDriverSupport.class);

    @Override
    public String getName() {
        return "Derby";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:derby:") || url.startsWith("jdbc:p6spy:derby:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:derby:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        if (url.startsWith("jdbc:derby://")) {
            return "org.apache.derby.jdbc.ClientDriver";
        }
        return "org.apache.derby.jdbc.EmbeddedDriver";
    }


    @Override
    public void shutdownDatabase(String url, Driver driver) {
        // only do this on the embedded version
        if (!url.startsWith("jdbc:derby://")) {
            try {
                int i = url.indexOf(";");
                String shutdownUrl = (i < 0 ? url : url.substring(0, i)) + ";shutdown=true";
                driver.connect(shutdownUrl, new Properties()).close();
            } catch (SQLException e) {
                LOG.debug("Unexpected error on Derby Embedded Database shutdown: " + e.getMessage());
            }
        }
    }
}
