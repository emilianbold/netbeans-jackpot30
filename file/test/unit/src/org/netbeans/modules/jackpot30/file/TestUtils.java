package org.netbeans.modules.jackpot30.file;

import org.netbeans.api.lexer.PartType;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;

import static org.junit.Assert.*;

/**
 *
 * @author lahvac
 */
public class TestUtils {

    public static void assertNextTokenEquals(TokenSequence<?> ts, TokenId id, String text) {
        assertNextTokenEquals(ts, id, PartType.COMPLETE, text);
    }

    public static void assertNextTokenEquals(TokenSequence<?> ts, TokenId id, PartType pt, String text) {
        assertTrue(ts.moveNext());

        Token<?> t = ts.token();

        assertNotNull(t);
        assertEquals(id, t.id());
        assertEquals(pt, t.partType());
        assertEquals(text, t.text().toString());
    }

}
