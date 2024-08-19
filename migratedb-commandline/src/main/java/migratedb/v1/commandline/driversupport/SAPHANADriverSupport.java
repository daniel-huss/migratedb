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

import java.util.Properties;

@AutoService(DriverSupport.class)
public class SAPHANADriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "SAP HANA";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:sap:") || url.startsWith("jdbc:p6spy:sap:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:sap:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "com.sap.db.jdbc.Driver";
    }

    @Override
    public void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("SESSIONVARIABLE:APPLICATION", APPLICATION_NAME);
    }
}
