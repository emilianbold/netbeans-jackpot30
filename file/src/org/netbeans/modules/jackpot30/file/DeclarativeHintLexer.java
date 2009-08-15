/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.file;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

/**
 *
 * @author Jan Lahoda
 */
class DeclarativeHintLexer implements Lexer<DeclarativeHintTokenId> {

    private final LexerInput input;
    private final TokenFactory<DeclarativeHintTokenId> fact;

    public DeclarativeHintLexer(LexerRestartInfo<DeclarativeHintTokenId> info) {
        input = info.input();
        fact  = info.tokenFactory();
    }

    public Token<DeclarativeHintTokenId> nextToken() {
        int read = input.read();

        if (read == LexerInput.EOF) {
            return null;
        }

        int whitespaceLength = 0;
        
        if (Character.isWhitespace(read)) {
            while ((read != LexerInput.EOF) && Character.isWhitespace((char) read)) {
                read = input.read();
            }

            if (read == LexerInput.EOF) {
                return fact.createToken(DeclarativeHintTokenId.WHITESPACE);
            }

            whitespaceLength = input.readLength() - 1;
        }

        while (read != LexerInput.EOF) {
            Matcher dnMatcher = DISPLAY_NAME_RE.matcher(input.readText());

            if (dnMatcher.find()) {
                int start = dnMatcher.start();

                if (start == 0) {
                    return fact.createToken(DeclarativeHintTokenId.DISPLAY_NAME);
                }

                if (whitespaceLength == start) {
                    input.backup(input.readLength() - whitespaceLength);
                    return fact.createToken(DeclarativeHintTokenId.WHITESPACE);
                }

                input.backup(input.readLength() - start);

                return fact.createToken(DeclarativeHintTokenId.JAVA_SNIPPET);
            }

            Matcher variableMatcher = VARIABLE_RE.matcher(input.readText());

            if (variableMatcher.find()) {
                int start = variableMatcher.start();

                if (start == 0) {
                    Matcher m;

                    while ((read = input.read()) != LexerInput.EOF && (m = VARIABLE_RE.matcher(input.readText())).find()) {
                        if (m.end() < input.readLength())
                            break;
                    }

                    if (read != LexerInput.EOF) {
                        input.backup(1);
                    }
                    
                    return fact.createToken(DeclarativeHintTokenId.VARIABLE);
                }

                if (whitespaceLength == start) {
                    input.backup(input.readLength() - whitespaceLength);
                    return fact.createToken(DeclarativeHintTokenId.WHITESPACE);
                }

                input.backup(input.readLength() - start);

                return fact.createToken(DeclarativeHintTokenId.JAVA_SNIPPET);
            }

            if (input.readLength() > 1) {
                String inputString = input.readText().toString();
                
                Token t = testToken(inputString.substring(input.readLength() - 2), whitespaceLength);

                if (t != null) {
                    return t;
                }

                if (input.readLength() >= "instanceof".length()) {
                    t = testToken(inputString.substring(input.readLength() - "instanceof".length()), whitespaceLength);

                    if (t != null) {
                        return t;
                    }
                }

                for (Entry<String, DeclarativeHintTokenId> e : BLOCK_TOKEN_START.entrySet()) {
                    if (!inputString.endsWith(e.getKey()))
                        continue;
                    
                    Token<DeclarativeHintTokenId> preread = resolvePrereadText(e.getKey().length(), whitespaceLength);

                    if (preread != null) {
                        return preread;
                    }

                    return readBlockToken(e.getValue(), BLOCK_TOKEN_END.get(e.getValue()));
                }
            }

            Token<DeclarativeHintTokenId> t = testToken(String.valueOf((char) read), whitespaceLength);

            if (t != null) {
                return t;
            }

            read = input.read();
        }

        return fact.createToken(DeclarativeHintTokenId.JAVA_SNIPPET);
    }

    public Object state() {
        return null;
    }

    public void release() {}

    private Token<DeclarativeHintTokenId> testToken(String toTest, int whitespaceLength) {
        if (TOKENS.containsKey(toTest)) {
            Token<DeclarativeHintTokenId> t = resolvePrereadText(toTest.length(), whitespaceLength);

            if (t != null) {
                return t;
            } else {
                return fact.createToken(TOKENS.get(toTest));
            }
        }

        return null;
    }

    private Token<DeclarativeHintTokenId> resolvePrereadText(int backupLength, int whitespaceLength) {
        if (whitespaceLength > 0) {
            if (input.readLength() == whitespaceLength + backupLength) {
                input.backup(input.readLength() - whitespaceLength);

                return fact.createToken(DeclarativeHintTokenId.WHITESPACE);
            } else {
                input.backup(backupLength);

                return fact.createToken(DeclarativeHintTokenId.JAVA_SNIPPET);
            }
        } else {
            if (input.readLength() == backupLength) {
                return null;
            } else {
                input.backup(backupLength);

                return fact.createToken(DeclarativeHintTokenId.JAVA_SNIPPET);
            }
        }
    }

    private Token<DeclarativeHintTokenId> readBlockToken(DeclarativeHintTokenId tokenId, String tokenEnd) {
        while (input.read() != LexerInput.EOF && !input.readText().toString().endsWith(tokenEnd))
            ;

        return fact.createToken(tokenId);
    }

    private static final Pattern DISPLAY_NAME_RE = Pattern.compile("'[^']*':");
    private static final Pattern VARIABLE_RE = Pattern.compile("\\$[A-Za-z0-9_$]+");

    private static final Map<String, DeclarativeHintTokenId> TOKENS;
    private static final Map<String, DeclarativeHintTokenId> BLOCK_TOKEN_START;
    private static final Map<DeclarativeHintTokenId, String> BLOCK_TOKEN_END;

    static {
        Map<String, DeclarativeHintTokenId> map = new HashMap<String, DeclarativeHintTokenId>();

        map.put("=>", DeclarativeHintTokenId.LEADS_TO);
        map.put("::", DeclarativeHintTokenId.DOUBLE_COLON);
        map.put("&&", DeclarativeHintTokenId.AND);
        map.put("!", DeclarativeHintTokenId.NOT);
        map.put(";;", DeclarativeHintTokenId.DOUBLE_SEMICOLON);
        map.put("%%", DeclarativeHintTokenId.DOUBLE_PERCENT);
        map.put("instanceof", DeclarativeHintTokenId.INSTANCEOF);

        TOKENS = Collections.unmodifiableMap(map);

        Map<String, DeclarativeHintTokenId> blockStartMap = new HashMap<String, DeclarativeHintTokenId>();

        blockStartMap.put("/*", DeclarativeHintTokenId.BLOCK_COMMENT);
        blockStartMap.put("//", DeclarativeHintTokenId.LINE_COMMENT);
        blockStartMap.put("<?", DeclarativeHintTokenId.JAVA_BLOCK);
        blockStartMap.put("<!", DeclarativeHintTokenId.OPTIONS);

        BLOCK_TOKEN_START = Collections.unmodifiableMap(blockStartMap);

        Map<DeclarativeHintTokenId, String> blockEndMap = new HashMap<DeclarativeHintTokenId, String>();

        blockEndMap.put(DeclarativeHintTokenId.BLOCK_COMMENT, "*/");
        blockEndMap.put(DeclarativeHintTokenId.LINE_COMMENT, "\n");
        blockEndMap.put(DeclarativeHintTokenId.JAVA_BLOCK, "?>");
        blockEndMap.put(DeclarativeHintTokenId.OPTIONS, ">");

        BLOCK_TOKEN_END = Collections.unmodifiableMap(blockEndMap);
    }
}
