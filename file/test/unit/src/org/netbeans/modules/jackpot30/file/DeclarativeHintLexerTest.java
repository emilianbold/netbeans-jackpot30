package org.netbeans.modules.jackpot30.file;

import org.junit.Test;
import org.netbeans.api.lexer.PartType;
import static org.junit.Assert.*;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;

import static org.netbeans.modules.jackpot30.file.DeclarativeHintTokenId.*;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintLexerTest {

    public DeclarativeHintLexerTest() {
    }

    @Test
    public void testSimple() {
        String text = " \"test\": 1 + 1 => \"fix\": 1 + 1;;";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, DISPLAY_NAME, "\"test\"");
        assertNextTokenEquals(ts, COLON, ":");
        assertNextTokenEquals(ts, PATTERN, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, WHITESPACE, " ");
        assertNextTokenEquals(ts, DISPLAY_NAME, "\"fix\"");
        assertNextTokenEquals(ts, COLON, ":");
        assertNextTokenEquals(ts, PATTERN, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testSimpleNoDisplayNames() {
        String text = " 1 + 1 => 1 + 1;;";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, PATTERN, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, PATTERN, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");

        assertFalse(ts.moveNext());
    }
    
    @Test
    public void testWhitespaceAtTheEnd() {
        String text = " 1 + 1 => 1 + 1;; ";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, PATTERN, " 1 + 1 ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, PATTERN, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");
        assertNextTokenEquals(ts, WHITESPACE, " ");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testVariablesWithTypes1() {
        String text = " $1{int} + $2{int} => 1 + 1;; ";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, PATTERN, PartType.START, " $1");
        assertNextTokenEquals(ts, TYPE,  "{int}");
        assertNextTokenEquals(ts, PATTERN, PartType.MIDDLE, " + $2");
        assertNextTokenEquals(ts, TYPE,  "{int}");
        assertNextTokenEquals(ts, PATTERN, PartType.END, " ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, PATTERN, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");
        assertNextTokenEquals(ts, WHITESPACE, " ");

        assertFalse(ts.moveNext());
    }

    @Test
    public void testLongVariables() {
        String text = " $this{int} + $that{int} => 1 + 1;; ";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, PATTERN, PartType.START, " $this");
        assertNextTokenEquals(ts, TYPE,  "{int}");
        assertNextTokenEquals(ts, PATTERN, PartType.MIDDLE, " + $that");
        assertNextTokenEquals(ts, TYPE,  "{int}");
        assertNextTokenEquals(ts, PATTERN, PartType.END, " ");
        assertNextTokenEquals(ts, LEADS_TO, "=>");
        assertNextTokenEquals(ts, PATTERN, " 1 + 1");
        assertNextTokenEquals(ts, DOUBLE_SEMICOLON, ";;");
        assertNextTokenEquals(ts, WHITESPACE, " ");

        assertFalse(ts.moveNext());
    }

    public static void assertNextTokenEquals(TokenSequence<?> ts, DeclarativeHintTokenId id, String text) {
        assertNextTokenEquals(ts, id, PartType.COMPLETE, text);
    }

    public static void assertNextTokenEquals(TokenSequence<?> ts, DeclarativeHintTokenId id, PartType pt, String text) {
        assertTrue(ts.moveNext());

        Token t = ts.token();

        assertNotNull(t);
        assertEquals(id, t.id());
        assertEquals(pt, t.partType());
        assertEquals(text, t.text().toString());
    }

}