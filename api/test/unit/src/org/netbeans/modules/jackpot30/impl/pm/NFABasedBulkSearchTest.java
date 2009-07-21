package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Map;
import junit.framework.TestSuite;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;

/**
 *
 * @author lahvac
 */
public class NFABasedBulkSearchTest extends BulkSearchTestPerformer {

    public NFABasedBulkSearchTest(String name) {
        super(name);
    }

//    public static TestSuite suite() {
//        NbTestSuite r = new NbTestSuite();
//
//        r.addTest(new NFABasedBulkSearchTest("testSerialization"));
//
//        return r;
//    }
    
    @Override
    protected BulkSearch createSearch() {
        return new NFABasedBulkSearch();
    }

}
