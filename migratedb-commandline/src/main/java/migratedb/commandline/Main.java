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
package migratedb.commandline;

import java.util.ArrayList;
import java.util.List;
import migratedb.core.api.logging.Log;
import migratedb.core.api.logging.LogSystem;
import migratedb.core.internal.logging.MultiLogSystem;
import migratedb.core.internal.logging.NoLogSystem;

public class Main {
    public static void main(String[] args) {
        Arguments arguments = new Arguments(args);
        var realStdOut = System.out;
        var realStdErr = System.err;
        if (arguments.shouldOutputJson()) {
            // Cannot allow anyone else (rogue loggers!) to write to stdout/stderr
            System.setOut(new DiscardingPrintStream());
            System.setErr(new DiscardingPrintStream());
        }
        Log.setDefaultLogSystem(getDefaultLogSystem(arguments));
        int exitCode;
        try {
            exitCode = new MigrateDbCommand(arguments,
                                            System.console(),
                                            realStdOut,
                                            realStdErr,
                                            System.in,
                                            System.getenv())
                .run();
        } catch (Exception e) {
            exitCode = ExitCode.UNHANDLED_EXCEPTION;
        }
        System.exit(exitCode);
    }

    private static LogSystem getDefaultLogSystem(Arguments arguments) {
        if (arguments.shouldOutputJson()) {
            return NoLogSystem.INSTANCE;
        }
        List<LogSystem> logSystems = new ArrayList<>();
        logSystems.add(new ConsoleLogSystem(arguments, System.out, System.err));
        if (arguments.isOutputFileSet()) {
            logSystems.add(new FileLogSystem(arguments));
        }
        return new MultiLogSystem(logSystems);
    }
}
