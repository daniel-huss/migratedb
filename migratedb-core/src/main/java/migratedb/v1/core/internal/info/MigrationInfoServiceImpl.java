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
package migratedb.v1.core.internal.info;

import migratedb.v1.core.api.*;
import migratedb.v1.core.api.MigrationState.Category;
import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.database.base.Database;
import migratedb.v1.core.api.internal.database.base.Schema;
import migratedb.v1.core.api.output.CommandResultFactory;
import migratedb.v1.core.api.output.InfoResult;
import migratedb.v1.core.api.output.OperationResult;
import migratedb.v1.core.api.output.ValidateOutput;
import migratedb.v1.core.api.resolver.Context;
import migratedb.v1.core.api.resolver.MigrationResolver;
import migratedb.v1.core.internal.schemahistory.SchemaHistory;

import java.util.*;
import java.util.function.Predicate;

public class MigrationInfoServiceImpl extends OperationResult implements MigrationInfoService {
    private final MigrationResolver migrationResolver;
    private final Configuration configuration;
    private final Database database;
    private final Context context;
    private final SchemaHistory schemaHistory;
    private final TargetVersion target;
    private final List<MigrationPattern> cherryPick;
    private final ValidationContext validationContext;
    /**
     * The migrations info calculated at the last refresh.
     */
    private List<MigrationInfo> migrationInfo;
    /**
     * Whether all the specified schemas are empty or not.
     */
    private boolean allSchemasEmpty;

    /**
     * @param migrationResolver The migration resolver for available migrations.
     * @param schemaHistory     The schema history table for applied migrations.
     * @param configuration     The current configuration.
     * @param target            The target version up to which to retrieve the info.
     * @param cherryPick        The migrations to consider when migration.
     */
    public MigrationInfoServiceImpl(MigrationResolver migrationResolver,
                                    SchemaHistory schemaHistory,
                                    Database database,
                                    Configuration configuration,
                                    TargetVersion target,
                                    List<MigrationPattern> cherryPick,
                                    ValidationContext validationContext) {
        this.migrationResolver = migrationResolver;
        this.schemaHistory = schemaHistory;
        this.configuration = configuration;
        this.context = () -> configuration;
        this.database = database;
        this.target = target;
        this.cherryPick = cherryPick;
        this.validationContext = validationContext;
    }

    /**
     * Refreshes the info about migration state using the resolved migrations and schema history information.
     */
    public void refresh() {
        var newMigrationInfo = new RefreshHelper(
            migrationResolver.resolveMigrations(context),
            schemaHistory.allAppliedMigrations(),
            cherryPick,
            target,
            validationContext
        ).getMigrationInfo();
        failOnMissingTarget(newMigrationInfo);
        migrationInfo = newMigrationInfo;
    }

    private void failOnMissingTarget(List<MigrationInfo> migrations) {
        if (configuration.isFailOnMissingTarget() && target != null) {
            target.withVersion(version -> {
                boolean targetFound = migrations.stream()
                                                .anyMatch(it -> version.equals(it.getVersion()));
                if (!targetFound) {
                    throw new MigrateDbException("No migration with a target version " + target +
                                                 " could be found. Ensure target is specified correctly and the " +
                                                 "migration" +
                                                 " exists.");
                }
            }).orElseDoNothing(); // Symbolic targets are never missing (I guess?!)
        }
    }

    @Override
    public MigrationInfo[] all() {
        return migrationInfo.toArray(MigrationInfo[]::new);
    }

    @Override
    public MigrationInfo current() {
        return migrationInfo.stream()
                            .filter(it -> it.getState().is(Category.APPLIED) &&
                                          it.getState() != MigrationState.DELETED &&
                                          it.getVersion() != null)
                            .max(Comparator.comparing(MigrationInfo::getVersion))
                            .or(this::latestAppliedRepeatableMigration)
                            .orElse(null);
    }

    private Optional<? extends MigrationInfo> latestAppliedRepeatableMigration() {
        for (int i = migrationInfo.size() - 1; i >= 0; i--) {
            var migrationInfo = this.migrationInfo.get(i);
            if (migrationInfo.getState().is(Category.APPLIED)
                && !MigrationState.DELETED.equals(migrationInfo.getState())
            ) {
                return Optional.of(migrationInfo);
            }
        }
        return Optional.empty();
    }

    @Override
    public MigrationInfo next() {
        var pending = pending();
        if (pending.length > 0) {
            return pending[0];
        }
        var outdated = outdated();
        if (outdated.length > 0) {
            return outdated[0];
        }
        return null;
    }

    @Override
    public MigrationInfo[] pending() {
        return filterByState(it -> it == MigrationState.PENDING);
    }

    @Override
    public MigrationInfo[] applied() {
        return filterByState(it -> it.is(Category.APPLIED));
    }

    @Override
    public MigrationInfo[] resolved() {
        return filterByState(it -> it.is(Category.RESOLVED));
    }

    @Override
    public MigrationInfo[] failed() {
        return filterByState(it -> it.is(Category.FAILED));
    }

    @Override
    public MigrationInfo[] future() {
        return filterByState(it -> it.is(Category.FUTURE));
    }

    @Override
    public MigrationInfo[] outOfOrder() {
        return filterByState(MigrationState.OUT_OF_ORDER::equals);
    }

    @Override
    public MigrationInfo[] outdated() {
        return filterByState(MigrationState.OUTDATED::equals);
    }

    private MigrationInfo[] filterByState(Predicate<MigrationState> predicate) {
        return migrationInfo.stream()
                            .filter(it -> predicate.test(it.getState()))
                            .toArray(MigrationInfo[]::new);
    }

    /**
     * @return The list of migrations that failed validation, which is empty if everything is fine.
     */
    public List<ValidateOutput> validate() {
        List<ValidateOutput> invalidMigrations = new ArrayList<>();

        for (MigrationInfo migrationInfo : migrationInfo) {
            ErrorDetails validateError = migrationInfo.validate();
            if (validateError != null) {
                invalidMigrations.add(CommandResultFactory.createValidateOutput(migrationInfo, validateError));
            }
        }
        return invalidMigrations;
    }

    public void setAllSchemasEmpty(Schema[] schemas) {
        allSchemasEmpty = Arrays.stream(schemas).filter(Schema::exists).allMatch(Schema::isEmpty);
    }

    @Override
    public InfoResult getInfoResult() {
        return getInfoResult(all());
    }

    public InfoResult getInfoResult(MigrationInfo[] infos) {
        return CommandResultFactory.createInfoResult(context.getConfiguration(),
                                                     database,
                                                     infos,
                                                     current(),
                                                     allSchemasEmpty);
    }

}
