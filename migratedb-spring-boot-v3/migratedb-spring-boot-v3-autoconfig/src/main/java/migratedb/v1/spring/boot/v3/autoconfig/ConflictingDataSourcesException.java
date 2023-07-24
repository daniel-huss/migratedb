package migratedb.v1.spring.boot.v3.autoconfig;

import org.springframework.beans.factory.BeanCreationException;

import java.util.List;

class ConflictingDataSourcesException extends BeanCreationException {
    ConflictingDataSourcesException(List<String> conflictingDataSourcesDescriptions) {
        super(createMessage(conflictingDataSourcesDescriptions));
    }

    private static String createMessage(List<String> conflictingDataSourcesDescriptions) {
        var message = new StringBuilder();
        message.append("Multiple migration-specific data sources have been defined, but only one can be used." +
                " Please remove all but one of the following definitions:");
        for (int i = 0; i < conflictingDataSourcesDescriptions.size(); i++) {
            message.append("\n").append(i + 1).append(". ").append(conflictingDataSourcesDescriptions.get(i));
        }
        return message.toString();
    }
}
