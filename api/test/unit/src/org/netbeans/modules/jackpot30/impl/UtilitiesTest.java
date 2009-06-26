package org.netbeans.modules.jackpot30.impl;

import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Collections;

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

}