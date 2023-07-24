package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

@AutoService(DriverSupport.class)
public class TestContainersDriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "Test Containers";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:tc:") || url.startsWith("jdbc:p6spy:tc:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:tc:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "org.testcontainers.jdbc.ContainerDatabaseDriver";
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !url.contains("user=");
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return !url.contains("password=");
    }
}
