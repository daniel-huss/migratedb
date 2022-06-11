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
package migratedb.core.internal.database.sqlite;

import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.internal.parser.BaseParser;
import migratedb.core.internal.parser.ParserContext;
import migratedb.core.internal.parser.PeekingReader;
import migratedb.core.internal.parser.Token;

import java.io.IOException;
import java.util.List;

public class SQLiteParser extends BaseParser {
    public SQLiteParser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 3);
    }

    @Override
    protected char getAlternativeIdentifierQuote() {
        return '`';
    }

    @Override
    protected Boolean detectCanExecuteInTransaction(String simplifiedStatement, List<Token> keywords) {
        if ("PRAGMA FOREIGN_KEYS".equals(simplifiedStatement)) {
            return false;
        }

        return null;
    }

    @Override
    protected void adjustBlockDepth(ParserContext context, List<Token> tokens, Token keyword, PeekingReader reader)
    throws IOException {
        String lastKeyword = keyword.getText();
        if ("BEGIN".equals(lastKeyword) || "CASE".equals(lastKeyword)) {
            context.increaseBlockDepth(lastKeyword);
        } else if ("END".equals(lastKeyword)) {
            context.decreaseBlockDepth();
        }
    }
}
