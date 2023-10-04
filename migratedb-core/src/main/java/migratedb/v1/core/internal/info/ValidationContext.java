/*
 * Copyright 2022-2023 The MigrateDB contributors
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

package migratedb.v1.core.internal.info;

import migratedb.v1.core.api.configuration.Configuration;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;

public final class ValidationContext {
    public static ValidationContext allAllowed() {
        return new ValidationContext(ValidationMatch.values());
    }

    private final EnumSet<ValidationMatch> allowed;

    public ValidationContext(Configuration configuration) {
        this(getAllowedValidationMatches(configuration));
    }

    private static EnumSet<ValidationMatch> getAllowedValidationMatches(Configuration configuration) {
        var result = EnumSet.noneOf(ValidationMatch.class);
        Map.of(
               ValidationMatch.OUT_OF_ORDER, configuration.isOutOfOrder(),
               ValidationMatch.MISSING, configuration.isIgnoreMissingMigrations(),
               ValidationMatch.PENDING, configuration.isIgnorePendingMigrations(),
               ValidationMatch.FUTURE, configuration.isIgnoreFutureMigrations(),
               ValidationMatch.IGNORED, configuration.isIgnoreFutureMigrations()
           ).entrySet()
           .stream()
           .filter(Map.Entry::getValue)
           .map(Map.Entry::getKey)
           .sequential()
           .forEach(result::add);
        return result;
    }

    public ValidationContext(ValidationMatch... allowed) {
        this.allowed = EnumSet.noneOf(ValidationMatch.class);
        this.allowed.addAll(Arrays.asList(allowed));
    }

    public ValidationContext(EnumSet<ValidationMatch> allowed) {
        this.allowed = EnumSet.copyOf(allowed);
    }

    private ValidationContext(EnumSet<ValidationMatch> allowed, boolean doNotCopy) {
        assert doNotCopy;
        this.allowed = allowed;
    }

    boolean allows(ValidationMatch f) {
        return allowed.contains(f);
    }

    public ValidationContext with(ValidationMatch match, boolean allow) {
        var newAllowed = EnumSet.copyOf(allowed);
        if (allow) {
            newAllowed.add(match);
        } else {
            newAllowed.remove(match);
        }
        return new ValidationContext(newAllowed, true);
    }
}
