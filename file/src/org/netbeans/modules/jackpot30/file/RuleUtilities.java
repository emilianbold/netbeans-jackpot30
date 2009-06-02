package org.netbeans.modules.jackpot30.file;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.openide.modules.SpecificationVersion;

/**
 *
 * @author lahvac
 */
public class RuleUtilities {

    private RuleUtilities() {
    }

    public static boolean referencedIn(final HintContext ctx, TreePath variable, Iterable<? extends TreePath> in) {
        final Element e = ctx.getInfo().getTrees().getElement(variable);

        if (e == null) { //TODO: check also error
            return false;
        }

        for (TreePath tp : in) {
            boolean occurs = new TreePathScanner<Boolean, Void>() {
                @Override
                public Boolean scan(Tree tree, Void p) {
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

    public static boolean sourceVersionGE(final HintContext ctx, SourceVersion source) {
        String sourceLevel = SourceLevelQuery.getSourceLevel(ctx.getInfo().getFileObject());

        if (sourceLevel == null) {
            return false;//TODO:???
        }

        String designedSourceLevel = "1." + source.name().substring(source.name().indexOf("_") + 1);
        return new SpecificationVersion(sourceLevel).compareTo(new SpecificationVersion(designedSourceLevel)) >= 0;
    }

    public static boolean hasModifier(HintContext ctx, TreePath variable, Modifier modifier) {
        final Element e = ctx.getInfo().getTrees().getElement(variable);

        if (e == null) {
            return false;
        }

        return e.getModifiers().contains(modifier);
    }
}
