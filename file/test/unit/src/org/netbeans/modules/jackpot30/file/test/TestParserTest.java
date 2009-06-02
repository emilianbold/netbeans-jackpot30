package org.netbeans.modules.jackpot30.file.test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.modules.jackpot30.file.test.TestParser.TestCase;

/**
 *
 * @author lahvac
 */
public class TestParserTest {

    public TestParserTest() {}

    @Test
    public void testParse1() {
        String code = "%%TestCase name\ncode\n%%=>\nfixed1\n%%=>\nfixed2\n";

        code += code;
        
        List<String> golden = Arrays.asList("name:code\n:[fixed1\n, fixed2\n]:0:16:[26, 38]",
                                            "name:code\n:[fixed1\n, fixed2\n]:45:61:[71, 83]");
        List<String> testCases = new LinkedList<String>();

        for (TestCase ts : TestParser.parse(code)) {
            testCases.add(ts.toString());
        }

        assertEquals(golden, testCases);
    }

    @Test
    public void testNoResults() {
        String code = "%%TestCase name\ncode\n";

        code += code;

        List<String> golden = Arrays.asList("name:code\n:[]:0:16:[]",
                                            "name:code\n:[]:21:37:[]");
        List<String> testCases = new LinkedList<String>();

        for (TestCase ts : TestParser.parse(code)) {
            testCases.add(ts.toString());
        }

        assertEquals(golden, testCases);
    }

}