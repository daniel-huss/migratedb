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

package migratedb.v1.core.internal.database.oracle;

import migratedb.v1.core.api.ConfigPropertiesConverter;
import migratedb.v1.core.api.ConvertedProperties;
import migratedb.v1.core.api.ExtensionConfig;

import java.util.Map;

import static migratedb.v1.core.internal.configuration.ConfigUtils.removeBoolean;

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

            Boolean sqlplusProp = removeBoolean(mutableProps, ORACLE_SQLPLUS);
            if (sqlplusProp != null) {
                config.setSqlplus(sqlplusProp);
            }
            Boolean sqlplusWarnProp = removeBoolean(mutableProps, ORACLE_SQLPLUS_WARN);
            if (sqlplusWarnProp != null) {
                config.setSqlplusWarn(sqlplusWarnProp);
            }
            String kerberosConfigFile = mutableProps.remove(ORACLE_KERBEROS_CONFIG_FILE);
            if (kerberosConfigFile != null) {
                config.setKerberosConfigFile(kerberosConfigFile);
            }
            String kerberosCacheFile = mutableProps.remove(ORACLE_KERBEROS_CACHE_FILE);
            if (kerberosCacheFile != null) {
                config.setKerberosCacheFile(kerberosCacheFile);
            }
            String walletLocationProp = mutableProps.remove(ORACLE_WALLET_LOCATION);
            if (walletLocationProp != null) {
                config.setWalletLocation(walletLocationProp);
            }

            return new ConvertedProperties<>(OracleConfig.class, config);
        }
    }

    private boolean sqlplus;
    private boolean sqlplusWarn;
    private String kerberosConfigFile;
    private String kerberosCacheFile;
    private String walletLocation;

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * Whether MigrateDB's support for Oracle SQL*Plus commands should be activated.
     *
     * @return {@code true} to active SQL*Plus support. {@code false} to fail fast instead. (default: {@code false})
     */
    public boolean isSqlplus() {
        return sqlplus;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * Whether MigrateDB should issue a warning instead of an error whenever it encounters an Oracle SQL*Plus statement
     * it doesn't yet support.
     *
     * @return {@code true} to issue a warning. {@code false} to fail fast instead. (default: {@code false})
     */
    public boolean isSqlplusWarn() {
        return sqlplusWarn;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos configuration.
     */
    public String getKerberosConfigFile() {
        return kerberosConfigFile;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos cache.
     */
    public String getKerberosCacheFile() {
        return kerberosCacheFile;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * The location of your Oracle wallet, used to automatically sign in to your databases.
     */
    public String getWalletLocation() {
        return walletLocation;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * Whether MigrateDB's support for Oracle SQL*Plus commands should be activated.
     *
     * @param Sqlplus {@code true} to active SQL*Plus support. {@code false} to fail fast instead. (default:
     *                {@code false})
     */
    public void setSqlplus(boolean Sqlplus) {
        this.sqlplus = Sqlplus;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * Whether MigrateDB's support for Oracle SQL*Plus commands should be activated.
     *
     * @param Sqlplus {@code true} to active SQL*Plus support. {@code false} to fail fast instead. (default:
     *                {@code false})
     */
    public OracleConfig sqlplus(boolean Sqlplus) {
        setSqlplus(Sqlplus);
        return this;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * Whether MigrateDB should issue a warning instead of an error whenever it encounters an Oracle SQL*Plus
     * statement it doesn't yet support.
     *
     * @param SqlplusWarn {@code true} to issue a warning. {@code false} to fail fast instead. (default: {@code
     *                    false})
     */
    public void setSqlplusWarn(boolean SqlplusWarn) {
        this.sqlplusWarn = SqlplusWarn;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * Whether MigrateDB should issue a warning instead of an error whenever it encounters an Oracle SQL*Plus
     * statement it doesn't yet support.
     *
     * @param SqlplusWarn {@code true} to issue a warning. {@code false} to fail fast instead. (default: {@code
     *                    false})
     */
    public OracleConfig sqlplusWarn(boolean SqlplusWarn) {
        setSqlplusWarn(SqlplusWarn);
        return this;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos configuration.
     */
    public void setKerberosConfigFile(String KerberosConfigFile) {
        this.kerberosConfigFile = KerberosConfigFile;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos configuration.
     */
    public OracleConfig kerberosConfigFile(String KerberosConfigFile) {
        setKerberosConfigFile(KerberosConfigFile);
        return this;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos cache.
     */
    public void setKerberosCacheFile(String KerberosCacheFile) {
        this.kerberosCacheFile = KerberosCacheFile;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * When Oracle needs to connect to a Kerberos service to authenticate, the location of the Kerberos cache.
     */
    public OracleConfig kerberosCacheFile(String KerberosCacheFile) {
        setKerberosCacheFile(KerberosCacheFile);
        return this;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * The location of your Oracle wallet, used to automatically sign in to your databases.
     *
     * @param WalletLocation The path to your Oracle Wallet
     */
    public void setWalletLocation(String WalletLocation) {
        this.walletLocation = WalletLocation;
    }

    /**
     * <b>Warning: Feature not implemented!</b>
     * <p>
     * The location of your Oracle wallet, used to automatically sign in to your databases.
     *
     * @param WalletLocation The path to your Oracle Wallet
     */
    public OracleConfig walletLocation(String WalletLocation) {
        setWalletLocation(WalletLocation);
        return this;
    }
}
