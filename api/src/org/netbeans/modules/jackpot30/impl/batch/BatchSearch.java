/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.batch;

import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.api.queries.VisibilityQuery;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.impl.indexing.CustomIndexerImpl;
import org.netbeans.modules.jackpot30.impl.indexing.FileBasedIndex;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.AdditionalQueryConstraints;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class BatchSearch {

    private static final Logger LOG = Logger.getLogger(BatchSearch.class.getName());

    public static BatchResult findOccurrences(Iterable<? extends HintDescription> patterns, Scope scope) {
        return findOccurrences(patterns, scope, new ProgressHandleWrapper(null));
    }

    public static BatchResult findOccurrences(Iterable<? extends HintDescription> patterns, final Scope scope, final ProgressHandleWrapper progress) {
        for (HintDescription pattern : patterns) {
            if (pattern.getTriggerKind() != null || pattern.getTriggerPattern() == null) {
                throw new UnsupportedOperationException();
            }
        }

        MapIndices knownSourceRootsMapper = new MapIndices() {
            private Set<FileObject> KNOWN_SOURCE_ROOTS = new HashSet<FileObject>(GlobalPathRegistry.getDefault().getSourceRoots());
            public Index findIndex(FileObject root, ProgressHandleWrapper progress) {
                progress.startNextPart(1);
                if (KNOWN_SOURCE_ROOTS.contains(root)) {
                    try {
                        return FileBasedIndex.get(root.getURL());
                    } catch (IOException ex) {
                        //TODO: would log+return null be more appropriate?
                        throw new IllegalStateException(ex);
                    }
                } else {
                    return null;
                }
            }
        };
        Set<FileObject> todo;
        
        switch (scope.scopeType) {
            case ALL_OPENED_PROJECTS:
                todo = new HashSet<FileObject>();

                for (ClassPath source : GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)) {
                    todo.addAll(Arrays.asList(source.getRoots()));
                }

                return findOccurrencesLocal(patterns, knownSourceRootsMapper, todo, progress);
            case ALL_DEPENDENT_OPENED_SOURCE_ROOTS:
                todo = new HashSet<FileObject>();
                try {
                    for (URL dep : SourceUtils.getDependentRoots(scope.sourceRoots.iterator().next().getURL())) {
                        todo.add(URLMapper.findFileObject(dep));
                    }
                } catch (FileStateInvalidException ex) {
                    Exceptions.printStackTrace(ex);
                }
                return findOccurrencesLocal(patterns, knownSourceRootsMapper, todo, progress);
            case GIVEN_SOURCE_ROOTS:
                return findOccurrencesLocal(patterns, knownSourceRootsMapper, scope.sourceRoots, progress);
            case GIVEN_FOLDER:
                todo = Collections.singleton(FileUtil.toFileObject(FileUtil.normalizeFile(new File(scope.folder))));

                MapIndices mapper;

                if (scope.indexURL != null) {
                    if (scope.subIndex == null) {
                        mapper = new MapIndices() {
                            public Index findIndex(FileObject root, ProgressHandleWrapper progress) {
                                return createOrUpdateIndex(root, new File(scope.indexURL), scope.update, progress);
                            }
                        };
                    } else {
                        mapper = new MapIndices() {
                            public Index findIndex(FileObject root, ProgressHandleWrapper progress) {
                                progress.startNextPart(1);
                                try {
                                    return Index.createWithRemoteIndex(root.getURL(), scope.indexURL, scope.subIndex);
                                } catch (FileStateInvalidException ex) {
                                    Exceptions.printStackTrace(ex);
                                    return null;
                                }
                            }
                        };
                    }
                } else {
                    mapper = knownSourceRootsMapper;
                }

                return findOccurrencesLocal(patterns, mapper, todo, progress);
            default:
                throw new UnsupportedOperationException(scope.scopeType.name());
        }
    }

    private static BatchResult findOccurrencesLocal(final Iterable<? extends HintDescription> patterns, final MapIndices indexMapper, final Collection<? extends FileObject> todo, final ProgressHandleWrapper progress) {
        final BatchResult[] result = new BatchResult[1];

        try {
            JavaSource.create(Utilities.createUniversalCPInfo()).runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController parameter) throws Exception {
                    result[0] = findOccurrencesLocalImpl(parameter, patterns, indexMapper, todo, progress);
                }
            }, true);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        return result[0];
    }
    
    private static BatchResult findOccurrencesLocalImpl(CompilationInfo info, final Iterable<? extends HintDescription> patterns, MapIndices indexMapper, Collection<? extends FileObject> todo, ProgressHandleWrapper progress) {
        final BulkPattern bulkPattern = preparePattern(patterns, info);
        final Map<Container, Collection<Resource>> result = new HashMap<Container, Collection<Resource>>();
        final Collection<MessageImpl> problems = new LinkedList<MessageImpl>();
        ProgressHandleWrapper innerForAll = progress.startNextPartWithEmbedding(ProgressHandleWrapper.prepareParts(2 * todo.size()));
        
        for (final FileObject src : todo) {
            LOG.log(Level.FINE, "Processing: {0}", FileUtil.getFileDisplayName(src));
            
            try {
                Index i = indexMapper.findIndex(src, innerForAll);

                if (i != null) {
                    innerForAll.startNextPart(1);
                    
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

                    final ProgressHandleWrapper innerProgress = innerForAll.startNextPartWithEmbedding(30, 70);
                    
                    recursive(src, src, files, innerProgress, 0, null, null);

                    LOG.log(Level.FINE, "files: {0}", files);

                    innerProgress.startNextPart(files.size());

                    if (!files.isEmpty()) {
                        long start = System.currentTimeMillis();

                        JavaSource.create(Utilities.createUniversalCPInfo(), files).runUserActionTask(new Task<CompilationController>() {
                            public void run(CompilationController cc) throws Exception {
                                if (cc.toPhase(Phase.PARSED).compareTo(Phase.PARSED) <0) {
                                    return ;
                                }

                                try {
                                    boolean matches = BulkSearch.getDefault().matches(cc, new TreePath(cc.getCompilationUnit()), bulkPattern);

                                    if (matches) {
                                        Collection<Resource> resources = result.get(id);

                                        if (resources == null) {
                                            result.put(id, resources = new LinkedList<Resource>());
                                        }

                                        resources.add(new Resource(id, FileUtil.getRelativePath(src, cc.getFileObject()), patterns, bulkPattern));
                                    }
                                } catch (ThreadDeath td) {
                                    throw td;
                                } catch (Throwable t) {
                                    LOG.log(Level.INFO, "Exception while performing batch search in " + FileUtil.getFileDisplayName(cc.getFileObject()), t);
                                    problems.add(new MessageImpl(MessageKind.WARNING, "An exception occurred while testing file: " + FileUtil.getFileDisplayName(cc.getFileObject()) + " (" + t.getLocalizedMessage() + ")."));
                                }

                                innerProgress.tick();
                            }
                        }, true);

                        long end = System.currentTimeMillis();

                        LOG.log(Level.FINE, "took: {0}, per file: {1}", new Object[]{end - start, (end - start) / files.size()});
                    }
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            progress.tick();
        }

        return new BatchResult(result, problems);
    }

    private static BulkPattern preparePattern(final Iterable<? extends HintDescription> patterns, CompilationInfo info) {
        Collection<String> code = new LinkedList<String>();
        Collection<Tree> trees = new LinkedList<Tree>();
        Collection<AdditionalQueryConstraints> additionalConstraints = new LinkedList<AdditionalQueryConstraints>();

        for (HintDescription pattern : patterns) {
            String textPattern = pattern.getTriggerPattern().getPattern();

            code.add(textPattern);
            trees.add(Utilities.parseAndAttribute(info, textPattern, null));
            additionalConstraints.add(pattern.getAdditionalConstraints());
        }

        return BulkSearch.getDefault().create(code, trees, additionalConstraints);
    }

    private static void recursive(FileObject root, FileObject file, Collection<FileObject> collected, ProgressHandleWrapper progress, int depth, Properties timeStamps, Set<String> removedFiles) {
        if (!VisibilityQuery.getDefault().isVisible(file)) return;

        if (file.isData()) {
            if (timeStamps != null) {
                String relativePath = FileUtil.getRelativePath(root, file);
                String lastModified = Long.toHexString(file.lastModified().getTime());

                removedFiles.remove(relativePath);

                if (lastModified.equals(timeStamps.getProperty(relativePath))) {
                    return;
                }

                timeStamps.setProperty(relativePath, lastModified);
            }

            if (/*???:*/"java".equals(file.getExt()) || "text/x-java".equals(FileUtil.getMIMEType(file, "text/x-java"))) {
                collected.add(file);
            }
        } else {
            FileObject[] children = file.getChildren();

            if (children.length == 0) return;

            ProgressHandleWrapper inner = depth < 2 ? progress.startNextPartWithEmbedding(ProgressHandleWrapper.prepareParts(children.length)) : null;

            if (inner == null && progress != null) {
                progress.startNextPart(children.length);
            } else {
                progress = null;
            }

            for (FileObject c : children) {
                recursive(root, c, collected, inner, depth + 1, timeStamps, removedFiles);

                if (progress != null) progress.tick();
            }
        }
    }

    private static Index createOrUpdateIndex(FileObject src, File indexRoot, boolean update, ProgressHandleWrapper progress) {
        Index index;

        try {
            index = FileBasedIndex.create(src.getURL(), indexRoot);
        } catch (FileStateInvalidException ex) {
            throw new IllegalStateException(ex);
        }

        File timeStampsFile = new File(indexRoot, "timestamps.properties");
        Properties timeStamps = new Properties();

        if (timeStampsFile.exists()) {
            if (!update) {
                progress.startNextPart(1);
                return index;
            }

            InputStream in = null;

            try {
                in = new BufferedInputStream(new FileInputStream(timeStampsFile));
                timeStamps.load(in);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    if (in != null)
                        in.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        Collection<FileObject> collected = new LinkedList<FileObject>();
        Set<String> removed = new HashSet<String>(timeStamps.stringPropertyNames());

        recursive(src, src, collected, progress, 0, timeStamps, removed);

        CustomIndexerImpl.doIndex(src, collected, removed, index);

        OutputStream out = null;

        try {
            out = new BufferedOutputStream(new FileOutputStream(timeStampsFile));
            timeStamps.store(out, null);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return index;
    }
    
    public static void getVerifiedSpans(BatchResult candidates, @NonNull ProgressHandleWrapper progress, final VerifiedSpansCallBack callback, final Collection<? super MessageImpl> problems) {
        int[] parts = new int[candidates.projectId2Resources.size()];
        int   index = 0;

        for (Entry<? extends Container, ? extends Collection<? extends Resource>> e : candidates.projectId2Resources.entrySet()) {
            parts[index++] = e.getValue().size();
        }

        ProgressHandleWrapper inner = progress.startNextPartWithEmbedding(parts);

        for (Collection<? extends Resource> it :candidates.projectId2Resources.values()) {
            inner.startNextPart(it.size());

            getVerifiedSpans(it, inner, callback, problems);
        }
    }

    private static void getVerifiedSpans(Collection<? extends Resource> resources, @NonNull final ProgressHandleWrapper progress, final VerifiedSpansCallBack callback, final Collection<? super MessageImpl> problems) {
        Collection<FileObject> files = new LinkedList<FileObject>();
        final Map<FileObject, Resource> file2Resource = new HashMap<FileObject, Resource>();

        for (Resource r : resources) {
            FileObject file = r.getResolvedFile();

            if (file != null) {
                files.add(file);
                file2Resource.put(file, r);
            } else {
                callback.cannotVerifySpan(r);
                progress.tick();
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
                    List<FileObject> toProcess = new ArrayList<FileObject>(e.getValue());
                    final AtomicInteger currentPointer = new AtomicInteger();
                    callback.groupStarted();

//                    for (FileObject f : toProcess) {
                    while (currentPointer.get() < toProcess.size()) {
                        final AtomicBoolean stop = new AtomicBoolean();
//                        JavaSource js = JavaSource.create(e.getKey(), f);
                        JavaSource js = JavaSource.create(e.getKey(), toProcess.subList(currentPointer.get(), toProcess.size()));

                        js.runUserActionTask(new Task<CompilationController>() {
                            public void run(CompilationController parameter) throws Exception {
                                if (stop.get()) return;

                                //workaround for #192481:
                                if (parameter.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0)
                                    return ;

                                boolean cont = true;

                                try {
                                    Context ctx = JavaSourceAccessor.getINSTANCE().getJavacTask(parameter).getContext();
                                    ClassReader reader = ClassReader.instance(ctx);
                                    Field attributeReaders = ClassReader.class.getDeclaredField("attributeReaders");

                                    attributeReaders.setAccessible(true);
                                    ((Map) attributeReaders.get(reader)).remove(Names.instance(ctx)._org_netbeans_ParameterNames);
                                    //workaround for #192481 end

                                    if (parameter.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                                        return ;

                                    progress.setMessage("processing: " + FileUtil.getFileDisplayName(parameter.getFileObject()));
                                    Resource r = file2Resource.get(parameter.getFileObject());
                                    Map<PatternDescription, List<HintDescription>> sortedHintsPatterns = new HashMap<PatternDescription, List<HintDescription>>();
                                    Map<Kind, List<HintDescription>> sortedHintsKinds = new HashMap<Kind, List<HintDescription>>();

                                    RulesManager.sortOut(r.hints, sortedHintsKinds, sortedHintsPatterns);

                                    List<ErrorDescription> hints = new HintsInvoker(parameter, new AtomicBoolean()).computeHints(parameter, sortedHintsKinds, sortedHintsPatterns, problems);

                                    cont = callback.spansVerified(parameter, r, hints);
                                } catch (ThreadDeath td) {
                                    throw td;
                                } catch (Throwable t) {
                                    LOG.log(Level.INFO, "Exception while performing batch processing in " + FileUtil.getFileDisplayName(parameter.getFileObject()), t);
                                    problems.add(new MessageImpl(MessageKind.WARNING, "An exception occurred while processing file: " + FileUtil.getFileDisplayName(parameter.getFileObject()) + " (" + t.getLocalizedMessage() + ")."));
                                }
                                
                                if (cont) {
                                    progress.tick();
                                    currentPointer.incrementAndGet();
                                } else {
                                    stop.set(true);
                                }
                            }
                        }, true);
                    }

                    callback.groupFinished();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } finally {
            if (toRegister != null) {
                GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, toRegister);
            }
            progress.finish();
        }
    }

    public interface VerifiedSpansCallBack {
        public void groupStarted();
        public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception;
        public void groupFinished();
        public void cannotVerifySpan(Resource r);
    }

    public final static class Scope {
        public  final ScopeType scopeType;
        public  final String folder; //public only for AddScopePanel
        public  final String indexURL; //public only for AddScopePanel
        public  final String subIndex; //public only for AddScopePanel
        public  final boolean update; //public only for AddScopePanel
        private final Collection<? extends FileObject> sourceRoots;

        private Scope() {
            this(null, null, null, null, true, null);
        }

        private Scope(ScopeType scopeType, String folder, String indexURL, String subIndex, boolean update, Collection<? extends FileObject> sourceRoots) {
            this.scopeType = scopeType;
            this.folder = folder;
            this.indexURL = indexURL;
            this.subIndex = subIndex;
            this.update = update;
            this.sourceRoots = sourceRoots;
        }

        public String serialize() {
            return Pojson.save(this);
            //sourceRoots currently never needs to be serialized:
//            return scopeType.name() + "\n" + (folder != null ? folder.getAbsolutePath() : "") + "\n" + indexURL + "\n" + subIndex + "\n" + update;
        }

        public static Scope deserialize(String serialized) {
//            String[] parts = serialized.split("\n");
//
//            return new Scope(ScopeType.valueOf(parts[0]), new File(parts[1]), parts[2], parts[3], Boolean.valueOf(parts[4]), null);
            return Pojson.load(Scope.class, serialized);
        }

        public static Scope createAllOpenedProjectsScope() {
            return new Scope(ScopeType.ALL_OPENED_PROJECTS, null, null, null, false, null);
        }

        public static Scope createAllDependentOpenedSourceRoots(FileObject from) {
            return new Scope(ScopeType.ALL_DEPENDENT_OPENED_SOURCE_ROOTS, null, null, null, false, Collections.singletonList(from));
        }

        public static Scope createGivenFolderNoIndex(String folder) {
            return new Scope(ScopeType.GIVEN_FOLDER, folder, null, null, false, null);
        }

        public static Scope createGivenFolderLocalIndex(String folder, File indexFolder, boolean update) {
            return new Scope(ScopeType.GIVEN_FOLDER, folder, indexFolder.getAbsolutePath(), null, update, null);
        }

        public static Scope createGivenFolderRemoteIndex(String folder, String urlIndex, String subIndex) {
            return new Scope(ScopeType.GIVEN_FOLDER, folder, urlIndex, subIndex, false, null);
        }

        public static Scope createGivenSourceRoots(FileObject... sourceRoots) {
            return new Scope(ScopeType.GIVEN_SOURCE_ROOTS, null, null, null, false, Arrays.asList(sourceRoots));
        }

        public String getDisplayName() {
            switch (scopeType) {
                case ALL_OPENED_PROJECTS: return "All Opened Projects";
                case GIVEN_FOLDER: return folder;
                default: throw new IllegalStateException();
            }
        }
    }
    
    public enum ScopeType {
        ALL_OPENED_PROJECTS,
        ALL_DEPENDENT_OPENED_SOURCE_ROOTS,
        GIVEN_SOURCE_ROOTS,
        GIVEN_FOLDER;
    }

    public static final class BatchResult {
        
        public final Map<? extends Container, ? extends Collection<? extends Resource>> projectId2Resources;
        public final Collection<? extends MessageImpl> problems;
        
        public BatchResult(Map<? extends Container, ? extends Collection<? extends Resource>> projectId2Resources, Collection<? extends MessageImpl> problems) {
            this.projectId2Resources = projectId2Resources;
            this.problems = problems;
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
        final Iterable<? extends HintDescription> hints;
        private final BulkPattern pattern;

        public Resource(Container container, String relativePath, Iterable<? extends HintDescription> hints, BulkPattern pattern) {
            this.container = container;
            this.relativePath = relativePath;
            this.hints = hints;
            this.pattern = pattern;
        }

        public Container getContainer() {
            return container;
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
            Map<String, Collection<TreePath>> found = BulkSearch.getDefault().match(ci, new TreePath(ci.getCompilationUnit()), pattern);
            
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
    }

    private static interface MapIndices {
        public Index findIndex(FileObject root, ProgressHandleWrapper progress);
    }

}
