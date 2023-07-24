package migratedb.v1.commandline;

import migratedb.v1.core.api.configuration.Configuration;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.Driver;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements driver-based database connection support in the command line application.
 */
public interface DriverSupport {
    /**
     * This is useful for databases that allow setting this in order to easily correlate individual application with
     * database connections.
     */
    String APPLICATION_NAME = "MigrateDB";

    // Don't grab semicolons and ampersands - they have special meaning in URLs
    Pattern defaultJdbcCredentialsPattern = Pattern.compile("password=([^;&]*).*", Pattern.CASE_INSENSITIVE);

    /**
     * @return The human-readable name.
     */
    String getName();

    /**
     * Check if this database type should handle the given JDBC url
     *
     * @param url The JDBC url.
     * @return {@code true} if this handles the JDBC url, {@code false} if not.
     */
    boolean handlesJdbcUrl(String url);

    /**
     * Detects whether a user is required from configuration. This may not be the case if the driver supports other
     * authentication mechanisms, or supports the user being encoded in the URL
     *
     * @param url The url to check
     * @return true if a username needs to be provided
     */
    default boolean detectUserRequiredByUrl(String url) {
        return true;
    }

    /**
     * Detects whether a password is required from configuration. This may not be the case if the driver supports other
     * authentication mechanisms, or supports the password being encoded in the URL
     *
     * @param url The url to check
     * @return true if a password needs to be provided
     */
    default boolean detectPasswordRequiredByUrl(String url) {
        return true;
    }

    /**
     * Detects whether external authentication is required.
     *
     * @return true if external authentication is required, else false.
     */
    default boolean externalAuthPropertiesRequired(String url, @Nullable String username, @Nullable String password) {
        return false;
    }

    /**
     * @param url      The JDBC url.
     * @param username The username for the connection.
     * @return Authentication properties from database specific locations (e.g. pgpass)
     */
    default Map<String, String> getExternalAuthProperties( @Nullable String url,  @Nullable String username) {
        return Map.of();
    }

    /**
     * A regex that identifies credentials in the JDBC URL, where they conform to a pattern specific to this database.
     * The first captured group must represent the password text, so that it can be redacted if necessary.
     *
     * @return The URL regex.
     */
    default Pattern getJdbcCredentialsPattern() {
        return defaultJdbcCredentialsPattern;
    }

    /**
     * (Probably) replaces secrets in {@code url} with {@code "***"}.
     */
    default String redactJdbcUrl(String url) {
        Matcher matcher = getJdbcCredentialsPattern().matcher(url);
        if (matcher.find()) {
            String password = matcher.group(1);
            return url.replace(password, "***");
        }
        return url;
    }

    /**
     * Get the driver class used to handle this JDBC url. This will only be called if {@code matchesJDBCUrl} previously
     * returned {@code true}.
     *
     * @param url         The JDBC url.
     * @param classLoader The classLoader to check for driver classes.
     * @return The full driver class name to be instantiated to handle this url.
     */
    String getDriverClass(String url, ClassLoader classLoader);

    /**
     * Retrieves a second choice backup driver for a JDBC url, in case the one returned by {@code getDriverClass} is not
     * available.
     *
     * @param url         The JDBC url.
     * @param classLoader The classLoader to check for driver classes.
     * @return The JDBC driver. {@code null} if none.
     */
    default @Nullable String getBackupDriverClass(String url, ClassLoader classLoader) {
        return null;
    }

    /**
     * @return A hint on the requirements for creating database instances (libs on class path, etc.)
     */
    default String instantiateClassExtendedErrorMessage() {
        return "";
    }

    /**
     * Set the default connection properties. These can be overridden by {@code
     * setConfigConnectionProps} and {@code setOverridingConnectionProps}.
     *
     * @param url         The JDBC url.
     * @param props       The properties to write to.
     * @param classLoader The classLoader to use.
     */
    default void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
    }

    /**
     * Set any necessary connection properties based on MigrateDB's configuration. These can be overridden by {@code
     * setOverridingConnectionProps}.
     *
     * @param config      The MigrateDB configuration to read properties from.
     * @param props       The properties to write to.
     * @param classLoader The classLoader to use.
     */
    default void modifyConfigConnectionProps(Configuration config, Properties props, ClassLoader classLoader) {
    }

    /**
     * These will override anything set by {@code setDefaultConnectionProps} and {@code setConfigConnectionProps} and
     * should only be used if neither of those can satisfy your requirement.
     *
     * @param props The properties to write to.
     */
    default void modifyOverridingConnectionProps(Map<String, String> props) {
    }

    /**
     * Only applicable to embedded databases that require this.
     */
    default void shutdownDatabase(String url, Driver driver) {
    }
}
