/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022 The MigrateDB contributors
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
package migratedb.core.internal.info;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import migratedb.core.api.MigrationPattern;
import migratedb.core.api.Version;
import migratedb.core.api.pattern.ValidatePattern;
import org.checkerframework.checker.nullness.qual.Nullable;

final class VersionContext {
    /**
     * VersionContext is immutable, this builder is mutable.
     */
    static final class Builder {
        private final List<ValidatePattern> ignorePatterns;
        private final List<MigrationPattern> cherryPick;
        public Map<String, Integer> latestRepeatableRuns = new HashMap<>();
        public Version target;
        public Version baseline;
        public Version lastResolved;
        public Version lastApplied;
        public Version latestBaselineMigration;

        Builder(@Nullable ValidatePattern[] ignorePatterns, @Nullable MigrationPattern[] cherryPick) {
            this.ignorePatterns = ignorePatterns == null ? List.of() : Arrays.asList(ignorePatterns);
            this.cherryPick = cherryPick == null ? List.of() : Arrays.asList(cherryPick);
        }

        VersionContext build() {
            return new VersionContext(this);
        }
    }

    public final List<ValidatePattern> ignorePatterns;
    public final Version target;
    public final List<MigrationPattern> cherryPick;
    public final Version baseline;
    public final Version lastResolved;
    public final Version lastApplied;
    public final Version latestBaselineMigration;
    public final Map<String, Integer> latestRepeatableRuns;

    private VersionContext(Builder b) {
        this.ignorePatterns = List.copyOf(b.ignorePatterns);
        this.cherryPick = List.copyOf(b.cherryPick);
        this.latestRepeatableRuns = Map.copyOf(b.latestRepeatableRuns);
        this.target = b.target;
        this.baseline = b.baseline;
        this.lastResolved = b.lastResolved;
        this.lastApplied = b.lastApplied;
        this.latestBaselineMigration = b.latestBaselineMigration;
    }
}

