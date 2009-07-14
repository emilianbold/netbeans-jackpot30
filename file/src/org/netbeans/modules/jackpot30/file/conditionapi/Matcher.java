package org.netbeans.modules.jackpot30.file.conditionapi;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import javax.lang.model.element.Element;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.MatcherUtilities;

/**
 *
 * @author lahvac
 */
public final class Matcher {

    private final HintContext ctx;

    //XXX: should not be public:
    public Matcher(HintContext ctx) {
        this.ctx = ctx;
    }

    public boolean matches(@NonNull Variable var, @NonNull String pattern) {
        return MatcherUtilities.matches(ctx, ctx.getVariables().get(var.variableName), pattern);
    }

    public boolean referencedIn(@NonNull Variable variable, @NonNull Variable in) {
        final Element e = ctx.getInfo().getTrees().getElement(ctx.getVariables().get(variable.variableName));

        if (e == null) { //TODO: check also error
            return false;
        }

        for (TreePath tp : Context.getVariable(ctx, in)) {
            boolean occurs = new TreePathScanner<Boolean, Void>() {
                @Override
                public Boolean scan(Tree tree, Void p) {
                    if (tree == null) {
                        return false;
                    }

                    TreePath currentPath = new TreePath(getCurrentPath(), tree);
                    Element currentElement = ctx.getInfo().getTrees().getElement(currentPath);

                    if (e.equals(currentElement)) {
                        return true; //TODO: throwing an exception might be faster...
                    }

                    return super.scan(tree, p);
                }

                @Override
                public Boolean reduce(Boolean r1, Boolean r2) {
                    if (r1 == null) {
                        return r2;
                    }

                    if (r2 == null) {
                        return r1;
                    }

                    return r1 || r2;
                }

            }.scan(tp, null) == Boolean.TRUE;

            if (occurs) {
                return true;
            }
        }

        return false;
    }

}
