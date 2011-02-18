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

package org.netbeans.modules.jackpot30.backend.impl.api;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndex.NameKind;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.modules.jackpot30.backend.impl.CategoryStorage;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.impl.batch.BatchUtilities;
import org.netbeans.modules.jackpot30.impl.batch.ProgressHandleWrapper;
import org.netbeans.modules.jackpot30.impl.examples.Example;
import org.netbeans.modules.jackpot30.impl.examples.Example.Option;
import org.netbeans.modules.jackpot30.impl.examples.LoadExamples;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.indexing.FileBasedIndex;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.PatternConvertor;
import org.netbeans.modules.java.source.usages.ClassIndexManager;
import org.netbeans.modules.jumpto.type.GoToTypeAction;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
@Path("/index")
public class API {

    @GET
    @Path("/find")
    @Produces("text/plain")
    //TODO: parameter for "verified"?
    public String find(@QueryParam("path") String segment, @QueryParam("pattern") String pattern, @QueryParam("asynchronous") @DefaultValue(value="false") boolean asynchronous) throws IOException {
        assert !asynchronous;

        Iterable<? extends HintDescription> hints = PatternConvertor.create(pattern);
        Set<FileObject> srcRoots = CategoryStorage.getCategoryContent(segment);
        final FileObject deepestCommonParent = deepestCommonParent(srcRoots);
        BatchResult batchResult = BatchSearch.findOccurrences(hints, Scope.createGivenSourceRoots(true, srcRoots.toArray(new FileObject[0])));
        final StringBuilder result = new StringBuilder();

        BatchSearch.getVerifiedSpans(batchResult, new ProgressHandleWrapper(1), new BatchSearch.VerifiedSpansCallBack() {
            @Override public void groupStarted() {}
            @Override public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                if (!hints.isEmpty()) {
                    result.append(FileUtil.getRelativePath(deepestCommonParent, r.getResolvedFile()));
                    result.append("\n");
                }
                System.err.println("processed file=" + r.getRelativePath());
                return true;
            }
            @Override public void groupFinished() {}
            @Override public void cannotVerifySpan(Resource r) { /*TODO: warn user?*/ }
        }, true, new LinkedList<MessageImpl>()); //TODO: show the messages to the user?

        return result.toString();
    }

