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
package migratedb.v1.commandline;

import migratedb.v1.core.api.MigrateDbException;
import migratedb.v1.core.api.logging.LogSystem;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileLogSystem implements LogSystem {
    private final LogLevel level;
    private final Path path;

    public FileLogSystem(Arguments commandLineArguments) {
        String outputFilepath = "";

        if (commandLineArguments.isOutputFileSet()) {
            outputFilepath = commandLineArguments.getOutputFile();
        }

        this.level = commandLineArguments.getLogLevel();
        this.path = Paths.get(outputFilepath);

        prepareOutputFile(path);
    }

    @Override
    public boolean isDebugEnabled(String logName) {
        return level == LogLevel.DEBUG;
    }

    @Override
    public void debug(String logName, String message) {
        if (isDebugEnabled(logName)) {
            writeLogMessage("DEBUG", message);
        }
    }

    @Override
    public void info(String logName, String message) {
        if (level.compareTo(LogLevel.INFO) <= 0) {
            writeLogMessage(message);
        }
    }

    @Override
    public void warn(String logName, String message) {
        writeLogMessage("WARNING", message);
    }

    @Override
    public void error(String logName, String message) {
        writeLogMessage("ERROR", message);
    }

    @Override
    public void error(String logName, String message, Exception e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();

        writeLogMessage("ERROR", message);
        writeLogMessage(stackTrace);
    }

    private void writeLogMessage(String prefix, String message) {
        String logMessage = prefix + ": " + message;
        writeLogMessage(logMessage);
    }

    private void writeLogMessage(String logMessage) {
        try {
            Files.write(path,
                        (logMessage + "\n").getBytes(Charset.defaultCharset()),
                        StandardOpenOption.APPEND,
                        StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new MigrateDbException("Could not write to file at " + path + ".", exception);
        }
    }

    private static void prepareOutputFile(Path path) {
        try {
            Files.write(path,
                        "".getBytes(Charset.defaultCharset()),
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.CREATE);
        } catch (IOException exception) {
            throw new MigrateDbException("Could not initialize log file at " + path + ".", exception);
        }
    }
}
