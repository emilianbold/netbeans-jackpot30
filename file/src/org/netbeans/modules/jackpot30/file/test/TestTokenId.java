package org.netbeans.modules.jackpot30.file.test;

import java.util.Arrays;
import java.util.Collection;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.spi.lexer.LanguageEmbedding;
import org.netbeans.spi.lexer.LanguageHierarchy;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 *
 * @author lahvac
 */
public enum TestTokenId implements TokenId {

    METADATA("metadata"),
    JAVA_CODE("snippet");

    private final String category;

    private TestTokenId(String category) {
        this.category = category;
    }

    public String primaryCategory() {
        return category;
    }

    private static final Language<TestTokenId> language = new LanguageHierarchy<TestTokenId>() {
        @Override
        protected Collection<TestTokenId> createTokenIds() {
            return Arrays.asList(TestTokenId.values());
        }

        @Override
        protected Lexer<TestTokenId> createLexer(LexerRestartInfo<TestTokenId> info) {
            return new TestLexer(info);
        }

        @Override
        protected String mimeType() {
            return "text/x-javahintstest";
        }

        @Override
        protected LanguageEmbedding<?> embedding(Token<TestTokenId> token, LanguagePath languagePath, InputAttributes inputAttributes) {
            switch (token.id()) {
                case JAVA_CODE:
                    return LanguageEmbedding.create(Language.find("text/x-java"), 0, 0);
                default:
                    return null;
            }
        }

    }.language();

    public static Language<TestTokenId> language() {
        return language;
    }
}
