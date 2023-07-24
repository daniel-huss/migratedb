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
