package org.netbeans.modules.jackpot30.file;

import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.impl.TestBase;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.java.hints.spi.AbstractHint.HintSeverity;

/**
 *
 * @author lahvac
 */
public class RuleUtilitiesTest extends TestBase {

    public RuleUtilitiesTest(String name) {
        super(name);
    }

    public void testReferencedInNoNPEForMissingTrees() throws Exception {
        String code = "package test; public class Test { private void test() { | if (true) System.err.println(); } private int a^aa;}";
        int pos = code.indexOf("|");
        
        code = code.replaceAll(Pattern.quote("|"), "");

        int varpos = code.indexOf("^");
        
        code = code.replaceAll(Pattern.quote("^"), "");

        prepareTest("test/Test.java", code);

        HintContext ctx = HintContext.create(info, HintSeverity.ERROR, null, null, null, null);
        TreePath tp = info.getTreeUtilities().pathFor(pos);
        TreePath var = info.getTreeUtilities().pathFor(varpos);

        RuleUtilities.referencedIn(ctx, var, Collections.singletonList(tp));
    }

}