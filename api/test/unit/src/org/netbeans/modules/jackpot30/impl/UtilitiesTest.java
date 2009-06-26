package org.netbeans.modules.jackpot30.impl;

import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Collections;
import javax.lang.model.type.TypeMirror;

/**
 *
 * @author lahvac
 */
public class UtilitiesTest extends TestBase {

    public UtilitiesTest(String name) {
        super(name);
    }

    public void testParseAndAttributeExpressionStatement() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.singletonMap("$1", info.getTreeUtilities().parseType("int", info.getTopLevelElements().get(0))));
        Tree result = Utilities.parseAndAttribute(info, "$1 = 1;", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.EXPRESSION_STATEMENT);
    }

    public void testParseAndAttributeVariable() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.singletonMap("$1", info.getTreeUtilities().parseType("int", info.getTopLevelElements().get(0))));
        Tree result = Utilities.parseAndAttribute(info, "int $2 = $1;", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.VARIABLE);
    }

    public void testParseAndAttributeMultipleStatements() throws Exception {
        prepareTest("test/Test.java", "package test; public class Test{}");

        Scope s = Utilities.constructScope(info, Collections.<String, TypeMirror>emptyMap());
        Tree result = Utilities.parseAndAttribute(info, "String $2 = $1; int $l = $2.length(); System.err.println($l);", s);

        assertTrue(result.getKind().name(), result.getKind() == Kind.BLOCK);

        String golden = "{\n" +
                        "    $$1;\n" +
                        "    String $2 = $1;\n" +
                        "    int $l = $2.length();\n" +
                        "    System.err.println($l);\n" +
                        "    $$2;\n" +
                        "}";
        assertEquals(golden.replaceAll("[ \n\r]+", " "), result.toString().replaceAll("[ \n\r]+", " "));
    }

}