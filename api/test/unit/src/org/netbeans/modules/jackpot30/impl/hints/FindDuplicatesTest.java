package org.netbeans.modules.jackpot30.impl.hints;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.impl.TestBase;
import org.netbeans.modules.java.source.pretty.VeryPretty;

/**
 *
 * @author lahvac
 */
public class FindDuplicatesTest extends TestBase {

    public FindDuplicatesTest(String name) {
        super(name);
    }

    public void testLocalVariablesGeneralization() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        |int i = 0;\n" +
                    "        i++;\n" +
                    "        int j = i;|\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "{\n" +
                    "    $s0$;\n" +
                    "    int $0 = 0;\n" +
                    "    $0++;\n" +
                    "    int $1 = $0;\n" +
                    "    $s1$;\n" +
                    "}");
    }
    
    public void testSingleStatementPattern() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        int i = 0;\n" +
                    "        |i++;|\n" +
                    "        int j = i;\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "$0++;");
    }

    public void testExpressionPattern() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        int i = 0;\n" +
                    "        i++;\n" +
                    "        int j = |i|;\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "$0");
    }

    public void testVoidMethod() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        |System.err.println()|;\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "System.err.println()");
    }

    private void performGeneralizationTest(String code, String generalized) throws Exception {
        String[] split = code.split(Pattern.quote("|"));

        assertEquals(3, split.length);

        int start = split[0].length();
        int end   = split[1].length() + start;
        
        code = split[0] + split[1] + split[2];

        prepareTest("test/Test.java", code);

        Tree generalizedTree = FindDuplicates.resolveAndGeneralizePattern(info, start, end);
        VeryPretty vp = new VeryPretty(info);

        vp.print((JCTree) generalizedTree);
        
        String repr = vp.toString();
        
        assertEquals(generalized.replaceAll("[ \n\t]+", " "),
                     repr.replaceAll("[ \n\t]+", " "));
    }

}