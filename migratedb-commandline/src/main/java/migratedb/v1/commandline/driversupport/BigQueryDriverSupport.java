package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;
import migratedb.v1.core.internal.util.ClassUtils;

import java.util.regex.Pattern;

@AutoService(DriverSupport.class)
public class BigQueryDriverSupport implements DriverSupport {
    private static final String BIGQUERY_JDBC42_DRIVER = "com.simba.googlebigquery.jdbc42.Driver";
    private static final String BIGQUERY_JDBC_DRIVER = "com.simba.googlebigquery.jdbc.Driver";
    private static final Pattern OAUTH_CREDENTIALS_PATTERN = Pattern.compile("OAuth\\w+=([^;]*)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String getName() {
        return "BigQuery";
    }


    @Override
    public Pattern getJdbcCredentialsPattern() {
        return OAUTH_CREDENTIALS_PATTERN;
    }

    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:bigquery:");
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        return BIGQUERY_JDBC42_DRIVER;
    }

    @Override
    public String getBackupDriverClass(String url, ClassLoader classLoader) {
        if (ClassUtils.isPresent(BIGQUERY_JDBC_DRIVER, classLoader)) {
            return BIGQUERY_JDBC_DRIVER;
        }
        return null;
    }

    @Override
    public String instantiateClassExtendedErrorMessage() {
        return "Failure probably due to inability to load dependencies. Please ensure you have downloaded " +
                "'https://cloud.google.com/bigquery/docs/reference/odbc-jdbc-drivers' and extracted to " +
                "'migratedb/drivers' folder";
    }
}
