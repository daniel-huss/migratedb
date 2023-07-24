package migratedb.v1.core.api;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A function that provides freshly initialized JDBC connections, possibly from a connection pool.
 */
@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection() throws SQLException;
}
