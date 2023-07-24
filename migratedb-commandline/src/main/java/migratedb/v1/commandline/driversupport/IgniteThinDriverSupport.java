package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

@AutoService(DriverSupport.class)
public class IgniteThinDriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "Apache Ignite";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:ignite:thin:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        return "org.apache.ignite.IgniteJdbcThinDriver";
    }
}
