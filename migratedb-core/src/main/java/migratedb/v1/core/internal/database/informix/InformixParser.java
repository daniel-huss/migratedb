/*
 * Copyright (C) Red Gate Software Ltd 2010-2021
 * Copyright 2022-2023 The MigrateDB contributors
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
package migratedb.v1.core.internal.database.informix;

import migratedb.v1.core.api.configuration.Configuration;
import migratedb.v1.core.api.internal.parser.ParsingContext;
import migratedb.v1.core.internal.parser.BaseParser;
import migratedb.v1.core.internal.parser.ParserContext;
import migratedb.v1.core.internal.parser.PeekingReader;
import migratedb.v1.core.internal.parser.Token;

import java.io.IOException;
import java.util.List;

public class InformixParser extends BaseParser {
    public InformixParser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 2);
    }

    @Override
    protected void adjustBlockDepth(ParserContext context, List<Token> tokens, Token keyword, PeekingReader reader)
    throws IOException {
        int lastKeywordIndex = getLastKeywordIndex(tokens);
        if (lastKeywordIndex < 0) {
            return;
        }

        String current = keyword.getText();
        if ("FUNCTION".equals(current) || "PROCEDURE".equals(current)) {
            String previous = tokens.get(lastKeywordIndex).getText();

            // CREATE( DBA)? (FUNCTION|PROCEDURE)
            if ("CREATE".equals(previous) || "DBA".equals(previous)) {
                context.increaseBlockDepth(previous);
            } else if ("END".equals(previous)) {
                context.decreaseBlockDepth();
            }
        }
    }
}
