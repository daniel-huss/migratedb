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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import migratedb.core.api.MigrationPattern;
import migratedb.core.api.Version;
import migratedb.core.api.pattern.ValidatePattern;

final class VersionContext {
    static final class Builder {
        private final List<ValidatePattern> ignorePatterns;
        private final List<MigrationPattern> cherryPick;
        public final Map<String, Integer> latestRepeatableRuns = new HashMap<>();
        public Version target;
        public Version schema;
        public Version baseline;
        public Version lastResolved;
        public Version lastApplied;
        public Version latestBaselineMigration;

        Builder(ValidatePattern[] ignorePatterns, MigrationPattern[] cherryPick) {
            this.ignorePatterns = Arrays.asList(ignorePatterns);
            this.cherryPick = Arrays.asList(cherryPick);
        }

        VersionContext build() {
            return new VersionContext(this);
        }
    }

    public final List<ValidatePattern> ignorePatterns;
    public final Version target;
    public final List<MigrationPattern> cherryPick;
    public final Version schema;
    public final Version baseline;
    public final Version lastResolved;
    public final Version lastApplied;
    public final Version latestBaselineMigration;
    public final Map<String, Integer> latestRepeatableRuns;

    private VersionContext(Builder b) {
        this.ignorePatterns = new ArrayList<>(b.ignorePatterns);
        this.cherryPick = new ArrayList<>(b.cherryPick);
        this.latestRepeatableRuns = new HashMap<>(b.latestRepeatableRuns);
        this.target = b.target;
        this.schema = b.schema;
        this.baseline = b.baseline;
        this.lastResolved = b.lastResolved;
        this.lastApplied = b.lastApplied;
        this.latestBaselineMigration = b.latestBaselineMigration;
    }
}

