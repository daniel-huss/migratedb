package migratedb.v1.spring.boot.v3.autoconfig;

import org.springframework.beans.factory.BeanCreationException;

final class MissingApplicationDataSourceException extends BeanCreationException {
    MissingApplicationDataSourceException() {
        super("Requested different credentials for migration data source," +
                " but no (unique/primary) application data source to derive from is available.");
    }
}
