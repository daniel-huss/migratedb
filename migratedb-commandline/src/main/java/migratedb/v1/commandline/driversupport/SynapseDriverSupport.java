package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;

@AutoService(DriverSupport.class)
public class SynapseDriverSupport extends SQLServerDriverSupport {
    @Override
    public String getName() {
        return "Azure Synapse";
    }

    @Override
    protected boolean supportsJTDS() {
        return false;
    }
}
