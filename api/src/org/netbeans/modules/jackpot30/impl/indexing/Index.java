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

package org.netbeans.modules.jackpot30.impl.indexing;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.tools.JavaCompiler.CompilationTask;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.codeviation.commons.patterns.Caches;
import org.codeviation.commons.patterns.Factory;
import org.codeviation.lutz.Lutz.SuppressIndexing;
import org.codeviation.pojson.Pojson.SuppressStoring;
import org.codeviation.strast.IndexingStorage;
import org.codeviation.strast.Strast;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.EncodingContext;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class Index {

    private final URL  sourceRoot;
    private final int  stripLength;
    private final File cacheRoot;

    private Index(URL sourceRoot, File cacheRoot) {
        this.sourceRoot = sourceRoot;
        this.stripLength = sourceRoot.getPath().length();
        this.cacheRoot = cacheRoot;
    }

    public static @CheckForNull Index get(URL sourceRoot) throws IOException {
        return new Index(sourceRoot, Cache.findCacheRoot(sourceRoot)); //XXX: new!
    }

    public IndexWriter openForWriting() throws IOException {
        return new IndexWriter();
    }

    public Collection<? extends String> findCandidates(BulkPattern pattern) throws IOException {
        Collection<? extends String> candidates = findCandidatesFromLucene(pattern);

        if (candidates.isEmpty()) {
            return candidates;
        }

        for (Iterator<? extends String> it = candidates.iterator(); it.hasNext();) {
            String relative = it.next();
            InputStream in = new FileInputStream(new File(new File(cacheRoot, "encoded"), relative));
            
            if (!BulkSearch.getDefault().matches(in, pattern)) {
                it.remove();
            }
        }

        return candidates;
    }

    private Collection<? extends String> findCandidatesFromLucene(BulkPattern pattern) throws IOException {
        File dir = new File(cacheRoot, "fulltext");

        if (dir.listFiles() == null || dir.listFiles().length == 0) {
            return Collections.emptyList();
        }
        
        IndexReader reader = IndexReader.open(dir);
        Searcher s = new IndexSearcher(reader);

        Hits hits;
        try {
            hits = s.search(query(pattern));
        } catch (ParseException ex) {
            throw new IOException(ex);
        }

        Collection<String> result = new HashSet<String>();
        @SuppressWarnings("unchecked")
        Iterator<Hit> it = (Iterator<Hit>) hits.iterator();

        while (it.hasNext()) {
            Hit h = it.next();

            result.add(h.getDocument().getField("path").stringValue());
        }

        return result;
    }

    private Query query(BulkPattern pattern) throws ParseException {
        BooleanQuery result = new BooleanQuery();

        for (int cntr = 0; cntr < pattern.getIdentifiers().size(); cntr++) {
            BooleanQuery q = new BooleanQuery();
            if (!pattern.getIdentifiers().get(cntr).isEmpty()) {
                for (String a : pattern.getIdentifiers().get(cntr)) {
                    q.add(new TermQuery(new Term("identifiers", a)), BooleanClause.Occur.MUST);
                }
            }
            if (!pattern.getKinds().get(cntr).isEmpty()) {
                for (String t : pattern.getKinds().get(cntr)) {
                    q.add(new TermQuery(new Term("treeKinds", t)), BooleanClause.Occur.MUST);
                }
            }

            result.add(q, BooleanClause.Occur.SHOULD);
        }

        return result;
    }

    public CharSequence getSourceCode(String relativePath) {
        try {
            URI absolute = sourceRoot.toURI().resolve(relativePath);
            StringBuilder result = new StringBuilder();
            InputStream input = null;
            Reader reader = null;

            try {
                input = absolute.toURL().openStream();
                reader = new InputStreamReader(input); //XXX encoding!!!

                int read;

                while ((read = reader.read()) != (-1)) {
                    result.append((char) read);
                }

                return result;
            } finally {
                if (reader != null) {
                    reader.close();
                } else {
                    if (input != null) {
                        input.close();
                    }
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    public class IndexWriter {

        private final org.apache.lucene.index.IndexWriter luceneWriter;
        private final IndexingStorage s;

        public IndexWriter() throws IOException {
            luceneWriter = new org.apache.lucene.index.IndexWriter(new File(cacheRoot, "fulltext"), new StandardAnalyzer());
            s = Strast.createIndexingStorage(new File(cacheRoot, "duplicates/objects"), new File(cacheRoot, "duplicates/index"));
        }

        public void record(final CompilationInfo info, URL source, final CompilationUnitTree cut) throws IOException {
            record(JavaSourceAccessor.getINSTANCE().getJavacTask(info), source, cut);
        }

        public void record(final CompilationTask task, URL source, final CompilationUnitTree cut) throws IOException {
            String relative = source.getPath().substring(stripLength);
            OutputStream out = null;
            File generalizedFile = new File(new File(cacheRoot, "generalized"), relative);
            generalizedFile.getParentFile().mkdirs();
            final OutputStream genout = new FileOutputStream(generalizedFile);
            EncodingContext ec = null;
            
            try {
                File cacheFile = new File(new File(cacheRoot, "encoded"), relative);

                cacheFile.getParentFile().mkdirs();

                out = new FileOutputStream(cacheFile);

                final List<Collection<Data>> data = new ArrayList<Collection<Data>>();
                final Map<String, Integer> key2Index = new HashMap<String, Integer>();
                final org.codeviation.commons.patterns.Cache<Integer, String> cache = Caches.permanent(new Factory<Integer, String>() {
                    public Integer create(String key) {
                        int index = data.size();

                        data.add(new LinkedList<Data>());
                        key2Index.put(key, index);
                        
                        return index;
                    }
                });
                
                final SourcePositions sp = Trees.instance(task).getSourcePositions();

                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void scan(Tree tree, Void p) {
                        if (tree == null) return null;
                        if (getCurrentPath() != null) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            final EncodingContext ec = new BulkSearch.EncodingContext(baos);
                            Tree generalizedPattern = Utilities.generalizePattern(task, new TreePath(getCurrentPath(), tree));
                            long value = Utilities.patternValue(generalizedPattern);
                            if (value >= MINIMAL_VALUE) {
                                BulkSearch.getDefault().encode( generalizedPattern, ec);
                                try {
                                    final String text = new String(baos.toByteArray(), "UTF-8") + ":" + value;
                                    Integer i = cache.create(text);

                                    data.get(i).add(new Data(sp.getStartPosition(cut, tree), sp.getEndPosition(cut, tree), tree.toString(), value));
                                } catch (UnsupportedEncodingException ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                            }
                        }
                        return super.scan(tree, p);
                    }
                }.scan(cut, null);

                final DuplicatesIndexRecord r = new DuplicatesIndexRecord();
                r.data = key2Index.keySet();
                r.key2Index = key2Index;
                r.index2Data = new MultiData[data.size()];

                for (int c = 0; c < data.size(); c++) {
                    r.index2Data[c] = new MultiData(data.get(c).toArray(new Data[0]));
                }
                
                s.put(r, relative.split("/"));

                ec = new EncodingContext(out);

                BulkSearch.getDefault().encode(cut, ec);

                luceneWriter.deleteDocuments(new Term("path", relative));
            
                Document doc = new Document();

                doc.add(new Field("identifiers", new TokenStreamImpl(ec.getIdentifiers())));
                doc.add(new Field("treeKinds", new TokenStreamImpl(ec.getKinds())));
                doc.add(new Field("path", relative, Field.Store.YES, Field.Index.UN_TOKENIZED));

                luceneWriter.addDocument(doc);
            } finally {
                if (out != null) {
                    out.close();
                }
                if (genout != null) {
                    genout.close();
                }
            }
        }

        public void remove(String relativePath) throws IOException {
            File f = new File(new File(cacheRoot, "encoded"), relativePath);

            if (f.canRead()) {
                f.delete();
            }

            luceneWriter.deleteDocuments(new Term("path", relativePath));
        }

        public void close() throws IOException {
            luceneWriter.optimize();
            luceneWriter.close();
            s.close();
        }
    }

    private static final class TokenStreamImpl extends TokenStream {

        private final Iterator<? extends String> tokens;

        public TokenStreamImpl(Iterable<? extends String> tokens) {
            this.tokens = tokens != null ? tokens.iterator() : /*???*/Collections.<String>emptyList().iterator();
        }

        @Override
        public Token next() throws IOException {
            if (!tokens.hasNext())
                return null;

            String t = tokens.next();

            return new Token(t, 0, t.length());
        }
    }

    private static final int MINIMAL_VALUE = 5;

    @Strast.Index(value="di")
    public static class DuplicatesIndexRecord {
        @SuppressStoring
        public Set<String> data;
        @SuppressIndexing
        public Map<String, Integer> key2Index;
        @SuppressIndexing
        public MultiData[] index2Data;
    }

    public static class MultiData {
        public Data[] data;

        public MultiData() {}

        public MultiData(Data[] data) {
            this.data = data;
        }
    }

    public static class Data {
        public Data() {}
        public Data(long start, long end, String text, long value) {
            this.start = start;
            this.end = end;
            this.text = text;
            this.value = value;
        }
        public long start;
        public long end;
        public String text;
        public long value;
    }
}
