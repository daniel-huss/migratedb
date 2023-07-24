package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

import java.util.Properties;

@AutoService(DriverSupport.class)
public class SybaseASEJConnectDriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "Sybase ASE";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:sybase:") || url.startsWith("jdbc:p6spy:sybase:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:sybase:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "com.sybase.jdbc4.jdbc.SybDriver";
    }

    @Override
    public void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("APPLICATIONNAME", APPLICATION_NAME);
    }
}
