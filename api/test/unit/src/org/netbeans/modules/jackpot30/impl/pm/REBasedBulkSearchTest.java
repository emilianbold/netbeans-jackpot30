package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Map;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;

/**
 *
 * @author lahvac
 */
public class REBasedBulkSearchTest extends BulkSearchTestPerformer {

    public REBasedBulkSearchTest(String name) {
        super(name);
    }

    @Override
    protected BulkPattern create(CompilationInfo info, Collection<? extends String> patterns) {
        return new REBasedBulkSearch().create(info, patterns);
    }

    @Override
    protected Map<String, Collection<TreePath>> match(CompilationInfo info, Tree tree, BulkPattern pattern) {
        return new REBasedBulkSearch().match(info, tree, pattern);
    }

}