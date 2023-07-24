package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

@AutoService(DriverSupport.class)
public class HSQLDBDriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "HSQLDB";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:hsqldb:") || url.startsWith("jdbc:p6spy:hsqldb:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:hsqldb:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "org.hsqldb.jdbcDriver";
    }
}
