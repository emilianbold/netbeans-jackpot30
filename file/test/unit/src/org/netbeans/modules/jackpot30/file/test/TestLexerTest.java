package org.netbeans.modules.jackpot30.file.test;

import org.junit.Test;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenHierarchy;

import static org.junit.Assert.*;
import static org.netbeans.modules.jackpot30.file.test.TestTokenId.*;
import static org.netbeans.modules.jackpot30.file.TestUtils.*;

/**
 *
 * @author lahvac
 */
public class TestLexerTest {

    public TestLexerTest() {
    }

    @Test
    public void testSimple() {
        String text = "%%TestCase name\njava code\n%%=>\ntarget\n";
        TokenHierarchy<?> hi = TokenHierarchy.create(text, language());
        TokenSequence<?> ts = hi.tokenSequence();
        assertNextTokenEquals(ts, METADATA, "%%TestCase name\n");
        assertNextTokenEquals(ts, JAVA_CODE, "java code\n");
        assertNextTokenEquals(ts, METADATA, "%%=>\n");
        assertNextTokenEquals(ts, JAVA_CODE, "target\n");

        assertFalse(ts.moveNext());
    }

}
