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

import java.io.PrintStream;
import migratedb.core.api.logging.LogAdapter;
import migratedb.core.api.logging.LogSystem;

public class ConsoleLogSystem implements LogSystem {
    private final Arguments commandLineArguments;
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final LogLevel level;
    private final ConsoleLogAdapter adapterInstance;

    public ConsoleLogSystem(Arguments commandLineArguments, PrintStream stdout, PrintStream stderr) {
        this.commandLineArguments = commandLineArguments;
        this.stdout = stdout;
        this.stderr = stderr;
        this.level = commandLineArguments.getLogLevel();
        this.adapterInstance = new ConsoleLogAdapter();
    }

    @Override
    public LogAdapter createLogAdapter(String logName) {
        return adapterInstance;
    }

    private final class ConsoleLogAdapter implements LogAdapter {
        @Override
        public boolean isDebugEnabled() {
            return level == LogLevel.DEBUG;
        }

        public void debug(String message) {
            if (isDebugEnabled()) {
                stdout.println("DEBUG: " + message);
            }
        }

        public void info(String message) {
            if (level.compareTo(LogLevel.INFO) <= 0) {
                stdout.println(message);
            }
        }

        public void warn(String message) {
            stdout.println("WARNING: " + message);
        }

        public void error(String message) {
            stderr.println("ERROR: " + message);
        }

        public void error(String message, Exception e) {
            stderr.println("ERROR: " + message);
            e.printStackTrace(stderr);
        }
    }
}
