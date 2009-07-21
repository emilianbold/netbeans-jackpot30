package org.netbeans.modules.jackpot30.impl.indexing;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.jackpot30.impl.indexing.Index.IndexWriter;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class CustomIndexerImpl extends CustomIndexer {

    @Override
    protected void index(Iterable<? extends Indexable> files, Context context) {
        final IndexWriter[] w = new IndexWriter[1];
        
        try {
            Collection<FileObject> toIndex = new LinkedList<FileObject>(); //XXX: better would be to use File

            for (Indexable i : files) {
                FileObject f = URLMapper.findFileObject(i.getURL());

                if (f != null) {
                    toIndex.add(f);
                }
            }

            if (toIndex.isEmpty()) {
                return ;
            }

            ClasspathInfo cpInfo = ClasspathInfo.create(context.getRoot());
            w[0] = Index.get(context.getRootURI()).openForWriting();

            JavaSource.create(cpInfo, toIndex).runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController cc) throws Exception {
                    if (cc.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0)
                        return ;

                    w[0].record(cc.getFileObject().getURL(), cc.getCompilationUnit());
                }
            }, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (w[0] != null) {
                try {
                    w[0].close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    public static final class CustomIndexerFactoryImpl extends CustomIndexerFactory {

        @Override
        public CustomIndexer createIndexer() {
            return new CustomIndexerImpl();
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            IndexWriter w = null;

            try {
                w = Index.get(context.getRootURI()).openForWriting();

                for (Indexable i : deleted) {
                    w.remove(i.getRelativePath());
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                if (w != null) {
                    try {
                        w.close();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {}

        @Override
        public String getIndexerName() {
            return Cache.NAME;
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return false;
        }

        @Override
        public int getIndexVersion() {
            return Cache.VERSION;
        }
        
    }

}
