/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.batch;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.parsing.impl.indexing.PathRegistry;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbCollections;

/**
 *
 * @author lahvac
 */
public class BatchSearch {

    private static final Logger LOG = Logger.getLogger(BatchSearch.class.getName());

    public static BatchResult findOccurrences(Iterable<? extends HintDescription> patterns, Scope scope, Object... parameters) {
        for (HintDescription pattern : patterns) {
            if (pattern.getTriggerKind() != null || pattern.getTriggerPattern() == null) {
                throw new UnsupportedOperationException();
            }
        }

        Set<FileObject> knownSourceRoots = new HashSet<FileObject>(GlobalPathRegistry.getDefault().getSourceRoots());
        Set<FileObject> todo;
        
        switch (scope) {
            case ALL_OPENED_PROJECTS:
                todo = new HashSet<FileObject>();

                for (ClassPath source : GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)) {
                    todo.addAll(Arrays.asList(source.getRoots()));
                }
                
                return findOccurrencesLocal(patterns, knownSourceRoots, todo);
            case GIVEN_SOURCE_ROOTS:
                todo = NbCollections.checkedSetByCopy(new HashSet<Object>(Arrays.asList(parameters)), FileObject.class, true);

                return findOccurrencesLocal(patterns, knownSourceRoots, todo);
            case GIVEN_FOLDER:
                todo = Collections.singleton((FileObject) parameters[0]);

                return findOccurrencesLocal(patterns, knownSourceRoots, todo);
            default:
                throw new UnsupportedOperationException(scope.name());
        }
    }

    private static BatchResult findOccurrencesLocal(final Iterable<? extends HintDescription> patterns, final Set<FileObject> indexedSourceRoots, final Set<FileObject> todo) {
        final BatchResult[] result = new BatchResult[1];

        try {
            JavaSource.create(Utilities.createUniversalCPInfo()).runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController parameter) throws Exception {
                    result[0] = findOccurrencesLocalImpl(parameter, patterns, indexedSourceRoots, todo);
                }
            }, true);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        return result[0];
    }
    private static BatchResult findOccurrencesLocalImpl(CompilationInfo info, final Iterable<? extends HintDescription> patterns, Set<FileObject> indexedSourceRoots, Set<FileObject> todo) {
        Collection<String> code = new LinkedList<String>();
        Collection<Tree> trees = new LinkedList<Tree>();

        for (HintDescription pattern : patterns) {
            String textPattern = pattern.getTriggerPattern().getPattern();
            
            code.add(textPattern);
            trees.add(Utilities.parseAndAttribute(info, textPattern, null));
        }

        final BulkPattern bulkPattern = BulkSearch.getDefault().create(code, trees);
        final Map<Container, Collection<Resource>> result = new HashMap<Container, Collection<Resource>>();
        
        for (final FileObject src : todo) {
            LOG.log(Level.FINE, "Processing: {0}", FileUtil.getFileDisplayName(src));
            
            try {
                if (indexedSourceRoots.contains(src)) {
                    Index i = Index.get(src.getURL());

                    if (i == null)
                         continue;
                    
                    Container id = new LocalContainer(src);

                    for (String candidate : i.findCandidates(bulkPattern)) {
                        Collection<Resource> resources = result.get(id);

                        if (resources == null) {
                            result.put(id, resources = new LinkedList<Resource>());
                        }

                        resources.add(new Resource(id, candidate, patterns, bulkPattern));
                    }
                } else {
                    Collection<FileObject> files = new LinkedList<FileObject>();
                    final Container id = new LocalContainer(src);
                    
                    recursive(src, files);

                    LOG.log(Level.FINE, "files: {0}", files);

                    if (!files.isEmpty()) {
                        long start = System.currentTimeMillis();

                        JavaSource.create(Utilities.createUniversalCPInfo(), files).runUserActionTask(new Task<CompilationController>() {
                            public void run(CompilationController cc) throws Exception {
                                if (cc.toPhase(Phase.PARSED).compareTo(Phase.PARSED) <0) {
                                    return ;
                                }

                                boolean matches = BulkSearch.getDefault().matches(cc, cc.getCompilationUnit(), bulkPattern);

                                if (matches) {
                                    Collection<Resource> resources = result.get(id);

                                    if (resources == null) {
                                        result.put(id, resources = new LinkedList<Resource>());
                                    }

                                    resources.add(new Resource(id, FileUtil.getRelativePath(src, cc.getFileObject()), patterns, bulkPattern));
                                }
                            }
                        }, true);

                        long end = System.currentTimeMillis();

                        LOG.log(Level.FINE, "took: {0}, per file: {1}", new Object[]{end - start, (end - start) / files.size()});
                    }
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return new BatchResult(result);
    }

    private static void recursive(FileObject file, Collection<FileObject> collected) {
        if (file.isData()) {
            if (/*???:*/"java".equals(file.getExt()) || "text/x-java".equals(FileUtil.getMIMEType(file, "text/x-java"))) {
                collected.add(file);
            }
        } else {
            for (FileObject c : file.getChildren()) {
                recursive(c, collected);
            }
        }
    }
    
    public static Map<? extends Resource, Iterable<? extends ErrorDescription>> getVerifiedSpans(Iterable<? extends Resource> resources, final Collection<? super MessageImpl> problems) {
        Collection<FileObject> files = new LinkedList<FileObject>();
        final Map<FileObject, Resource> file2Resource = new HashMap<FileObject, Resource>();
        final Map<Resource, Iterable<? extends ErrorDescription>> resource2Errors = new HashMap<Resource, Iterable<? extends ErrorDescription>>();

        for (Resource r : resources) {
            if (r.areSpansComputed()) {
                resource2Errors.put(r, r.getVerifiedSpans(problems));
                continue;
            }
            
            FileObject file = r.getResolvedFile();

            if (file != null) {
                files.add(file);
                file2Resource.put(file, r);
            } else {
                r.setVerifiedSpans(null);
            }
        }

        Map<ClasspathInfo, Collection<FileObject>> cp2Files = BatchUtilities.sortFiles(files);
        Set<ClassPath> toRegisterSet = new HashSet<ClassPath>();

        for (ClasspathInfo cpInfo : cp2Files.keySet()) {
            toRegisterSet.add(cpInfo.getClassPath(PathKind.SOURCE));
        }

        ClassPath[] toRegister = !toRegisterSet.isEmpty() ? toRegisterSet.toArray(new ClassPath[0]) : null;

        if (toRegister != null) {
            GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, toRegister);
            try {
                Utilities.waitScanFinished();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        try {
            for (Entry<ClasspathInfo, Collection<FileObject>> e : cp2Files.entrySet()) {
                try {
                    JavaSource.create(e.getKey(), e.getValue()).runUserActionTask(new Task<CompilationController>() {
                        public void run(CompilationController parameter) throws Exception {
                            if (parameter.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                                return ;

                            Resource r = file2Resource.get(parameter.getFileObject());
                            Map<PatternDescription, List<HintDescription>> sortedHintsPatterns = new HashMap<PatternDescription, List<HintDescription>>();
                            Map<Kind, List<HintDescription>> sortedHintsKinds = new HashMap<Kind, List<HintDescription>>();

                            RulesManager.sortOut(r.hints, sortedHintsKinds, sortedHintsPatterns);

                            List<ErrorDescription> hints = new HintsInvoker().computeHints(parameter, sortedHintsKinds, sortedHintsPatterns, problems);

                            r.setVerifiedSpans(hints);
                            resource2Errors.put(r, hints);
                        }
                    }, true);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } finally {
            if (toRegister != null) {
                GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, toRegister);
            }
        }

        return resource2Errors;
    }

    public enum Scope {
        ALL_OPENED_PROJECTS,
        GIVEN_SOURCE_ROOTS,
        ALL_REMOTE_PROJECTS,
        GIVEN_FOLDER;
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
        
        LocalContainer(@NonNull FileObject localFO) {
            this.localFO = localFO;
        }
        
        @Override
        public boolean isLocal() {
            return true;
        }

        @Override
        CharSequence getSourceCode(String relativePath) {
            try {
                FileObject file = localFO.getFileObject(relativePath);
                ByteBuffer bb = ByteBuffer.wrap(file.asBytes());

                return FileEncodingQuery.getEncoding(file).decode(bb);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
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
        private final Iterable<? extends HintDescription> hints;
        private final BulkPattern pattern;

        public Resource(Container container, String relativePath, Iterable<? extends HintDescription> hints, BulkPattern pattern) {
            this.container = container;
            this.relativePath = relativePath;
            this.hints = hints;
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

            for (Entry<String, Collection<TreePath>> e : found.entrySet()) {
                Tree treePattern = Utilities.parseAndAttribute(ci, e.getKey(), null);
                
                for (TreePath tp : e.getValue()) {
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

        private Iterable<ErrorDescription> verifiedSpans;
        private boolean spansComputed;

        synchronized void setVerifiedSpans(Iterable<ErrorDescription> eds) {
            this.verifiedSpans = eds;
            this.spansComputed = true;
        }

        synchronized boolean areSpansComputed() {
            return spansComputed;
        }

        public synchronized Iterable<ErrorDescription> getVerifiedSpans(Collection<? super MessageImpl> problems) {
            if (!spansComputed) {
                BatchSearch.getVerifiedSpans(Collections.singletonList(this), problems);
            }
            
            return verifiedSpans;
        }
    }

}
