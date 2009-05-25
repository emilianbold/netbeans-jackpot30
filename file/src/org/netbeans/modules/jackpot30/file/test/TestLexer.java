package org.netbeans.modules.jackpot30.file.test;

import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

/**
 *
 * @author lahvac
 */
class TestLexer implements Lexer<TestTokenId> {

    private final LexerInput input;
    private final TokenFactory<TestTokenId> factory;

    public TestLexer(LexerRestartInfo<TestTokenId> info) {
        this.input = info.input();
        this.factory = info.tokenFactory();
    }

    public Token<TestTokenId> nextToken() {
        if (input.read() == LexerInput.EOF) {
            return null;
        }
        
        input.read();

        if (input.readText().toString().startsWith("%%")) {
            readUntil("\n");
            
            return factory.createToken(TestTokenId.METADATA);
        }

        if (readUntil("\n%%")) {
            input.backup(2);
        }

        return factory.createToken(TestTokenId.JAVA_CODE);
    }

    private boolean readUntil(String condition) {
        int read;

        while ((read = input.read()) != LexerInput.EOF && !input.readText().toString().endsWith(condition))
            ;

        return read != LexerInput.EOF;
    }

    public Object state() {
        return null;
    }

    public void release() {}

}
