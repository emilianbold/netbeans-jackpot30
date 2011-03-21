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
package org.netbeans.modules.jackpot30.impl.duplicates;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.tools.JavaCompiler.CompilationTask;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesCustomIndexerImpl;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesIndex;
import org.netbeans.modules.jackpot30.impl.indexing.AbstractLuceneIndex.BitSetCollector;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.EncodingContext;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;


/**
 *
 * @author lahvac
 */
public class ComputeDuplicates {

    public Iterator<? extends DuplicateDescription> computeDuplicatesForAllOpenedProjects(ProgressHandle progress, AtomicBoolean cancel) throws IOException {
        Set<URL> urls = new HashSet<URL>();

        for (ClassPath cp : GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)) {
            for (ClassPath.Entry e : cp.entries()) {
                urls.add(e.getURL());
            }
        }

        long start = System.currentTimeMillis();
        try {
            return computeDuplicates(urls, progress, cancel);
        } finally {
            System.err.println("duplicates for all open projects: " + (System.currentTimeMillis() - start));
        }
    }

    public Iterator<? extends DuplicateDescription> computeDuplicates(Set<URL> forURLs, ProgressHandle progress, AtomicBoolean cancel) throws IOException {
        Map<IndexReader, FileObject> readers2Roots = new LinkedHashMap<IndexReader, FileObject>();

        progress.progress("Updating indices");

        for (URL u : forURLs) {
            try {
                //TODO: needs to be removed for server mode
                new DuplicatesCustomIndexerImpl.FactoryImpl().updateIndex(u, cancel); //TODO: show updating progress to the user
                
                File cacheRoot = Cache.findCache(DuplicatesIndex.NAME, DuplicatesIndex.VERSION).findCacheRoot(u);

                File dir = new File(cacheRoot, "fulltext");

                if (dir.listFiles() != null && dir.listFiles().length > 0) {
                    IndexReader reader = IndexReader.open(FSDirectory.open(dir), true);

                    readers2Roots.put(reader, URLMapper.findFileObject(u));
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        progress.progress("Searching for duplicates");

        MultiReader r = new MultiReader(readers2Roots.keySet().toArray(new IndexReader[0]));

        List<String> dd = new ArrayList<String>(getDuplicatedValues(r, "generalized", cancel));

        sortHashes(dd);

        //TODO: only show valuable duplicates?:
//        dd = dd.subList(0, dd.size() / 10 + 1);

        return new DuplicatesIterator(readers2Roots, dd, 2);
    }

    public static Iterator<? extends DuplicateDescription> XXXduplicatesOf(Map<IndexReader, FileObject> readers2Roots, Collection<String> hashes) {
        List<String> hashesList = new ArrayList<String>(hashes);
        sortHashes(hashesList);
        return new DuplicatesIterator(readers2Roots, hashesList, 1);
    }

    private static final class DuplicatesIterator implements Iterator<DuplicateDescription> {
        private final Map<IndexReader, FileObject> readers2Roots;
        private final Iterator<String> duplicateCandidates;
        private final int minDuplicates;
        private final List<DuplicateDescription> result = new LinkedList<DuplicateDescription>();

        public DuplicatesIterator(Map<IndexReader, FileObject> readers2Roots, Iterable<String> duplicateCandidates, int minDuplicates) {
            this.readers2Roots = readers2Roots;
            this.duplicateCandidates = duplicateCandidates.iterator();
            this.minDuplicates = minDuplicates;
        }

        private DuplicateDescription nextDescription() throws IOException {
        while (duplicateCandidates.hasNext()) {
            String longest = duplicateCandidates.next();
            List<Span> foundDuplicates = new LinkedList<Span>();

            Query query = new TermQuery(new Term("generalized", longest));

            for (Entry<IndexReader, FileObject> e : readers2Roots.entrySet()) {
                Searcher s = new IndexSearcher(e.getKey());
                BitSet matchingDocuments = new BitSet(e.getKey().maxDoc());
                Collector c = new BitSetCollector(matchingDocuments);

                s.search(query, c);

                for (int docNum = matchingDocuments.nextSetBit(0); docNum >= 0; docNum = matchingDocuments.nextSetBit(docNum + 1)) {
                    final Document doc = e.getKey().document(docNum);
                    int pos = Arrays.binarySearch(doc.getValues("generalized"), longest);

                    if (pos < 0) {
                        System.err.println("FOOBAR=" + pos);
                        continue;
                    }
                    
                    String spanSpec = doc.getValues("positions")[pos];
                    String relPath = doc.getField("path").stringValue();

                    for (String spanPart : spanSpec.split(";")) {
                        Span span = Span.of(e.getValue().getFileObject(relPath), spanPart);

                        if (span != null) {
                            foundDuplicates.add(span);
                        }
                    }
                }
            }

            if (foundDuplicates.size() >= minDuplicates) {
                DuplicateDescription current = DuplicateDescription.of(foundDuplicates, getValue(longest), longest);
                boolean add = true;

                for (Iterator<DuplicateDescription> it = result.iterator(); it.hasNext();) {
                    DuplicateDescription existing = it.next();

                    if (subsumes(existing, current)) {
                        add = false;
                        break;
                    }

                    if (subsumes(current, existing)) {
                        //can happen? (note that the duplicates are sorted by value)
                        it.remove();
                    }
                }

                if (add) {
                    result.add(current);
                    return current;
                }
            }

        }
        return null;
        }

        private DuplicateDescription next;

        public boolean hasNext() {
            if (next == null) {
                try {
                    next = nextDescription();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            return next != null;
        }

        public DuplicateDescription next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            DuplicateDescription r = next;

            next = null;
            return r;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }

    }

    private static List<String> getDuplicatedValues(IndexReader ir, String field, AtomicBoolean cancel) throws IOException {
        List<String> values = new ArrayList<String>();
        TermEnum terms = ir.terms( new Term(field));
        //while (terms.next()) {
        do {
            if (cancel.get()) return Collections.emptyList();

            final Term term =  terms.term();

            if ( !field.equals( term.field() ) ) {
                break;
            }

            if (terms.docFreq() < 2) continue;

            values.add(term.text());
        }
        while (terms.next());
        return values;
    }

    private static long getValue(String encoded) {
        return Long.parseLong(encoded.substring(encoded.lastIndexOf(":") + 1));
    }

    private static void sortHashes(List<String> hashes) {
        Collections.sort(hashes, new Comparator<String>() {
            public int compare(String arg0, String arg1) {
                return (int) Math.signum(getValue(arg1) - getValue(arg0));
            }
        });
    }
    
    private static boolean subsumes(DuplicateDescription bigger, DuplicateDescription smaller) {
        Set<FileObject> bFiles = new HashSet<FileObject>();

        for (Span s : bigger.dupes) {
            bFiles.add(s.file);
        }

        Set<FileObject> sFiles = new HashSet<FileObject>();

        for (Span s : smaller.dupes) {
            sFiles.add(s.file);
        }

        if (!bFiles.equals(sFiles)) return false;

        Span testAgainst = bigger.dupes.get(0);

        for (Span s : smaller.dupes) {
            if (s.file == testAgainst.file) {
                if (   (testAgainst.startOff <= s.startOff && testAgainst.endOff > s.endOff)
                    || (testAgainst.startOff < s.startOff && testAgainst.endOff >= s.endOff)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static Map<String, long[]> encodeGeneralized(CompilationInfo info) {
        return encodeGeneralized(JavaSourceAccessor.getINSTANCE().getJavacTask(info), info.getCompilationUnit());
    }

    public static Map<String, long[]> encodeGeneralized(final CompilationTask task, final CompilationUnitTree cut) {
        final SourcePositions sp = Trees.instance(task).getSourcePositions();
        final Map<String, Collection<Long>> positions = new HashMap<String, Collection<Long>>();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void scan(Tree tree, Void p) {
                if (tree == null) return null;
                if (getCurrentPath() != null) {
                    Tree generalizedPattern = Utilities.generalizePattern(task, new TreePath(getCurrentPath(), tree));
                    long value = Utilities.patternValue(generalizedPattern);
                    if (value >= MINIMAL_VALUE) {
                        {
                            DigestOutputStream baos = null;
                            try {
                                baos = new DigestOutputStream(new ByteArrayOutputStream(), MessageDigest.getInstance("MD5"));
                                final EncodingContext ec = new BulkSearch.EncodingContext(baos, true);
                                BulkSearch.getDefault().encode( generalizedPattern, ec);
                                StringBuilder text = new StringBuilder();
                                byte[] bytes = baos.getMessageDigest().digest();
                                for (int cntr = 0; cntr < 4; cntr++) {
                                    text.append(String.format("%02X", bytes[cntr]));
                                }
                                text.append(':').append(value);
                                String enc = text.toString();
                                Collection<Long> spanSpecs = positions.get(enc);
                                if (spanSpecs == null) {
                                    positions.put(enc, spanSpecs = new LinkedList<Long>());
//                                } else {
//                                    spanSpecs.append(";");
                                }
                                long start = sp.getStartPosition(cut, tree);
//                                spanSpecs.append(start).append(":").append(sp.getEndPosition(cut, tree) - start);
                                spanSpecs.add(start);
                                spanSpecs.add(sp.getEndPosition(cut, tree));
                            } catch (NoSuchAlgorithmException ex) {
                                Exceptions.printStackTrace(ex);
                           } finally {
                                try {
                                    baos.close();
                                } catch (IOException ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                            }
                        }
                    }
                }
                return super.scan(tree, p);
            }
        }.scan(cut, null);

        Map<String, long[]> result = new TreeMap<String, long[]>();

        for (Entry<String, Collection<Long>> e : positions.entrySet()) {
            long[] spans = new long[e.getValue().size()];
            int idx = 0;

            for (Long l : e.getValue()) {
                spans[idx++] = l;
            }

            result.put(e.getKey(), spans);
        }

        return result;
    }

    private static final int MINIMAL_VALUE = 10;

    public static final class DuplicateDescription {

        public final List<Span> dupes;
        public final long value;
        public final String hash;

        private DuplicateDescription(List<Span> dupes, long value, String hash) {
            this.dupes = dupes;
            this.value = value;
            this.hash = hash;
        }

        public static DuplicateDescription of(List<Span> dupes, long value, String hash) {
            return new DuplicateDescription(dupes, value, hash);
        }
    }

    public static final class Span {
        public final FileObject file;
        public final int startOff;
        public final int endOff;

        public Span(FileObject file, int startOff, int endOff) {
            this.file = file;
            this.startOff = startOff;
            this.endOff = endOff;
        }

        public static @CheckForNull Span of(FileObject file, String spanSpec) {
            String[] split = spanSpec.split(":");
            int start = Integer.valueOf(split[0]);
            int end = start + Integer.valueOf(split[1]);
            if (start < 0 || end < 0) return null; //XXX

            return new Span(file, start, end);
        }

    }
}
