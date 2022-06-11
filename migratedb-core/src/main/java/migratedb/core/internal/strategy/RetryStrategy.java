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
package migratedb.core.internal.strategy;

import migratedb.core.api.MigrateDbException;
import migratedb.core.api.internal.util.SqlCallable;

import java.sql.SQLException;

/**
 * A class that retries a Callable a given number of times until success is obtained.
 */
public class RetryStrategy {
    private boolean unlimitedRetries;

    private int numberOfRetriesRemaining;

    /**
     * A class that retries a Callable a given number of times until success is obtained.
     */
    public RetryStrategy(int numberOfRetries) {
        numberOfRetriesRemaining = numberOfRetries;
    }

    private boolean hasMoreRetries() {
        return (unlimitedRetries || numberOfRetriesRemaining > 0);
    }

    private void nextRetry() {
        if (!unlimitedRetries) {
            numberOfRetriesRemaining--;
        }
    }

    private int nextWaitInMilliseconds() {
        return 1000;
    }

    /**
     * Keep retrying a Callable with a potentially varying wait on each iteration, until one of the following happens: -
     * the callable returns {@code true}; - an InterruptedException happens - the number of retries is exceeded.
     *
     * @param callable               The callable to retry
     * @param interruptionMessage    The message to relay if interruption happens
     * @param retriesExceededMessage The message to relay if the number of retries is exceeded
     *
     * @throws SQLException
     */
    public void doWithRetries(SqlCallable<Boolean> callable, String interruptionMessage, String retriesExceededMessage)
    throws SQLException {
        while (!callable.call()) {
            try {
                Thread.sleep(nextWaitInMilliseconds());
            } catch (InterruptedException e) {
                throw new MigrateDbException(interruptionMessage, e);
            }

            if (!hasMoreRetries()) {
                throw new MigrateDbException(retriesExceededMessage);
            }
            nextRetry();
        }
    }
}
