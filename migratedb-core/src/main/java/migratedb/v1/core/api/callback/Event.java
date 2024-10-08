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
package migratedb.v1.core.api.callback;

public enum Event {
    /**
     * Fired before migrate is executed. This event will be fired in a separate transaction from the actual migrate
     * operation.
     */
    BEFORE_MIGRATE("beforeMigrate"),
    /**
     * Fired before each individual migration is executed. This event will be fired within the same transaction (if any)
     * as the migration and can be used for things like setting up connection parameters that are required by
     * migrations.
     */
    BEFORE_EACH_MIGRATE("beforeEachMigrate"),
    /**
     * Fired before each individual statement in a migration is executed. This event will be fired within the same
     * transaction (if any) as the migration and can be used for things like asserting a statement complies with policy
     * (for example: no grant statements allowed).
     */
    BEFORE_EACH_MIGRATE_STATEMENT("beforeEachMigrateStatement"),
    /**
     * Fired after each individual statement in a migration that succeeded. This event will be fired within the same
     * transaction (if any) as the migration.
     */
    AFTER_EACH_MIGRATE_STATEMENT("afterEachMigrateStatement"),
    /**
     * Fired after each individual statement in a migration that failed. This event will be fired within the same
     * transaction (if any) as the migration.
     */
    AFTER_EACH_MIGRATE_STATEMENT_ERROR("afterEachMigrateStatementError"),
    /**
     * Fired after each individual migration that succeeded. This event will be fired within the same transaction (if
     * any) as the migration.
     */
    AFTER_EACH_MIGRATE("afterEachMigrate"),
    /**
     * Fired after each individual migration that failed. This event will be fired within the same transaction (if any)
     * as the migration.
     */
    AFTER_EACH_MIGRATE_ERROR("afterEachMigrateError"),
    /**
     * Fired before any repeatable migrations are applied. This event will be fired in a separate transaction from the
     * actual migrate operation.
     */
    BEFORE_REPEATABLES("beforeRepeatables"),
    /**
     * Fired after all versioned migrations are applied. This event will be fired in a separate transaction from the
     * actual migrate operation.
     */
    AFTER_VERSIONED("afterVersioned"),
    /**
     * Fired after migrate has succeeded, and at least one migration has been applied. This event will be fired in a
     * separate transaction from the actual migrate operation.
     */
    AFTER_MIGRATE_APPLIED("afterMigrateApplied"),
    /**
     * Fired after migrate has succeeded. This event will be fired in a separate transaction from the actual migrate
     * operation.
     */
    AFTER_MIGRATE("afterMigrate"),
    /**
     * Fired after migrate has failed. This event will be fired in a separate transaction from the actual migrate
     * operation.
     */
    AFTER_MIGRATE_ERROR("afterMigrateError"),
    /**
     * Fired before validate is executed. This event will be fired in a separate transaction from the actual validate
     * operation.
     */
    BEFORE_VALIDATE("beforeValidate"),
    /**
     * Fired after validate has succeeded. This event will be fired in a separate transaction from the actual validate
     * operation.
     */
    AFTER_VALIDATE("afterValidate"),
    /**
     * Fired after validate has failed. This event will be fired in a separate transaction from the actual validate
     * operation.
     */
    AFTER_VALIDATE_ERROR("afterValidateError"),
    /**
     * Fired before baseline is executed. This event will be fired in a separate transaction from the actual baseline
     * operation.
     */
    BEFORE_BASELINE("beforeBaseline"),
    /**
     * Fired after baseline has succeeded. This event will be fired in a separate transaction from the actual baseline
     * operation.
     */
    AFTER_BASELINE("afterBaseline"),
    /**
     * Fired after baseline has failed. This event will be fired in a separate transaction from the actual baseline
     * operation.
     */
    AFTER_BASELINE_ERROR("afterBaselineError"),
    /**
     * Fired before repair is executed. This event will be fired in a separate transaction from the actual repair
     * operation.
     */
    BEFORE_REPAIR("beforeRepair"),
    /**
     * Fired after repair has succeeded. This event will be fired in a separate transaction from the actual repair
     * operation.
     */
    AFTER_REPAIR("afterRepair"),
    /**
     * Fired after repair has failed. This event will be fired in a separate transaction from the actual repair
     * operation.
     */
    AFTER_REPAIR_ERROR("afterRepairError"),
    /**
     * Fired before info is executed. This event will be fired in a separate transaction from the actual info
     * operation.
     */
    BEFORE_INFO("beforeInfo"),
    /**
     * Fired after info has succeeded. This event will be fired in a separate transaction from the actual info
     * operation.
     */
    AFTER_INFO("afterInfo"),
    /**
     * Fired after info has failed. This event will be fired in a separate transaction from the actual info operation.
     */
    AFTER_INFO_ERROR("afterInfoError"),
    /**
     * Fired before liberate is executed. This event will be fired in a separate transaction from the actual liberate
     * operation.
     */
    BEFORE_LIBERATE("beforeLiberate"),
    /**
     * Fired after liberate has succeeded. This event will be fired in a separate transaction from the actual liberate
     * operation.
     */
    AFTER_LIBERATE("afterLiberate"),
    /**
     * Fired after liberate has failed. This event will be fired in a separate transaction from the actual liberate
     * operation.
     */
    AFTER_LIBERATE_ERROR("afterLiberateError"),
    /**
     * Fired after a migrate operation has finished.
     */
    AFTER_MIGRATE_OPERATION_FINISH("afterMigrateOperationFinish"),
    /**
     * Fired after an info operation has finished.
     */
    AFTER_INFO_OPERATION_FINISH("afterInfoOperationFinish"),
    /**
     * Fired after a validate operation has finished.
     */
    AFTER_VALIDATE_OPERATION_FINISH("afterInfoOperationFinish"),
    /**
     * Fired after a validate operation has finished.
     */
    AFTER_REPAIR_OPERATION_FINISH("afterInfoOperationFinish"),
    /**
     * Fired after a validate operation has finished.
     */
    AFTER_BASELINE_OPERATION_FINISH("afterInfoOperationFinish"),
    /**
     * Fired after a liberate operation has finished.
     */
    AFTER_LIBERATE_OPERATION_FINISH("afterLiberateOperationFinish"),
    /**
     * Fired before any non-existent schemas are created.
     */
    CREATE_SCHEMA("createSchema");

    /**
     * The MigrateDB lifecycle events that can be handled in callbacks.
     */
    private final String id;

    Event(String id) {
        this.id = id;
    }

    /**
     * @return The id of an event. Examples: {@code beforeClean}, {@code afterEachMigrate}, ...
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the event with this id.
     *
     * @param id The id.
     * @return The event. {@code null} if not found.
     */
    public static Event fromId(String id) {
        for (Event event : values()) {
            if (event.id.equals(id)) {
                return event;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return id;
    }
}
