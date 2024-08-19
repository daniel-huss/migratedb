/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
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
package migratedb.v1.core.internal.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;

public final class ExceptionUtils {
    /**
     * @return The root cause or the throwable itself if it doesn't have a cause.
     */
    public static @Nullable Throwable getRootCause(@Nullable Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        var seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(throwable);
        Throwable rootCause = throwable;
        while (true) {
            var cause = rootCause.getCause();
            if (cause == null || !seen.add(cause) || seen.size() >= 10_000) break;
            rootCause = cause;
        }

        return rootCause;
    }

    /**
     * Transforms the details of this SQLException into a nice readable message.
     */
    public static String toMessage(SQLException e) {
        SQLException cause = e;
        while (cause.getNextException() != null) {
            cause = cause.getNextException();
        }

        String message = "SQL State  : " + cause.getSQLState() + "\n"
                + "Error Code : " + cause.getErrorCode() + "\n";
        if (cause.getMessage() != null) {
            message += "Message    : " + cause.getMessage().trim() + "\n";
        }

        return message;

    }
}
