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

import java.util.Map;

@AutoService(DriverSupport.class)
public class RedshiftDriverSupport implements DriverSupport {
    private static final String REDSHIFT_JDBC4_DRIVER = "com.amazon.redshift.jdbc4.Driver";
    private static final String REDSHIFT_JDBC41_DRIVER = "com.amazon.redshift.jdbc41.Driver";

    @Override
    public String getName() {
        return "Redshift";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:redshift:") || url.startsWith("jdbc:p6spy:redshift:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:redshift:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "com.amazon.redshift.jdbc42.Driver";
    }

    @Override
    public String getBackupDriverClass(String url, ClassLoader classLoader) {
        if (ClassUtils.isPresent(REDSHIFT_JDBC41_DRIVER, classLoader)) {
            return REDSHIFT_JDBC41_DRIVER;
        }
        return REDSHIFT_JDBC4_DRIVER;
    }


    @Override
    public void modifyOverridingConnectionProps(Map<String, String> props) {
        // Necessary because the Amazon v2 driver does not appear to respect the way Properties.get() handles defaults.
        // If not forced to false, the driver allows result sets to be read on different threads and will throw if
        // connections are closed before all results are read.
        props.put("enableFetchRingBuffer", "false");
    }
}
