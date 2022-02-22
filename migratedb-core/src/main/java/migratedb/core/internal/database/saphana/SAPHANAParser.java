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
package migratedb.core.internal.database.saphana;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import migratedb.core.api.configuration.Configuration;
import migratedb.core.internal.parser.Parser;
import migratedb.core.internal.parser.ParserContext;
import migratedb.core.internal.parser.ParsingContext;
import migratedb.core.internal.parser.PeekingReader;
import migratedb.core.internal.parser.StatementType;
import migratedb.core.internal.parser.Token;
import migratedb.core.internal.parser.TokenType;

public class SAPHANAParser extends Parser {
    private static final StatementType FUNCTION_OR_PROCEDURE_STATEMENT = new StatementType();
    private static final Pattern FUNCTION_OR_PROCEDURE_REGEX = Pattern.compile(
        "^CREATE(\\sOR\\sREPLACE)?\\s(FUNCTION|PROCEDURE)");

    private static final StatementType ANONYMOUS_BLOCK_STATEMENT = new StatementType();
    private static final Pattern ANONYMOUS_BLOCK_REGEX = Pattern.compile(
        "^DO.*BEGIN");

    public SAPHANAParser(Configuration configuration, ParsingContext parsingContext) {
        super(configuration, parsingContext, 2);
    }

    @Override
    protected StatementType detectStatementType(String simplifiedStatement, ParserContext context) {
        if (FUNCTION_OR_PROCEDURE_REGEX.matcher(simplifiedStatement).matches()) {
            return FUNCTION_OR_PROCEDURE_STATEMENT;
        }
        if (ANONYMOUS_BLOCK_REGEX.matcher(simplifiedStatement).matches()) {
            return ANONYMOUS_BLOCK_STATEMENT;
        }

        return super.detectStatementType(simplifiedStatement, context);
    }

    @Override
    protected boolean shouldAdjustBlockDepth(ParserContext context, List<Token> tokens, Token token) {
        TokenType tokenType = token.getType();
        if ((context.getStatementType() == FUNCTION_OR_PROCEDURE_STATEMENT ||
             context.getStatementType() == ANONYMOUS_BLOCK_STATEMENT) &&
            (TokenType.EOF == tokenType || TokenType.DELIMITER == tokenType)) {
            return true;
        }

        return super.shouldAdjustBlockDepth(context, tokens, token);
    }

    @Override
    protected void adjustBlockDepth(ParserContext context, List<Token> tokens, Token keyword, PeekingReader reader)
    throws IOException {
        int parensDepth = keyword.getParensDepth();

        // BEGIN, CASE, DO and IF increases block depth
        if ("BEGIN".equals(keyword.getText()) || "CASE".equals(keyword.getText()) || "DO".equals(keyword.getText()) ||
            "IF".equals(keyword.getText())
            // But not END IF
            && !lastTokenIs(tokens, parensDepth, "END")) {
            context.increaseBlockDepth(keyword.getText());
        } else if (doTokensMatchPattern(tokens, keyword, FUNCTION_OR_PROCEDURE_REGEX)) {
            context.increaseBlockDepth("FUNCTION_OR_PROCEDURE_REGEX");
        } else if ("END".equals(keyword.getText())) {
            context.decreaseBlockDepth();
        }

        TokenType tokenType = keyword.getType();
        if ((context.getStatementType() == FUNCTION_OR_PROCEDURE_STATEMENT ||
             context.getStatementType() == ANONYMOUS_BLOCK_STATEMENT) &&
            (TokenType.EOF == tokenType || TokenType.DELIMITER == tokenType) &&
            context.getBlockDepth() == 1 &&
            lastTokenIs(tokens, parensDepth, "END")) {
            context.decreaseBlockDepth();
            return;
        }
    }
}