package org.netbeans.modules.jackpot30.server.indexer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.indexing.Index.IndexWriter;

/**
 *
 * @author lahvac
 */
public class StandaloneIndexer {

    public static void index(File root) throws IOException {
        IndexWriter w = Index.get(root.toURI().toURL()).openForWriting();

        try {
            new StandaloneIndexer().doIndex(w, root);
        } finally {
            w.close();
        }
    }

    private void doIndex(IndexWriter w, File fileOrDir) throws IOException {
        if (fileOrDir.isDirectory()) {
            for (File f : fileOrDir.listFiles()) {
                doIndex(w, f);
            }
        } else {
            indexFile(w, fileOrDir);
        }
    }

    private void indexFile(IndexWriter w, File source) throws IOException {
        if (!source.getName().endsWith(".java"))
            return ;
        
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        StandardJavaFileManager m = tool.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> fos = m.getJavaFileObjects(source);
        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath), null, fos);
        CompilationUnitTree cut = ct.parse().iterator().next();

        w.record(source.toURI().toURL(), cut);
    }
    
}
