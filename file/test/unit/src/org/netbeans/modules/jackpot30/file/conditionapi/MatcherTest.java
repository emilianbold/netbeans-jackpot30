package org.netbeans.modules.jackpot30.file.conditionapi;

import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.impl.TestBase;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.java.hints.spi.AbstractHint.HintSeverity;

/**
 *
 * @author lahvac
 */
public class MatcherTest extends TestBase {

    public MatcherTest(String name) {
        super(name);
    }

    public void testReferencedInNoNPEForMissingTrees() throws Exception {
        String code = "package test; public class Test { private void test() { | if (true) System.err.println(); } private int a^aa;}";
        int pos = code.indexOf("|");
        
        code = code.replaceAll(Pattern.quote("|"), "");

        int varpos = code.indexOf("^");
        
        code = code.replaceAll(Pattern.quote("^"), "");

        prepareTest("test/Test.java", code);

        TreePath tp = info.getTreeUtilities().pathFor(pos);
        TreePath var = info.getTreeUtilities().pathFor(varpos);
        Map<String, TreePath> variables = new HashMap<String, TreePath>();
        variables.put("$1", var);
        Map<String, Collection<? extends TreePath>> multiVariables = new HashMap<String, Collection<? extends TreePath>>();
        multiVariables.put("$2$", Arrays.asList(tp));
        HintContext ctx = HintContext.create(info, HintSeverity.ERROR, null, variables, multiVariables, null);

        new Matcher(ctx).referencedIn(new Variable("$1"), new Variable("$2$"));
    }

}
