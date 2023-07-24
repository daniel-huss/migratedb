package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

import java.util.Properties;

@AutoService(DriverSupport.class)
public class FirebirdDriverSupport implements DriverSupport {
    @Override
    public String getName() {
        return "Firebird";
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:firebird:") || url.startsWith("jdbc:firebirdsql:") ||
                url.startsWith("jdbc:p6spy:firebird:") || url.startsWith("jdbc:p6spy:firebirdsql:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:firebird:") || url.startsWith("jdbc:p6spy:firebirdsql:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "org.firebirdsql.jdbc.FBDriver";
    }


    @Override
    public void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        props.put("processName", APPLICATION_NAME);
    }
}
