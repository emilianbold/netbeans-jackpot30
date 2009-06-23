package org.netbeans.modules.jackpot30.impl.batch;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class BatchSearch {

    public static BatchResult findOccurrences(HintDescription pattern, Scope scope) {
        if (pattern.getTriggerKind() != null || pattern.getTriggerPattern() == null) {
            throw new UnsupportedOperationException();
        }
        
        switch (scope) {
            case ALL_OPENED_PROJECTS:
                return findOccurrencesLocal(pattern);
            default:
                throw new UnsupportedOperationException(scope.name());
        }
    }

    private static BatchResult findOccurrencesLocal(final HintDescription pattern) {
        final BatchResult[] result = new BatchResult[1];

        try {
            JavaSource.create(Utilities.createUniversalCPInfo()).runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController parameter) throws Exception {
                    result[0] = findOccurrencesLocalImpl(parameter, pattern);
                }
            }, true);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        return result[0];
    }
    private static BatchResult findOccurrencesLocalImpl(CompilationInfo info, HintDescription pattern) {
        String textPattern = pattern.getTriggerPattern().getPattern();
        Tree treePattern = Utilities.parseAndAttribute(info, textPattern, null);
        BulkPattern bulkPattern = BulkSearch.getDefault().create(Collections.singleton(textPattern), Collections.singleton(treePattern));
        Map<Container, Collection<Resource>> result = new HashMap<Container, Collection<Resource>>();
        
        for (FileObject src : GlobalPathRegistry.getDefault().getSourceRoots()) {
            try {
                Index i = Index.get(src.getURL());

                if (i == null) {
                    continue;
                }
                
                Container id = new LocalContainer(src, i);

                for (String candidate : i.findCandidates(bulkPattern)) {
                    Collection<Resource> resources = result.get(id);

                    if (resources == null) {
                        result.put(id, resources = new LinkedList<Resource>());
                    }

                    resources.add(new Resource(id, candidate, textPattern, bulkPattern));
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return new BatchResult(result);
    }

    public enum Scope {
        ALL_OPENED_PROJECTS,
        ALL_REMOTE_PROJECTS;
    }

    public static final class BatchResult {
        
        public final Map<? extends Container, ? extends Iterable<? extends Resource>> projectId2Resources;

        public BatchResult(Map<? extends Container, ? extends Iterable<? extends Resource>> projectId2Resources) {
            this.projectId2Resources = projectId2Resources;
        }

    }

    public static abstract class Container {
        Container() {}

        public abstract boolean isLocal();
               abstract CharSequence getSourceCode(String relativePath);
               abstract FileObject resolve(String relativePath);
               abstract String toDebugString() throws Exception;
    }

    public static final class LocalContainer extends Container {

        private final FileObject localFO;
        private final Index index;
        
        LocalContainer(@NonNull FileObject localFO, @NonNull Index index) {
            this.localFO = localFO;
            this.index = index;
        }
        
        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        CharSequence getSourceCode(String relativePath) {
            return index.getSourceCode(relativePath);
        }

        @Override
        FileObject resolve(String relativePath) {
            return localFO.getFileObject(relativePath);
        }

        @Override
        String toDebugString() throws FileStateInvalidException {
            return localFO.getURL().toExternalForm();
        }

    }

    public static final class Resource {
        private final Container container;
        private final String relativePath;
        private final String treePatternCode;
        private final BulkPattern pattern;

        public Resource(Container container, String relativePath, String treePatternCode, BulkPattern pattern) {
            this.container = container;
            this.relativePath = relativePath;
            this.treePatternCode = treePatternCode;
            this.pattern = pattern;
        }

        public String getRelativePath() {
            return relativePath;
        }
        
        public Iterable<int[]> getCandidateSpans() {
            FileObject file = getResolvedFile();
            JavaSource js;

            if (file != null) {
                js = JavaSource.forFileObject(file);
            } else {
                CharSequence text = getSourceCode();

                if (text == null) {
                    return null;
                }

                Writer out = null;

                try {
                    file = FileUtil.createData(FileUtil.createMemoryFileSystem().getRoot(), relativePath);
                    out = new OutputStreamWriter(file.getOutputStream());

                    out.write(text.toString());
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                    return null;
                } finally {
                    if (out != null) {
                        try {
                            out.close();
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }

                js = JavaSource.create(Utilities.createUniversalCPInfo(), file);
            }

            final List<int[]> span = new LinkedList<int[]>();

            try {
                js.runUserActionTask(new Task<CompilationController>() {
                    public void run(CompilationController cc) throws Exception {
                        cc.toPhase(Phase.PARSED);

                        span.addAll(doComputeSpans(cc));
                    }
                }, true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            return span;
        }

        private Collection<int[]> doComputeSpans(CompilationInfo ci) {
            Collection<int[]> result = new LinkedList<int[]>();
            Map<String, Collection<TreePath>> found = BulkSearch.getDefault().match(ci, ci.getCompilationUnit(), pattern);
            Tree treePattern = Utilities.parseAndAttribute(ci, treePatternCode, null);

            for (Collection<TreePath> tps : found.values()) {
                for (TreePath tp : tps) {
                    //XXX: this pass will not be performed on the web!!!
                    if (   BulkSearch.getDefault().requiresLightweightVerification()
                        && !CopyFinder.isDuplicate(ci, new TreePath(new TreePath(ci.getCompilationUnit()), treePattern), tp, false, new AtomicBoolean())) {
                        continue;
                    }
                    int[] span = new int[] {
                        (int) ci.getTrees().getSourcePositions().getStartPosition(ci.getCompilationUnit(), tp.getLeaf()),
                        (int) ci.getTrees().getSourcePositions().getEndPosition(ci.getCompilationUnit(), tp.getLeaf())
                    };

                    result.add(span);
                }
            }

            return result;
        }
        
        public FileObject getResolvedFile() {
            return container.resolve(relativePath);
        }

        public String getDisplayName() {
            FileObject file = getResolvedFile();

            if (file != null) {
                return FileUtil.getFileDisplayName(file);
            } else {
                return relativePath; //TODO:+container
            }
        }
        
        public CharSequence getSourceCode() {
            return container.getSourceCode(relativePath);
        }
    }

}
