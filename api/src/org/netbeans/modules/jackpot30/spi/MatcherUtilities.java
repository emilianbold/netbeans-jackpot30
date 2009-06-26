package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder;

/**
 *
 * @author lahvac
 */
public class MatcherUtilities {

    public static boolean parentMatches(@NonNull HintContext ctx, @NonNull String pattern) {
        Scope s = Utilities.constructScope(ctx.getInfo(), Collections.<String, TypeMirror>emptyMap());
        Tree  patternTree = Utilities.parseAndAttribute(ctx.getInfo(), pattern, s);
        TreePath patternTreePath = new TreePath(new TreePath(ctx.getInfo().getCompilationUnit()), patternTree);
        TreePath parent = ctx.getPath().getParentPath();
        Map<String, TreePath> variables = new HashMap<String, TreePath>(ctx.getVariables());

        variables.put("$_", ctx.getPath());

        return CopyFinder.isDuplicate(ctx.getInfo(), patternTreePath, parent, true, variables, new AtomicBoolean()/*XXX*/);
    }

}
