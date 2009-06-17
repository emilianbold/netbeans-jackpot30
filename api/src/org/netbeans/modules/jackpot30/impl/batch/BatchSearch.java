package org.netbeans.modules.jackpot30.impl.batch;

import com.sun.source.tree.Tree;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer.Result;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class BatchSearch {

    public static BatchResult findOccurrences(PatternDescription pattern, Scope scope) {
        switch (scope) {
            case ALL_OPENED_PROJECTS:
                return findOccurrencesLocal(pattern);
            default:
                throw new UnsupportedOperationException(scope.name());
        }
    }

    private static BatchResult findOccurrencesLocal(final PatternDescription pattern) {
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
    private static BatchResult findOccurrencesLocalImpl(CompilationInfo info/*XXX*/, PatternDescription pattern) {
        Tree treePattern = Utilities.parseAndAttribute(info, pattern.getPattern(), null);
        Result serializedPattern = TreeSerializer.serializePatterns(treePattern);
        Map<Container, Collection<Resource>> result = new HashMap<Container, Collection<Resource>>();
        
        for (FileObject src : GlobalPathRegistry.getDefault().getSourceRoots()) {
            Container id = new LocalContainer(src);

            try {
                Index i = Index.get(src.getURL());

                if (i == null) {
                    continue;
                }

                for (String candidate : i.findCandidates(serializedPattern)) {
                    Collection<Resource> resources = result.get(id);

                    if (resources == null) {
                        result.put(id, resources = new LinkedList<Resource>());
                    }

                    resources.add(new Resource(id, candidate));
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
               abstract FileObject resolve(String relativePath);
               abstract String toDebugString() throws Exception;
    }

    public static final class LocalContainer extends Container {

        private final FileObject localFO;
        
        LocalContainer(FileObject localFO) {
            this.localFO = localFO;
        }
        
        @Override
        public boolean isLocal() {
            return true;
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

        public Resource(Container container, String relativePath) {
            this.container = container;
            this.relativePath = relativePath;
        }

        public String getRelativePath() {
            return relativePath;
        }
        
        public Iterable<int[]> getSpans() {
            throw new UnsupportedOperationException();
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
        
    }

}
