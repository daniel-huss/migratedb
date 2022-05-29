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

package migratedb.core.internal.database.oracle;

import migratedb.core.api.ConfigPropertiesConverter;
import migratedb.core.api.ConvertedProperties;
import migratedb.core.api.ExtensionConfig;

import java.util.Map;

import static migratedb.core.internal.configuration.ConfigUtils.removeBoolean;

public final class OracleConfig implements ExtensionConfig {

    public static final String ORACLE_SQLPLUS = "migratedb.oracle.sqlplus";
    public static final String ORACLE_SQLPLUS_WARN = "migratedb.oracle.sqlplusWarn";
    public static final String ORACLE_KERBEROS_CONFIG_FILE = "migratedb.oracle.kerberosConfigFile";
    public static final String ORACLE_KERBEROS_CACHE_FILE = "migratedb.oracle.kerberosCacheFile";
    public static final String ORACLE_WALLET_LOCATION = "migratedb.oracle.walletLocation";

    public static final class PropertiesConverter implements ConfigPropertiesConverter {
        @Override
        public ConvertedProperties<OracleConfig> convert(Map<String, String> mutableProps) {
            var config = new OracleConfig();

            Boolean oracleSqlplusProp = removeBoolean(mutableProps, ORACLE_SQLPLUS);
            if (oracleSqlplusProp != null) {
                config.setOracleSqlplus(oracleSqlplusProp);
            }
            Boolean oracleSqlplusWarnProp = removeBoolean(mutableProps, ORACLE_SQLPLUS_WARN);
            if (oracleSqlplusWarnProp != null) {
                config.setOracleSqlplusWarn(oracleSqlplusWarnProp);
            }
            String oracleKerberosConfigFile = mutableProps.remove(ORACLE_KERBEROS_CONFIG_FILE);
            if (oracleKerberosConfigFile != null) {
                config.setOracleKerberosConfigFile(oracleKerberosConfigFile);
            }
            String oracleKerberosCacheFile = mutableProps.remove(ORACLE_KERBEROS_CACHE_FILE);
            if (oracleKerberosCacheFile != null) {
                config.setOracleKerberosCacheFile(oracleKerberosCacheFile);
            }
            String oracleWalletLocationProp = mutableProps.remove(ORACLE_WALLET_LOCATION);
            if (oracleWalletLocationProp != null) {
                config.setOracleWalletLocation(oracleWalletLocationProp);
            }

            return new ConvertedProperties<>(OracleConfig.class, config);
        }
    }

    private boolean oracleSqlplus;
    private boolean oracleSqlplusWarn;
    private String oracleKerberosConfigFile;
    private String oracleKerberosCacheFile;
    private String oracleWalletLocation;

    /**
     * Whether MigrateDB's support for Oracle SQL*Plus commands should be activated.
     *
     * @return {@code true} to active SQL*Plus support. {@code false} to fail fast instead. (default: {@code false})
     */
    public boolean isOracleSqlplus() {
        return oracleSqlplus;
    }

    /**
     * Whether MigrateDB should issue a warning instead of an error whenever it encounters an Oracle SQL*Plus statement
     * it doesn't yet support.
     *
     * @return {@code true} to issue a warning. {@code false} to fail fast instead. (default: {@code false})
     */
    public boolean isOracleSqlplusWarn() {
        return oracleSqlplusWarn;
    }

    /**
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos configuration.
     */
    public String getOracleKerberosConfigFile() {
        return oracleKerberosConfigFile;
    }

    /**
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos cache.
     */
    public String getOracleKerberosCacheFile() {
        return oracleKerberosCacheFile;
    }

    /**
     * Whether MigrateDB's support for Oracle SQL*Plus commands should be activated.
     *
     * @param oracleSqlplus {@code true} to active SQL*Plus support. {@code false} to fail fast instead. (default:
     *                      {@code false})
     */
    public void setOracleSqlplus(boolean oracleSqlplus) {
        this.oracleSqlplus = oracleSqlplus;
    }

    /**
     * Whether MigrateDB should issue a warning instead of an error whenever it encounters an Oracle SQL*Plus
     * statementit doesn't yet support.
     *
     * @param oracleSqlplusWarn {@code true} to issue a warning. {@code false} to fail fast instead. (default: {@code
     *                          false})
     */
    public void setOracleSqlplusWarn(boolean oracleSqlplusWarn) {
        this.oracleSqlplusWarn = oracleSqlplusWarn;
    }

    /**
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos configuration.
     */
    public void setOracleKerberosConfigFile(String oracleKerberosConfigFile) {
        this.oracleKerberosConfigFile = oracleKerberosConfigFile;
    }

    /**
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos cache.
     */
    public void setOracleKerberosCacheFile(String oracleKerberosCacheFile) {
        this.oracleKerberosCacheFile = oracleKerberosCacheFile;
    }

    /**
     * The location of your Oracle wallet, used to automatically sign in to your databases.
     *
     * @param oracleWalletLocation The pasth to your Oracle Wallet
     */
    public void setOracleWalletLocation(String oracleWalletLocation) {
        this.oracleWalletLocation = oracleWalletLocation;
    }
}
