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
