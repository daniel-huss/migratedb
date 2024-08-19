/*
 * Copyright 2022-2024 The MigrateDB contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
