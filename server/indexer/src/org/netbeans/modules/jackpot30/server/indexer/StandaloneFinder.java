package org.netbeans.modules.jackpot30.server.indexer;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;

/**
 *
 * @author lahvac
 */
public class StandaloneFinder {

    public static Collection<? extends String> findCandidates(File sourceRoot, String pattern) throws IOException {
        BulkPattern bulkPattern = BulkSearch.getDefault().create(Collections.singleton(pattern), Collections.singleton(parsePattern(pattern)));
        
        return Index.get(sourceRoot.toURI().toURL()).findCandidates(bulkPattern);
    }

    private static Tree parsePattern(String pattern) {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath), null, Collections.<JavaFileObject>emptyList());

        return Utilities.parseAndAttribute(ct, pattern);
    }
}
