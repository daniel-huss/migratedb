package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

@AutoService(DriverSupport.class)
public class YugabyteDBDriverSupport extends PostgreSQLDriverSupport{
    @Override
    public String getName() {
        return "YugabyteDB";
    }
}
