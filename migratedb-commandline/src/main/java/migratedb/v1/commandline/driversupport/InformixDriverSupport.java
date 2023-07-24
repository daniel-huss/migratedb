package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

@AutoService(DriverSupport.class)
public class InformixDriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "Informix";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:informix-sqli:") || url.startsWith("jdbc:p6spy:informix-sqli:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:informix-sqli:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "com.informix.jdbc.IfxDriver";
    }
}
