package org.netbeans.modules.jackpot30.file;

import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;

import static org.netbeans.modules.jackpot30.file.DeclarativeHintTokenId.*;
import static org.netbeans.modules.jackpot30.file.TestUtils.*;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintLexerTest {

    public DeclarativeHintLexerTest() {
    }

    @Test
    public void testSimple() {
        String text = " \'test\': 1 + 1 => \'fix\': 1 + 1;;";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, DISPLAY_NAME, "\'test\':");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, DISPLAY_NAME, "\'fix\':");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testSimpleNoDisplayNames() {
        String text = " 1 + 1 => 1 + 1;;";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testWhitespaceAtTheEnd() {
        String text = " 1 + 1 => 1 + 1;; ";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");
        assertNextTokenEquals(ts, WHITESPACE, " ");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testVariable() {
        String text = " $1 + 1 => 1 + $1;; ";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, WHITESPACE, " ");//TODO
        assertNextTokenEquals(ts, VARIABLE, "$1");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + ");
        assertNextTokenEquals(ts, VARIABLE, "$1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");
        assertNextTokenEquals(ts, WHITESPACE, " ");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testCondition() {
        String text = " 1 + 1 :: $1 instanceof something && $test instanceof somethingelse => 1 + 1;; ";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1 ");
        assertNextTokenEquals(ts, DOUBLE_COLON, "::");
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, VARIABLE, "$1");
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, INSTANCEOF, "instanceof");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " something ");
        assertNextTokenEquals(ts, AND, "&&");
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, VARIABLE, "$test");
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, INSTANCEOF, "instanceof");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " somethingelse ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");
        assertNextTokenEquals(ts, WHITESPACE, " ");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testMultiple() {
        String text = "'test': 1 + 1 => 1 + 1;;'test2': 1 + 1 => 1 + 1;;";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, DISPLAY_NAME, "'test':");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");
        assertNextTokenEquals(ts, DISPLAY_NAME, "'test2':");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, JAVA_SNIPPET, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testNot() {
        String text = "!";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, NOT, "!");

        assertFalse(ts.moveNext());
    }
}