//    @GET
//    @Path("/findCategorize")
//    @Produces("text/plain")
//    public String findCategorize(@QueryParam("path") final String segment, @QueryParam("pattern") final String pattern, @QueryParam("asynchronous") @DefaultValue(value="false") boolean asynchronous) throws IOException {
//        assert asynchronous;
//
//        long id = this.id.getAndIncrement();
//        final ProgressImpl progress = new ProgressImpl();
//
//        workInProgress.put(id, progress);
//
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    File sourceRoot = Cache.sourceRootForKey(segment);
//                    Index idx = FileBasedIndex.get(sourceRoot.toURI().toURL());
//                    new SortedQuery().query(idx, pattern, progress);
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                    progress.finish();
//                }
//            }
//        }).start();
//
//        return Long.toString(id);
//    }

    @GET
    @Path("/findSpans")
    @Produces("text/plain")
    public String findSpans(@QueryParam("path") String segment, @QueryParam("relativePath") String relativePath, @QueryParam("pattern") String pattern) throws IOException {
        Iterable<? extends HintDescription> hints = PatternConvertor.create(pattern);
        Set<FileObject> srcRoots = CategoryStorage.getCategoryContent(segment);
        FileObject deepestCommonParent = deepestCommonParent(srcRoots);
        BatchResult batchResult = BatchSearch.findOccurrences(hints, Scope.createGivenSourceRoots(deepestCommonParent.getFileObject(relativePath)));
        final StringBuilder result = new StringBuilder();

        BatchSearch.getVerifiedSpans(batchResult, new ProgressHandleWrapper(1), new BatchSearch.VerifiedSpansCallBack() {
            @Override public void groupStarted() {}
            @Override public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                for (ErrorDescription ed : hints) {
                    result.append(ed.getRange().getBegin().getOffset());
                    result.append(":");
                    result.append(ed.getRange().getEnd().getOffset());
                    result.append(":");
                }
                return true;
            }
            @Override public void groupFinished() {}
            @Override public void cannotVerifySpan(Resource r) { /*TODO: warn user?*/ }
        }, true, new LinkedList<MessageImpl>()); //TODO: show the messages to the user?

        if (result.length() > 0) {
            result.delete(result.length() - 1, result.length());
        }
        return result.toString();
    }

    @GET
    @Path("/list")
    @Produces("text/plain")
    public String list() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (Entry<String, String> e : CategoryStorage.listCategoriesWithNames().entrySet()) {
            sb.append(e.getKey());
            sb.append(":");
            sb.append(e.getValue());
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/cat")
    @Produces("text/plain")
    public String cat(@QueryParam("path") String segment, @QueryParam("relative") String relative) throws IOException {
        Set<FileObject> srcRoots = CategoryStorage.getCategoryContent(segment);
        FileObject deepestCommonParent = deepestCommonParent(srcRoots);
        FileObject file = deepestCommonParent.getFileObject(relative);
        ClassPath cp = ClassPathSupport.createClassPath(srcRoots.toArray(new FileObject[0]));
        FileObject root = cp.findOwnerRoot(file);
        String path = cp.getResourceName(file);
        Index index = FileBasedIndex.get(root.getURL());

        if (index == null) {
            throw new IOException("No index");
        }

        CharSequence source = index.getSourceCode(path);

        if (source == null) {
            throw new IOException("Source code not found");
        }
        
        return source.toString().replaceAll("\r\n", "\n");
    }

    @GET
    @Path("/apply")
    @Produces("text/diff")
    //TODO: parameter for "verified"?
    public StreamingOutput apply(@QueryParam("path") String segment, @QueryParam("pattern") String pattern, @QueryParam("asynchronous") @DefaultValue(value="false") boolean asynchronous) throws IOException {
        assert !asynchronous;

        Iterable<? extends HintDescription> hints = PatternConvertor.create(pattern);
        Set<FileObject> srcRoots = CategoryStorage.getCategoryContent(segment);
        final FileObject deepestCommonParent = deepestCommonParent(srcRoots);
        BatchResult batchResult = BatchSearch.findOccurrences(hints, Scope.createGivenSourceRoots(srcRoots.toArray(new FileObject[0])));
        final Collection<ModificationResult> modifications = BatchUtilities.applyFixes(batchResult, new ProgressHandleWrapper(1), new AtomicBoolean(), new LinkedList<MessageImpl>());

        return new StreamingOutput() {
            @Override public void write(OutputStream output) throws IOException, WebApplicationException {
                Writer w = new OutputStreamWriter(output, "UTF-8");

                try {
                    for (ModificationResult modResult : modifications) {
                        BatchUtilities.exportDiff(modResult, deepestCommonParent, w);
                    }
                } finally {
                    try {
                        w.close();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        };
    }

//    @GET
//    @Path("/info")
//    @Produces("text/plain")
//    public String info(@QueryParam("path") String segment) throws IOException {
//        URL sourceRoot = Cache.sourceRootForKey(segment).toURI().toURL();
//        Index index = FileBasedIndex.get(sourceRoot);
//
//        if (index == null) {
//            throw new IOException("No index");
//        }
//
//        return Pojson.save(index.getIndexInfo());
//    }

    @GET
    @Path("/examples")
    @Produces("text/plain")
    public String examples() throws IOException {
        List<Map<String, String>> examples = new LinkedList<Map<String, String>>();

        for (Example ex : LoadExamples.loadExamples()) {
            if (ex.getOptions().contains(Option.VERIFY) || ex.getOptions().contains(Option.FIX)) continue;
            Map<String, String> desc = new HashMap<String, String>();

            desc.put("displayName", ex.getDisplayName());
            desc.put("pattern", ex.getCode());

            examples.add(desc);
        }

        return Pojson.save(examples);
    }

    @GET
    @Path("/errors")
    @Produces("text/plain")
    public String errors(@QueryParam("pattern") String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (Diagnostic<? extends JavaFileObject> d : StandaloneFinder.parseAndReportErrors(pattern)) {
            sb.append(d.getMessage(null));
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/findDuplicates")
    @Produces("text/plain")
    public String findDuplicates(@QueryParam("path") String segment, @QueryParam("hashes") String hashes) throws IOException {
        Map<String, Map<String, Collection<? extends String>>> hash2Segment2Contains = new HashMap<String, Map<String, Collection<? extends String>>>();
        Collection<String> segments = new LinkedList<String>();

        if (segment != null) segments.add(segment);
        else {
            for (String key : Cache.knownSourceRoots()) {
                segments.add(key);
            }
        }

        Iterable<? extends String> hashesList = Arrays.asList(Pojson.load(String[].class, hashes));

        for (String key : segments) {
            Map<String, Collection<? extends String>> found = StandaloneFinder.containsHash(Cache.sourceRootForKey(key), hashesList);

            for (Entry<String, Collection<? extends String>> e : found.entrySet()) {
                Map<String, Collection<? extends String>> perRoot = hash2Segment2Contains.get(e.getKey());

                if (perRoot == null) {
                    hash2Segment2Contains.put(e.getKey(), perRoot = new HashMap<String, Collection<? extends String>>());
                }

                perRoot.put(key, e.getValue());
            }
        }

        return Pojson.save(hash2Segment2Contains);
    }

    @GET
    @Path("/checkProgress")
    @Produces("text/plain")
    public String checkProgress(@QueryParam("id") long id) throws IOException {
        ProgressImpl progress = workInProgress.get(id);
        Map<String, Object> result;

        if (progress == null) {
            result = new HashMap<String, Object>();
            result.put("total", 0);
            result.put("workDone", 0);
            result.put("finished", true);
            result.put("result", Collections.<String>emptyList());
        } else {
            result = progress.progressPacket();
        }

        if (result.get("finished") == Boolean.TRUE) {
            workInProgress.remove(id);
        }

        return Pojson.save(result);
    }

    @GET
    @Path("/cancelProgress")
    @Produces("text/plain")
    public String cancelProgress(@QueryParam("id") long id) throws IOException {
        ProgressImpl progress = workInProgress.get(id);

        if (progress != null) {
            progress.cancel.set(true);
        }

        return "done";
    }

    @GET
    @Path("/capabilities")
    @Produces("text/plain")
    public String capabilities() throws IOException {
        Map<String, Object> result = new HashMap<String, Object>();
        List<String> methods = new ArrayList<String>(API.class.getDeclaredMethods().length);

        for (Method m : API.class.getDeclaredMethods()) {
            if (m.isAnnotationPresent(GET.class) && (m.getModifiers() & Modifier.PUBLIC) != 0) {
                methods.add(m.getName());
            }
        }

        result.put("methods", methods);
        result.put("attributed", true);

        return Pojson.save(result);
    }

    @GET
    @Path("/findType")
    @Produces("text/plain")
    public String findType(@QueryParam("path") String segment, @QueryParam("prefix") String prefix, @QueryParam("casesensitive") @DefaultValue("false") boolean casesensitive, @QueryParam("asynchronous") @DefaultValue(value="false") boolean asynchronous) throws IOException {
        assert !asynchronous;

        //copied (and converted to NameKind) from jumpto's GoToTypeAction:
        boolean exact = prefix.endsWith(" "); // NOI18N

        prefix = prefix.trim();

        if ( prefix.length() == 0) {
            return "";
        }

        NameKind nameKind;
        int wildcard = GoToTypeAction.containsWildCard(prefix);

        if (exact) {
            //nameKind = panel.isCaseSensitive() ? SearchType.EXACT_NAME : SearchType.CASE_INSENSITIVE_EXACT_NAME;
            nameKind = NameKind.SIMPLE_NAME;
        }
        else if ((GoToTypeAction.isAllUpper(prefix) && prefix.length() > 1) || GoToTypeAction.isCamelCase(prefix)) {
            nameKind = NameKind.CAMEL_CASE;
        }
        else if (wildcard != -1) {
            nameKind = casesensitive ? NameKind.REGEXP : NameKind.CASE_INSENSITIVE_REGEXP;
        }
        else {
            nameKind = casesensitive ? NameKind.PREFIX : NameKind.CASE_INSENSITIVE_PREFIX;
        }

        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        Set<FileObject> srcRoots = CategoryStorage.getCategoryContent(segment);
        FileObject deepestCommonParent = deepestCommonParent(srcRoots);

        for (FileObject srcRoot : srcRoots) {
            String rootId = FileUtil.getRelativePath(deepestCommonParent, srcRoot);
            List<String> currentResult = new ArrayList<String>();

            result.put(rootId, currentResult);

            ClassIndexManager.getDefault().createUsagesQuery(srcRoot.getURL(), true);
            ClasspathInfo cpInfo = ClasspathInfo.create(ClassPath.EMPTY, ClassPath.EMPTY, ClassPathSupport.createClassPath(srcRoot));
            Set<ElementHandle<TypeElement>> names = new HashSet<ElementHandle<TypeElement>>(cpInfo.getClassIndex().getDeclaredTypes(prefix, nameKind, EnumSet.of(SearchScope.SOURCE)));

            if (nameKind == NameKind.CAMEL_CASE) {
                names.addAll(cpInfo.getClassIndex().getDeclaredTypes(prefix, NameKind.CASE_INSENSITIVE_PREFIX, EnumSet.of(SearchScope.SOURCE)));
            }

            for (ElementHandle<TypeElement> d : names) {
                currentResult.add(d.getBinaryName());
            }
        }

        return Pojson.save(result);
    }

    //XXX: not really correct, a base directory(-ies?) should be set in the category!
    private static FileObject deepestCommonParent(Set<FileObject> roots) {
        FileObject result = null;

        for (FileObject r : roots) {
            if (result == null) {
                result = r;
            } else {
                while (!FileUtil.isParentOf(result, r)) {
                    result = result.getParent();
                }
            }
        }

        return result;
    }

    private final static AtomicLong id = new AtomicLong();
    private final static Map<Long, ProgressImpl> workInProgress = new HashMap<Long, ProgressImpl>();//XXX: should be cleared eventually even if not read

    private static final class ProgressImpl implements Progress {
        private int total = -1;
        private int workDone = 0;
        private boolean finished;
        private final List<String> result = new ArrayList<String>();
        private final AtomicBoolean cancel = new AtomicBoolean();
        public synchronized void setTotalWork(int total) {
            this.total = total;
        }
        public synchronized void progress(int totalDone) {
            workDone = totalDone;
        }
        public synchronized void updateProgress(int updateDone) {
            workDone += updateDone;
        }
        public synchronized void addResultPart(String part) {
            result.add(part);
        }
        public synchronized void finish() {
            finished = true;
        }
        public synchronized boolean isCancelled() {
            return cancel.get();
        }
        synchronized Map<String, Object> progressPacket() {
            Map<String, Object> result = new HashMap<String, Object>();

            result.put("total", total);
            result.put("workDone", workDone);
            result.put("finished", finished);
            result.put("result", new ArrayList<String>(this.result));

            this.result.clear();

            return result;
        }
    }

}
