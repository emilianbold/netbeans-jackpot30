package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.Collections;
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

    public static boolean matches(@NonNull HintContext ctx, @NonNull TreePath variable, @NonNull String pattern) {
        Scope s = Utilities.constructScope(ctx.getInfo(), Collections.<String, TypeMirror>emptyMap());
        Tree  patternTree = Utilities.parseAndAttribute(ctx.getInfo(), pattern, s);
        TreePath patternTreePath = new TreePath(new TreePath(ctx.getInfo().getCompilationUnit()), patternTree);
        
        return CopyFinder.isDuplicate(ctx.getInfo(), patternTreePath, variable, true, ctx.getVariables(), new AtomicBoolean()/*XXX*/);
    }

}
