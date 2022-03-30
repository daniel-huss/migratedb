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
package migratedb.core.internal.database.cockroachdb;

import java.io.IOException;
import java.util.List;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.api.internal.parser.ParsingContext;
import migratedb.core.internal.parser.BaseParser;
import migratedb.core.internal.parser.ParserContext;
import migratedb.core.internal.parser.PeekingReader;
import migratedb.core.internal.parser.Token;
import migratedb.core.internal.parser.TokenType;

public class CockroachDBParser extends BaseParser {
    public CockroachDBParser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 3);
    }

    @Override
    protected char getAlternativeStringLiteralQuote() {
        return '$';
    }

    @SuppressWarnings("Duplicates")
    @Override
    protected Token handleAlternativeStringLiteral(PeekingReader reader, ParserContext context, int pos, int line,
                                                   int col) throws IOException {
        String dollarQuote = (char) reader.read() + reader.readUntilIncluding('$');
        reader.swallowUntilExcluding(dollarQuote);
        reader.swallow(dollarQuote.length());
        return new Token(TokenType.STRING, pos, line, col, null, null, context.getParensDepth());
    }

    @Override
    protected Boolean detectCanExecuteInTransaction(String simplifiedStatement, List<Token> keywords) {
        return false;
    }
}
