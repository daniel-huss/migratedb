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
package migratedb.v1.core.internal.database.h2;

import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.internal.parser.*;

import java.io.IOException;

public class H2Parser extends BaseParser {
    public H2Parser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 2);
    }

    @Override
    protected char getAlternativeIdentifierQuote() {
        // Necessary for MySQL compatibility mode. We don't know the mode at this point so be generous and
        // parse backticks even though they may not run on the database.
        return '`';
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
}
