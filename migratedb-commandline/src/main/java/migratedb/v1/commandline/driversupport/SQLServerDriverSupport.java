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

import java.util.Properties;

@AutoService(DriverSupport.class)
public class SQLServerDriverSupport implements DriverSupport {
    private static final Log LOG = Log.getLog(SQLServerDriverSupport.class) ;

    @Override
    public String getName() {
        return "SQL Server";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:sqlserver:") || (supportsJTDS() && url.startsWith("jdbc:jtds:")) ||
                url.startsWith("jdbc:p6spy:sqlserver:") || (supportsJTDS() && url.startsWith("jdbc:p6spy:jtds:"));
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:sqlserver:") || (supportsJTDS() && url.startsWith("jdbc:p6spy:jtds:"))) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }

        if (url.startsWith("jdbc:jtds:")) {
            if (supportsJTDS()) {
                return "net.sourceforge.jtds.jdbc.Driver";
            } else if (!supportsJTDS()) {
                LOG.warn("JTDS does not support this database. Using the Microsoft JDBC driver instead");
            }
        }

        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    @Override
    public void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("applicationName", APPLICATION_NAME);
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !(url.contains("integratedSecurity=")
                || url.contains("authentication=ActiveDirectoryIntegrated")
                || url.contains("authentication=ActiveDirectoryMSI"));
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {

        return !(url.contains("integratedSecurity=")
                || url.contains("authentication=ActiveDirectoryIntegrated")
                || url.contains("authentication=ActiveDirectoryMSI"));
    }

    protected boolean supportsJTDS() {
        return true;
    }
}
