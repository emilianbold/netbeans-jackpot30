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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.lexer.PartType;
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

    private int state;
    private boolean wasSnippetPart;

    public DeclarativeHintLexer(LexerRestartInfo<DeclarativeHintTokenId> info) {
        input = info.input();
        fact  = info.tokenFactory();

        if (info.state() != null) {
            State s = (State) info.state();
            
            state = s.state;
            wasSnippetPart = s.wasSnippetPart;
        }
    }

    public Token<DeclarativeHintTokenId> nextToken() {
        if (input.read() == LexerInput.EOF) {
            return null;
        }

        input.backup(1);

        if (state == 0 || state == 2) {
            Token<DeclarativeHintTokenId> readWhiteSpace = readWhiteSpace();

            if (readWhiteSpace != null) {
                return readWhiteSpace;
            }
        }

        int read;
        Token<DeclarativeHintTokenId> s;

        switch (state) {
            case 0:
                s = readString();

                if (s != null) {
                    return s;
                }

                read = input.read();

                if (read == ':') {
                    state = 1;
                    return fact.createToken(DeclarativeHintTokenId.COLON);
                }
            case 1:
                s = readSnippet();

                if (wasSnippetPart) {
                    state = 4;
                } else {
                    state = 2;
                }
                return s;
            case 2:
                read = input.read();

                if (read == '=') {
                    if (input.read() == '>') {
                        return fact.createToken(DeclarativeHintTokenId.LEADS_TO);
                    }

                    input.backup(1);

                    return fact.createToken(DeclarativeHintTokenId.ERROR);
                }

                if (read == ':') {
                    state = 3;
                    return fact.createToken(DeclarativeHintTokenId.COLON);
                }

                if (read == ';') {
                    int next = input.read();

                    if (next == ';') {
                        state = 0;
                        return fact.createToken(DeclarativeHintTokenId.DOUBLE_SEMICOLON);
                    }

                    if (next != LexerInput.EOF) {
                        input.backup(1);
                    }

                    return fact.createToken(DeclarativeHintTokenId.ERROR);
                }

                if (read == '"') {
                    input.backup(1);

                    s = readString();

                    if (s != null) {
                        return s;
                    }
                }
            case 3:
                s = readSnippet();
                if (wasSnippetPart) {
                    state = 4;
                } else {
                    state = 2;
                }
                return s;
            case 4:
                state = 2;
                return readType();
        }

        throw new IllegalStateException("" + state);
    }

    public Object state() {
        return new State(state, wasSnippetPart);
    }

    public void release() {}

    private Token<DeclarativeHintTokenId> readWhiteSpace() {
        int read = input.read();
        boolean create = false;

        while ((read != LexerInput.EOF) && Character.isWhitespace((char) read)) {
            read = input.read();
            create = true;
        }

        if (read != LexerInput.EOF) {
            input.backup(1);
        }

        if (read != '"' && read != ':' && read != LexerInput.EOF) {
            input.backup(input.readLength());
            return null;
        }

        if (create) {
            return fact.createToken(DeclarativeHintTokenId.WHITESPACE);
        }

        return null;
    }

    private Token<DeclarativeHintTokenId> readString() {
        int read = input.read();

        if (read != '"') {
            input.backup(1);

            return null;
        }

        read = input.read();
        
        while ((read != LexerInput.EOF) && read != '"') {
            read = input.read();
        }

        return fact.createToken(DeclarativeHintTokenId.DISPLAY_NAME);
    }

    private Token<DeclarativeHintTokenId> readSnippet() {
        boolean wasSnippetPartCopy = this.wasSnippetPart;
        
        while (input.read() != LexerInput.EOF) {
            if (input.readText().toString().endsWith("=>") || input.readText().toString().endsWith(";;")) {
                input.backup(2);

//                while (Character.isWhitespace(input.readText().charAt(input.readText().length() - 1))) {
//                    input.backup(1);
//                }

                this.wasSnippetPart = false;

                return fact.createToken(DeclarativeHintTokenId.PATTERN, input.readLength(), wasSnippetPartCopy ? PartType.END : PartType.COMPLETE);
            }

            Matcher m = VARIABLE_WITH_TYPE_RE.matcher(input.readText());

            if (m.find() && m.end() == input.readLength()) {
                input.backup(1);
                this.wasSnippetPart = true;
                
                return fact.createToken(DeclarativeHintTokenId.PATTERN, input.readLength(), wasSnippetPartCopy ? PartType.MIDDLE : PartType.START);
            }
        }

        if (input.read() != LexerInput.EOF) {
            this.wasSnippetPart = false;
            return fact.createToken(DeclarativeHintTokenId.PATTERN, input.readLength(), wasSnippetPartCopy ? PartType.END : PartType.COMPLETE);
        }

        return null;
    }

    private Token<DeclarativeHintTokenId> readType() {
        int read = input.read();

        assert read == '{';

        while ((read != LexerInput.EOF) && read != '}') {
            read = input.read();
        }

        return fact.createToken(DeclarativeHintTokenId.TYPE);
    }

    private static final Pattern VARIABLE_WITH_TYPE_RE = Pattern.compile("\\$[A-Za-z0-9_]\\{");

    private static final class State {
        private final int state;
        private final boolean wasSnippetPart;

        public State(int state, boolean wasSnippetPart) {
            this.state = state;
            this.wasSnippetPart = wasSnippetPart;
        }

    }
}
