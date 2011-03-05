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

import org.netbeans.spi.editor.hints.Severity;
import java.util.Iterator;
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
import java.net.URI;
import java.net.URISyntaxException;
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
import java.util.concurrent.atomic.AtomicReference;
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
import org.netbeans.modules.jackpot30.impl.WebUtilities;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.impl.indexing.CustomIndexerImpl;
import org.netbeans.modules.jackpot30.impl.indexing.FileBasedIndex;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.indexing.RemoteIndex;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.AdditionalQueryConstraints;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import static org.netbeans.modules.jackpot30.impl.WebUtilities.escapeForQuery;

/**
 *
 * @author lahvac
 */
public class BatchSearch {

    private static final Logger LOG = Logger.getLogger(BatchSearch.class.getName());

    public static BatchResult findOccurrences(Iterable<? extends HintDescription> patterns, Scope scope) {
        return findOccurrences(patterns, scope, new ProgressHandleWrapper(null));
    }

    public static BatchResult findOccurrences(final Iterable<? extends HintDescription> patterns, final Scope scope, final ProgressHandleWrapper progress) {
        for (HintDescription pattern : patterns) {
            if (pattern.getTriggerKind() != null || pattern.getTriggerPattern() == null) {
                throw new UnsupportedOperationException();
            }
        }

        MapIndices knownSourceRootsMapper = new MapIndices() {
            private Set<FileObject> KNOWN_SOURCE_ROOTS = new HashSet<FileObject>(GlobalPathRegistry.getDefault().getSourceRoots());
            public IndexEnquirer findIndex(FileObject root, ProgressHandleWrapper progress) {
                progress.startNextPart(1);
                if (KNOWN_SOURCE_ROOTS.contains(root) || scope.forceIndicesUpToDate) {
                    try {
                        return new SimpleIndexIndexEnquirer(root, FileBasedIndex.get(root.getURL()));
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
                            public IndexEnquirer findIndex(FileObject root, ProgressHandleWrapper progress) {
                                return new SimpleIndexIndexEnquirer(root, createOrUpdateIndex(root, new File(scope.indexURL), scope.update, progress));
                            }
                        };
                    } else {
                        mapper = new MapIndices() {
                            public IndexEnquirer findIndex(FileObject root, ProgressHandleWrapper progress) {
                                progress.startNextPart(1);
                                try {
                                    return new SimpleIndexIndexEnquirer(root, Index.createWithRemoteIndex(root.getURL(), scope.indexURL, scope.subIndex));
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
            case ALL_REMOTE:
                todo = new HashSet<FileObject>();

                for (RemoteIndex remoteIndex : RemoteIndex.loadIndices()) {
                    todo.add(FileUtil.toFileObject(FileUtil.normalizeFile(new File(remoteIndex.folder))));
                }

                return findOccurrencesLocal(patterns, new MapIndices() {
                    public IndexEnquirer findIndex(FileObject root, ProgressHandleWrapper progress) {
                        for (RemoteIndex remoteIndex : RemoteIndex.loadIndices()) {
                            if (FileUtil.toFileObject(FileUtil.normalizeFile(new File(remoteIndex.folder))) == root) {
                                return enquirerForRemoteIndex(root, remoteIndex, patterns);
                            }
                        }
                        throw new IllegalStateException();
                    }
                }, todo, progress);
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
    
    private static BatchResult findOccurrencesLocalImpl(final CompilationInfo info, final Iterable<? extends HintDescription> patterns, MapIndices indexMapper, Collection<? extends FileObject> todo, ProgressHandleWrapper progress) {
        final DelayedBulkPattern bulkPattern = new DelayedBulkPattern() {
            private final AtomicReference<BulkPattern> pattern = new AtomicReference<BulkPattern>();
            public BulkPattern get() {
                if (pattern.get() == null) {
                    pattern.set(preparePattern(patterns, info));
                }

                return pattern.get();
            }
        };
        final Map<IndexEnquirer, Collection<? extends Resource>> result = new HashMap<IndexEnquirer, Collection<? extends Resource>>();
        final Collection<MessageImpl> problems = new LinkedList<MessageImpl>();
        ProgressHandleWrapper innerForAll = progress.startNextPartWithEmbedding(ProgressHandleWrapper.prepareParts(2 * todo.size()));
        
        for (final FileObject src : todo) {
            LOG.log(Level.FINE, "Processing: {0}", FileUtil.getFileDisplayName(src));
            
            IndexEnquirer indexEnquirer = indexMapper.findIndex(src, innerForAll);

            if (indexEnquirer == null) {
                indexEnquirer = new FileSystemBasedIndexEnquirer(src);
            }

            Collection<? extends Resource> occurrences = indexEnquirer.findResources(patterns, innerForAll, bulkPattern, problems);

            if (!occurrences.isEmpty()) {
                result.put(indexEnquirer, occurrences);
            }

            innerForAll.tick();
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
        getVerifiedSpans(candidates, progress, callback, false, problems);
    }

    public static void getVerifiedSpans(BatchResult candidates, @NonNull ProgressHandleWrapper progress, final VerifiedSpansCallBack callback, boolean doNotRegisterClassPath, final Collection<? super MessageImpl> problems) {
        int[] parts = new int[candidates.projectId2Resources.size()];
        int   index = 0;

        for (Entry<? extends IndexEnquirer, ? extends Collection<? extends Resource>> e : candidates.projectId2Resources.entrySet()) {
            parts[index++] = e.getValue().size();
        }

        ProgressHandleWrapper inner = progress.startNextPartWithEmbedding(parts);

        for (Entry<? extends IndexEnquirer, ? extends Collection<? extends Resource>> e : candidates.projectId2Resources.entrySet()) {
            inner.startNextPart(e.getValue().size());

            e.getKey().validateResource(e.getValue(), progress, callback, problems);
        }
    }

    private static void getLocalVerifiedSpans(Collection<? extends Resource> resources, @NonNull final ProgressHandleWrapper progress, final VerifiedSpansCallBack callback, boolean doNotRegisterClassPath, final Collection<? super MessageImpl> problems) {
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
        ClassPath[] toRegister = null;

        if (!doNotRegisterClassPath) {
            Set<ClassPath> toRegisterSet = new HashSet<ClassPath>();

            for (ClasspathInfo cpInfo : cp2Files.keySet()) {
                toRegisterSet.add(cpInfo.getClassPath(PathKind.SOURCE));
            }

            toRegister = !toRegisterSet.isEmpty() ? toRegisterSet.toArray(new ClassPath[0]) : null;

            if (toRegister != null) {
                GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, toRegister);
                try {
                    Utilities.waitScanFinished();
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
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

    private static boolean isAttributedIndexWithSpans(RemoteIndex remoteIndex) {
        try {
            URI capabilitiesURI = new URI(remoteIndex.remote.toExternalForm() + "/capabilities");
            String capabilitiesString = WebUtilities.requestStringResponse(capabilitiesURI);

            if (capabilitiesURI == null) return false;

            @SuppressWarnings("unchecked")
            Map<String, Object> capabilities = Pojson.load(HashMap.class, capabilitiesString);

            return capabilities.get("attributed") == Boolean.TRUE; //TODO: should also check "methods contains findWithSpans"
        } catch (URISyntaxException ex) {
            LOG.log(Level.FINE, null, ex);
            return false;
        }
    }

    private static IndexEnquirer enquirerForRemoteIndex(FileObject src, RemoteIndex remoteIndex, Iterable<? extends HintDescription> hints) {
        boolean fullySupported = isAttributedIndexWithSpans(remoteIndex);
        StringBuilder textualRepresentation = new StringBuilder();

        for (HintDescription hd : hints) {
            if (hd.getTriggerPattern().getImports().iterator().hasNext()) {
                fullySupported = false;
            }

            if (!fullySupported) break;

            String hintText = hd.getHintText();

            if (hintText != null) {
                textualRepresentation.append(hintText);
            } else {
                textualRepresentation.append(defaultHintText(hd));
                fullySupported = false;
            }

            textualRepresentation.append("\n");
        }

        if (fullySupported) {
            return new RemoteFullyAttributedIndexEnquirer(src, remoteIndex, textualRepresentation.toString());
        } else {
            try {
                return new SimpleIndexIndexEnquirer(src, Index.createWithRemoteIndex(src.getURL(), remoteIndex.remote.toExternalForm(), remoteIndex.remoteSegment));
            } catch (FileStateInvalidException ex) {
                throw new IllegalStateException(ex);
            }
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
        private final boolean forceIndicesUpToDate;
        private final boolean allRemote;

        private Scope() {
            this(null, null, null, null, true, null, false, false);
        }

        private Scope(ScopeType scopeType, String folder, String indexURL, String subIndex, boolean update, Collection<? extends FileObject> sourceRoots, boolean forceIndicesUpToDate, boolean allRemote) {
            this.scopeType = scopeType;
            this.folder = folder;
            this.indexURL = indexURL;
            this.subIndex = subIndex;
            this.update = update;
            this.sourceRoots = sourceRoots;
            this.forceIndicesUpToDate = forceIndicesUpToDate;
            this.allRemote = allRemote;
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
            return new Scope(ScopeType.ALL_OPENED_PROJECTS, null, null, null, false, null, false, false);
        }

        public static Scope createAllDependentOpenedSourceRoots(FileObject from) {
            return new Scope(ScopeType.ALL_DEPENDENT_OPENED_SOURCE_ROOTS, null, null, null, false, Collections.singletonList(from), false, false);
        }

        public static Scope createGivenFolderNoIndex(String folder) {
            return new Scope(ScopeType.GIVEN_FOLDER, folder, null, null, false, null, false, false);
        }

        public static Scope createGivenFolderLocalIndex(String folder, File indexFolder, boolean update) {
            return new Scope(ScopeType.GIVEN_FOLDER, folder, indexFolder.getAbsolutePath(), null, update, null, false, false);
        }

        public static Scope createGivenFolderRemoteIndex(String folder, String urlIndex, String subIndex) {
            return new Scope(ScopeType.GIVEN_FOLDER, folder, urlIndex, subIndex, false, null, false, false);
        }

        public static Scope createGivenSourceRoots(FileObject... sourceRoots) {
            return createGivenSourceRoots(false, sourceRoots);
        }

        public static Scope createGivenSourceRoots(boolean forceIndicesUpToDate, FileObject... sourceRoots) {
            return new Scope(ScopeType.GIVEN_SOURCE_ROOTS, null, null, null, false, Arrays.asList(sourceRoots), forceIndicesUpToDate, false);
        }

        public static Scope createAllRemote() {
            return new Scope(ScopeType.ALL_REMOTE, null, null, null, false, null, false, true);
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
        GIVEN_FOLDER,
        ALL_REMOTE;
    }

    public static final class BatchResult {
        
        private final Map<? extends IndexEnquirer, ? extends Collection<? extends Resource>> projectId2Resources;
        public final Collection<? extends MessageImpl> problems;
        
        public BatchResult(Map<? extends IndexEnquirer, ? extends Collection<? extends Resource>> projectId2Resources, Collection<? extends MessageImpl> problems) {
            this.projectId2Resources = projectId2Resources;
            this.problems = problems;
        }

        public Collection<? extends Collection<? extends Resource>> getResources() {
            return projectId2Resources.values();
        }

        public Map<FileObject, Collection<? extends Resource>> getResourcesWithRoots() {
            Map<FileObject, Collection<? extends Resource>> result = new HashMap<FileObject, Collection<? extends Resource>>();

            for (Entry<? extends IndexEnquirer, ? extends Collection<? extends Resource>> e : projectId2Resources.entrySet()) {
                result.put(e.getKey().src, e.getValue());
            }

            return result;
        }
    }

    public static final class Resource {
        private final IndexEnquirer indexEnquirer;
        private final String relativePath;
        final Iterable<? extends HintDescription> hints;
        private final BulkPattern pattern;

        public Resource(IndexEnquirer indexEnquirer, String relativePath, Iterable<? extends HintDescription> hints, BulkPattern pattern) {
            this.indexEnquirer = indexEnquirer;
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
            return indexEnquirer.src.getFileObject(relativePath);
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
            try {
                FileObject file = getResolvedFile();
                ByteBuffer bb = ByteBuffer.wrap(file.asBytes());

                return FileEncodingQuery.getEncoding(file).decode(bb);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
        }

        public FileObject getRoot() {
            return indexEnquirer.src;
        }
    }

    private static interface MapIndices {
        public IndexEnquirer findIndex(FileObject root, ProgressHandleWrapper progress);
    }

    private static interface DelayedBulkPattern {
        public BulkPattern get();
    }

    private static abstract class IndexEnquirer {
        final FileObject src;
        public IndexEnquirer(FileObject src) {
            this.src = src;
        }
        public abstract Collection<? extends Resource> findResources(Iterable<? extends HintDescription> hints, ProgressHandleWrapper progress, DelayedBulkPattern bulkPattern, Collection<? super MessageImpl> problems);
        public abstract void validateResource(Collection<? extends Resource> resources, ProgressHandleWrapper progress, VerifiedSpansCallBack callback, Collection<? super MessageImpl> problems);
//        public int[] getEstimatedSpan(Resource r);
    }

    private static abstract class LocalIndexEnquirer extends IndexEnquirer {
        public LocalIndexEnquirer(FileObject src) {
            super(src);
        }
        public void validateResource(Collection<? extends Resource> resources, ProgressHandleWrapper progress, VerifiedSpansCallBack callback, Collection<? super MessageImpl> problems) {
            getLocalVerifiedSpans(resources, progress, callback, false/*XXX*/, problems);
        }
    }

    private static final class FileSystemBasedIndexEnquirer extends LocalIndexEnquirer {
        public FileSystemBasedIndexEnquirer(FileObject src) {
            super(src);
        }
        public Collection<? extends Resource> findResources(final Iterable<? extends HintDescription> hints, ProgressHandleWrapper progress, final DelayedBulkPattern bulkPattern, final Collection<? super MessageImpl> problems) {
            Collection<FileObject> files = new LinkedList<FileObject>();

            final ProgressHandleWrapper innerProgress = progress.startNextPartWithEmbedding(30, 70);

            recursive(src, src, files, innerProgress, 0, null, null);

            LOG.log(Level.FINE, "files: {0}", files);

            innerProgress.startNextPart(files.size());

            final Collection<Resource> result = new ArrayList<Resource>();

            if (!files.isEmpty()) {
                try {
                    long start = System.currentTimeMillis();

                    JavaSource.create(Utilities.createUniversalCPInfo(), files).runUserActionTask(new Task<CompilationController>() {
                        public void run(CompilationController cc) throws Exception {
                            if (cc.toPhase(Phase.PARSED).compareTo(Phase.PARSED) <0) {
                                return ;
                            }

                            try {
                                boolean matches = BulkSearch.getDefault().matches(cc, new TreePath(cc.getCompilationUnit()), bulkPattern.get());

                                if (matches) {
                                    result.add(new Resource(FileSystemBasedIndexEnquirer.this, FileUtil.getRelativePath(src, cc.getFileObject()), hints, bulkPattern.get()));
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
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            return result;
        }

    }

    private static final class SimpleIndexIndexEnquirer extends LocalIndexEnquirer {
        private final Index idx;
        public SimpleIndexIndexEnquirer(FileObject src, Index idx) {
            super(src);
            this.idx = idx;
        }
        public Collection<? extends Resource> findResources(final Iterable<? extends HintDescription> hints, ProgressHandleWrapper progress, final DelayedBulkPattern bulkPattern, final Collection<? super MessageImpl> problems) {
            final Collection<Resource> result = new ArrayList<Resource>();

            progress.startNextPart(1);

            try {
                for (String candidate : idx.findCandidates(bulkPattern.get())) {
                    result.add(new Resource(this, candidate, hints, bulkPattern.get()));
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            return result;
        }

    }

    private static final class RemoteFullyAttributedIndexEnquirer extends IndexEnquirer {
        private final RemoteIndex remoteIndex;
        private final String textualHintRepresentation;
        public RemoteFullyAttributedIndexEnquirer(FileObject src, RemoteIndex remoteIndex, String textualHintRepresentation) {
            super(src);
            assert isAttributedIndexWithSpans(remoteIndex);
            this.remoteIndex = remoteIndex;
            this.textualHintRepresentation = textualHintRepresentation;
        }
        public Collection<? extends Resource> findResources(final Iterable<? extends HintDescription> hints, ProgressHandleWrapper progress, final DelayedBulkPattern bulkPattern, final Collection<? super MessageImpl> problems) {
            final Collection<Resource> result = new ArrayList<Resource>();

            progress.startNextPart(1);

            try {
                URI u = new URI(remoteIndex.remote.toExternalForm() + "/find?path=" + escapeForQuery(remoteIndex.remoteSegment) + "&pattern=" + escapeForQuery(textualHintRepresentation));

                for (String occurrence : new ArrayList<String>(WebUtilities.requestStringArrayResponse(u))) {
                    result.add(new Resource(this, occurrence, hints, bulkPattern.get()));
                }

            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }

            return result;
        }

        @Override
        public void validateResource(Collection<? extends Resource> resources, ProgressHandleWrapper progress, VerifiedSpansCallBack callback, Collection<? super MessageImpl> problems) {
            for (Resource r : resources) {
                try {
                    URI spanURI = new URI(remoteIndex.remote.toExternalForm() + "/findSpans?path=" + escapeForQuery(remoteIndex.remoteSegment) + "&relativePath=" + escapeForQuery(r.relativePath) + "&pattern=" + escapeForQuery(textualHintRepresentation));
                    FileObject fo = r.getResolvedFile();

                    if (fo == null) {
                        callback.cannotVerifySpan(r);
                    } else {
                        List<ErrorDescription> result = new ArrayList<ErrorDescription>();

                        for (int[] span : parseSpans(WebUtilities.requestStringResponse(spanURI))) {
                            result.add(ErrorDescriptionFactory.createErrorDescription(Severity.WARNING, "Occurrence", fo, span[0], span[1]));
                        }

                        callback.spansVerified(null, r, result);
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

    }

    private static Iterable<int[]> parseSpans(String from) {
        if (from.isEmpty()) {
            return Collections.emptyList();
        }
        String[] split = from.split(":");
        List<int[]> result = new LinkedList<int[]>();

        for (int i = 0; i < split.length; i += 2) {
            result.add(new int[] {
                Integer.parseInt(split[i + 0].trim()),
                Integer.parseInt(split[i + 1].trim())
            });
        }

        return result;
    }

    private static String defaultHintText(HintDescription hd) {
        StringBuilder result = new StringBuilder();

        PatternDescription pd = hd.getTriggerPattern();

        if (pd == null) return null;

        if (pd.getImports().iterator().hasNext()) {
            //cannot currently handle patterns with imports:
            return null;
        }

        result.append(pd.getPattern());

        if (!pd.getConstraints().isEmpty()) {
            result.append(" :: ");

            for (Iterator<Entry<String, String>> it = pd.getConstraints().entrySet().iterator(); it.hasNext(); ) {
                Entry<String, String> e = it.next();

                result.append(e.getKey()).append(" instanceof ").append(e.getValue());

                if (it.hasNext()) {
                    result.append(" && ");
                }
            }
        }

        result.append(";;\n");

        return result.toString();
    }
}
