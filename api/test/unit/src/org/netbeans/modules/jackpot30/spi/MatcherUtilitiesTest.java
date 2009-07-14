package org.netbeans.modules.jackpot30.spi;

import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.impl.TestBase;
import org.netbeans.modules.java.hints.spi.AbstractHint.HintSeverity;

/**
 *
 * @author lahvac
 */
public class MatcherUtilitiesTest extends TestBase {

    public MatcherUtilitiesTest(String name) {
        super(name);
    }

    public void testParentMatches1() throws Exception {
        String code = "package test; public class Test { private int test() { int i = 0; i = test(|); } }";
        int pos = code.indexOf("|");

        code = code.replaceAll(Pattern.quote("|"), "");

        prepareTest("test/Test.java", code);

        TreePath tp = info.getTreeUtilities().pathFor(pos);
        HintContext ctx = HintContext.create(info, HintSeverity.ERROR, tp, Collections.<String, TreePath>emptyMap(), null, null);

        assertTrue(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), "$0 = $_"));
    }

    public void testParentMatches2() throws Exception {
        String code = "package test; public class Test { private int test() { int i = test(|); } }";
        int pos = code.indexOf("|");

        code = code.replaceAll(Pattern.quote("|"), "");

        prepareTest("test/Test.java", code);

        TreePath tp = info.getTreeUtilities().pathFor(pos);
        HintContext ctx = HintContext.create(info, HintSeverity.ERROR, tp, Collections.<String, TreePath>emptyMap(), null, null);

        assertTrue(MatcherUtilities.matches(ctx, ctx.getPath().getParentPath(), "$1 $0 = $_;"));
    }

}