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

import java.util.Locale;

@AutoService(DriverSupport.class)
public class H2DriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "H2";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:h2:") || url.startsWith("jdbc:p6spy:h2:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:h2:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "org.h2.Driver";
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !(url.toLowerCase(Locale.ROOT).contains(":mem:"));
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return !(url.toLowerCase(Locale.ROOT).contains(":mem:"));
    }
}
