package migratedb.v1.commandline;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.logging.Log;
import migratedb.v1.core.internal.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DriverSupportRegistry {
    private static final Log LOG = Log.getLog(DriverSupportRegistry.class);

    private final List<DriverSupport> driverSupportList = new ArrayList<>();

    public void register(DriverSupport driverSupport) {
        driverSupportList.add(driverSupport);
    }

    public DriverSupport getDriverSupportForUrl(String url) {
        List<DriverSupport> driversAcceptingUrl = getAllDriverSupportForUrl(url);
        if (!driversAcceptingUrl.isEmpty()) {
            if (driversAcceptingUrl.size() > 1) {
                StringBuilder builder = new StringBuilder();
                for (DriverSupport type : driversAcceptingUrl) {
                    if (builder.length() > 0) {
                        builder.append(", ");
                    }
                    builder.append(type.getName());
                }
                LOG.debug("Multiple databases found that handle url '" + redactJdbcUrl(url) + "': " + builder);
            }
            return driversAcceptingUrl.get(0);
        } else {
            throw new MigrateDbException("No database found to handle " + redactJdbcUrl(url));
        }
    }

    private List<DriverSupport> getAllDriverSupportForUrl(String url) {
        List<DriverSupport> support = new ArrayList<>();
        for (var driverSupport : driverSupportList) {
            if (driverSupport.handlesJdbcUrl(url)) {
                support.add(driverSupport);
            }
        }
        return support;
    }

    public String redactJdbcUrl(String url) {
        List<DriverSupport> allDriverSupport = getAllDriverSupportForUrl(url);
        if (allDriverSupport.isEmpty()) {
            url = redactJdbcUrl(url, DriverSupport.defaultJdbcCredentialsPattern);
        } else {
            for (DriverSupport driverSupport : allDriverSupport) {
                Pattern dbPattern = driverSupport.getJdbcCredentialsPattern();
                url = redactJdbcUrl(url, dbPattern);
            }
        }
        return url;
    }

    private String redactJdbcUrl(String url, Pattern pattern) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            String password = matcher.group(1);
            return url.replace(password, StringUtils.trimOrPad("", password.length(), '*'));
        }
        return url;
    }
}
