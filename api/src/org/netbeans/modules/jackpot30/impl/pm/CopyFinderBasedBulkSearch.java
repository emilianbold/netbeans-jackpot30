package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;

/**
 *
 * @author lahvac
 */
public class CopyFinderBasedBulkSearch extends BulkSearch {

    public CopyFinderBasedBulkSearch() {
        super(false);
    }

    @Override
    public Map<String, Collection<TreePath>> match(CompilationInfo info, Tree tree, BulkPattern pattern, Map<String, Long> timeLog) {
        Map<String, Collection<TreePath>> result = new HashMap<String, Collection<TreePath>>();
        
        for (Entry<Tree, String> e : ((BulkPatternImpl) pattern).pattern2Code.entrySet()) {
            TreePath topLevel = new TreePath(info.getCompilationUnit());
            
            for (TreePath r : CopyFinder.computeDuplicates(info, new TreePath(topLevel, e.getKey()), topLevel, false, new AtomicBoolean(), Collections.<String, TypeMirror>emptyMap()).keySet()) {
                Collection<TreePath> c = result.get(e.getValue());

                if (c == null) {
                    result.put(e.getValue(), c = new LinkedList<TreePath>());
                }

                c.add(r);
            }
        }

        return result;
    }

    @Override
    public boolean matches(CompilationInfo info, Tree tree, BulkPattern pattern) {
        //XXX: performance
        return !match(info, tree, pattern).isEmpty();
    }

    @Override
    public BulkPattern create(Collection<? extends String> code, Collection<? extends Tree> patterns) {
        Map<Tree, String> pattern2Code = new HashMap<Tree, String>();

        Iterator<? extends String> itCode = code.iterator();
        Iterator<? extends Tree>   itPatt = patterns.iterator();

        while (itCode.hasNext() && itPatt.hasNext()) {
            pattern2Code.put(itPatt.next(), itCode.next());
        }

        return new BulkPatternImpl(pattern2Code);
    }

    @Override
    public boolean matches(InputStream encoded, BulkPattern pattern) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void encode(Tree tree, EncodingContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static final class BulkPatternImpl extends BulkPattern {

        private final Map<Tree, String> pattern2Code;
        
        public BulkPatternImpl(Map<Tree, String> pattern2Code) {
            super(null, null);
            this.pattern2Code = pattern2Code;
        }

    }

}
