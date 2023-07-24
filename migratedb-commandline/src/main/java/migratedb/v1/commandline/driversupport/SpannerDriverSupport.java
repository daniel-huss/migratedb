package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

@AutoService(DriverSupport.class)
public class SpannerDriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "Google Cloud Spanner";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return (url.startsWith("jdbc:cloudspanner:") || url.startsWith("jdbc:p6spy:cloudspanner:"));
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:cloudspanner:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "com.google.cloud.spanner.jdbc.JdbcDriver";
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !url.contains("credentials=");
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return !url.contains("credentials=");
    }
}
