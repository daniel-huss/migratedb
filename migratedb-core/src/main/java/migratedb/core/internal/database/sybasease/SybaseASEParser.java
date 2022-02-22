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
package migratedb.core.internal.database.sybasease;

import java.io.IOException;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.internal.parser.Parser;
import migratedb.core.internal.parser.ParserContext;
import migratedb.core.internal.parser.ParsingContext;
import migratedb.core.internal.parser.PeekingReader;
import migratedb.core.internal.sqlscript.Delimiter;

public class SybaseASEParser extends Parser {
    public SybaseASEParser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 3);
    }

    @Override
    protected Delimiter getDefaultDelimiter() {
        return Delimiter.GO;
    }

    @Override
    protected boolean isDelimiter(String peek, ParserContext context, int col, int colIgnoringWhitepace) {
        return peek.length() >= 2
               && (peek.charAt(0) == 'G' || peek.charAt(0) == 'g')
               && (peek.charAt(1) == 'O' || peek.charAt(1) == 'o')
               && (peek.length() == 2 || Character.isWhitespace(peek.charAt(2)));
    }

    @Override
    protected String readKeyword(PeekingReader reader, Delimiter delimiter, ParserContext context) throws IOException {
        // #2414: Ignore delimiter as GO (unlike ;) can be part of a regular keyword
        return "" + (char) reader.read() + reader.readKeywordPart(null, context);
    }
}