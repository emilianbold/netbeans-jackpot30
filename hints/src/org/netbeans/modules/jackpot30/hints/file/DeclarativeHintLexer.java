/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javahints.file;

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

    public DeclarativeHintLexer(LexerRestartInfo<DeclarativeHintTokenId> info) {
        input = info.input();
        fact  = info.tokenFactory();

        if (info.state() != null) {
            state = (Integer) info.state();
        }
    }

    public Token<DeclarativeHintTokenId> nextToken() {
        if (input.read() == LexerInput.EOF) {
            return null;
        }

        input.backup(1);

        Token<DeclarativeHintTokenId> readWhiteSpace = readWhiteSpace();

        if (readWhiteSpace != null) {
            return readWhiteSpace;
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
                } else {
                    return fact.createToken(DeclarativeHintTokenId.ERROR);
                }
            case 1:
                state = 2;
                return readSnippet();
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
                    state = 0;
                    return fact.createToken(DeclarativeHintTokenId.SEMICOLON);
                }

                if (read == '"') {
                    input.backup(1);

                    s = readString();

                    if (s != null) {
                        return s;
                    }
                }

                return fact.createToken(DeclarativeHintTokenId.ERROR);
            case 3:
                state = 2;
                return readSnippet();
        }

        throw new IllegalStateException("" + state);
    }

    public Object state() {
        return state;
    }

    public void release() {
    }

    private Token<DeclarativeHintTokenId> readWhiteSpace() {
        int read = input.read();
        boolean create = false;

        while ((read != LexerInput.EOF) && Character.isWhitespace((char) read)) {
            read = input.read();
            create = true;
        }

        input.backup(1);
        
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
        while (input.read() != LexerInput.EOF) {
            if (input.readText().toString().endsWith("=>") || input.readText().toString().endsWith(";")) {
                input.backup(input.readText().toString().endsWith("=>") ? 2 : 1);

                while (Character.isWhitespace(input.readText().charAt(input.readText().length() - 1))) {
                    input.backup(1);
                }

                return fact.createToken(DeclarativeHintTokenId.PATTERN);
            }
        }

        if (input.read() != LexerInput.EOF) {
            return fact.createToken(DeclarativeHintTokenId.PATTERN);
        }

        return null;
    }
}
