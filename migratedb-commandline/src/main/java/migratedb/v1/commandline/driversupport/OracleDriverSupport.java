package migratedb.v1.commandline.driversupport;

import com.google.auto.service.AutoService;
import migratedb.v1.commandline.DriverSupport;
import migratedb.v1.core.api.MigrateDbException;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.regex.Pattern;

@AutoService(DriverSupport.class)
public final class OracleDriverSupport implements DriverSupport {
    // Oracle usernames/passwords can be 1-30 chars, can only contain alphanumerics and # _ $
    // The first (and only) capture group represents the password
    private static final Pattern usernamePasswordPattern = Pattern.compile(
            "^jdbc:oracle:thin:[a-zA-Z\\d#_$]+/([a-zA-Z\\d#_$]+)@.*");

    /**
     * Gets the String value of a static field.
     *
     * @param className   The fully qualified name of the class to instantiate.
     * @param classLoader The ClassLoader to use.
     * @param fieldName   The field name
     * @return The value of the field.
     * @throws MigrateDbException If the field value cannot be read.
     */
    public static String getStaticFieldValue(String className, String fieldName, ClassLoader classLoader) {
        try {
            return getStaticFieldValue(Class.forName(className, true, classLoader), fieldName);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new MigrateDbException(
                    "Unable to obtain field value " + className + "." + fieldName + " : " + e.getMessage(), e);
        }
    }

    /**
     * Gets the String value of a static field.
     *
     * @throws MigrateDbException If the field value cannot be read.
     */
    public static String getStaticFieldValue(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getField(fieldName);
            return (String) field.get(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new MigrateDbException(
                    "Unable to obtain field value " + clazz.getName() + "." + fieldName + " : " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return "Oracle";
    }

    @Override
    public boolean detectUserRequiredByUrl(String url) {
        return !usernamePasswordPattern.matcher(url).matches();
    }

    @Override
    public boolean detectPasswordRequiredByUrl(String url) {
        return !usernamePasswordPattern.matcher(url).matches();
    }

    @Override
    public void modifyDefaultConnectionProps(String url, Properties props, ClassLoader classLoader) {
        String osUser = System.getProperty("user.name");
        props.put("v$session.osuser", osUser.substring(0, Math.min(osUser.length(), 30)));
        props.put("v$session.program", APPLICATION_NAME);
        props.put("oracle.net.keepAlive", "true");

        String oobb = getStaticFieldValue("oracle.jdbc.OracleConnection",
                "CONNECTION_PROPERTY_THIN_NET_DISABLE_OUT_OF_BAND_BREAK",
                classLoader);
        props.put(oobb, "true");
    }


    @Override
    public boolean handlesJdbcUrl(String url) {
        return url.startsWith("jdbc:oracle") || url.startsWith("jdbc:p6spy:oracle");
    }

    @Override
    public Pattern getJdbcCredentialsPattern() {
        return usernamePasswordPattern;
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url.startsWith("jdbc:p6spy:oracle:")) {
            return "com.p6spy.engine.spy.P6SpyDriver";
        }
        return "oracle.jdbc.OracleDriver";
    }
}
