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

package org.netbeans.modules.jackpot30.impl.indexing;

import com.sun.source.tree.CompilationUnitTree;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.FSDirectory;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.EncodingContext;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public final class FileBasedIndex extends Index {

    private static final Logger LOG = Logger.getLogger(FileBasedIndex.class.getName());
    
    public static Index create(URL sourceRoot, File indexRoot) {
        return new FileBasedIndex(sourceRoot, indexRoot);
    }

    public static @CheckForNull Index get(URL sourceRoot) throws IOException {
        return new FileBasedIndex(sourceRoot, Cache.findCache(FileBasedIndex.NAME).findCacheRoot(sourceRoot)); //XXX: new!
    }

    private final URL  sourceRoot;
    private final int  stripLength;
    private final File cacheRoot;

    private FileBasedIndex(URL sourceRoot, File cacheRoot) {
        this.sourceRoot = sourceRoot;
        this.stripLength = sourceRoot.getPath().length();
        this.cacheRoot = cacheRoot;
    }

    public IndexWriterImpl openForWriting() throws IOException {
        return new IndexWriterImpl();
    }

    public Collection<? extends String> findCandidates(BulkPattern pattern) throws IOException {
        File dir = new File(cacheRoot, "fulltext");

        if (dir.listFiles() == null || dir.listFiles().length == 0) {
            return Collections.emptyList();
        }
        
        IndexReader reader = IndexReader.open(FSDirectory.open(dir), true);
        Searcher s = new IndexSearcher(reader);
        BitSet matchingDocuments = new BitSet(reader.maxDoc());
        Collector c = new BitSetCollector(matchingDocuments);

        try {
            s.search(query(pattern), c);
        } catch (ParseException ex) {
            throw new IOException(ex);
        }

        Collection<String> result = new HashSet<String>();

        for (int docNum = matchingDocuments.nextSetBit(0); docNum >= 0; docNum = matchingDocuments.nextSetBit(docNum+1)) {
            try {
                final Document doc = reader.document(docNum);
                
                ByteArrayInputStream in = new ByteArrayInputStream(CompressionTools.decompress(doc.getField("encoded").getBinaryValue()));

                try {
                    if (!BulkSearch.getDefault().matches(in, pattern)) {
                        continue;
                    }
                } finally {
                    in.close();
                }

                result.add(doc.getField("path").stringValue());
            } catch (DataFormatException ex) {
                throw new IOException(ex);
            }
        }

        return result;
    }

    private Query query(BulkPattern pattern) throws ParseException {
        BooleanQuery result = new BooleanQuery();

        for (int cntr = 0; cntr < pattern.getIdentifiers().size(); cntr++) {
            assert !pattern.getRequiredContent().get(cntr).isEmpty();
            
            BooleanQuery emb = new BooleanQuery();
            
            for (List<String> c : pattern.getRequiredContent().get(cntr)) {
                if (c.isEmpty()) continue;
                
                PhraseQuery pq = new PhraseQuery();
                
                for (String s : c) {
                    pq.add(new Term("content", s));
                }
                
                emb.add(pq, BooleanClause.Occur.MUST);
            }
            result.add(emb, BooleanClause.Occur.SHOULD);
        }

        return result;
    }

    public CharSequence getSourceCode(String relativePath) {
        return getSourceCode(sourceRoot, relativePath);
    }
    
    static CharSequence getSourceCode(URL sourceRoot, String relativePath) {
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

    public @NonNull IndexInfo getIndexInfo() {
        File infoFile = new File(cacheRoot, "info");

        if (infoFile.exists()) {
            try {
                return Pojson.load(IndexInfo.class, infoFile);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        return IndexInfo.empty();
    }

    private final class IndexWriterImpl extends IndexWriter {

        private final org.apache.lucene.index.IndexWriter luceneWriter;
        private final File infoFile;
        private final IndexInfo info;

        public IndexWriterImpl() throws IOException {
            luceneWriter = new org.apache.lucene.index.IndexWriter(FSDirectory.open(new File(cacheRoot, "fulltext")), new NoAnalyzer(), MaxFieldLength.UNLIMITED);

            infoFile = new File(cacheRoot, "info");

            if (infoFile.exists()) {
                info = Pojson.load(IndexInfo.class, infoFile);
            } else {
                info = new IndexInfo();
                info.majorVersion = 1;
                info.minorVersion = 1;
            }
        }

        public void record(URL source, final CompilationUnitTree cut) throws IOException {
            String relative = source.getPath().substring(stripLength);
            ByteArrayOutputStream out = null;
            EncodingContext ec = null;
            
            try {
                out = new ByteArrayOutputStream();

                ec = new EncodingContext(out, false);

                BulkSearch.getDefault().encode(cut, ec);

                luceneWriter.deleteDocuments(new Term("path", relative));
            
                Document doc = new Document();

                doc.add(new Field("content", new TokenStreamImpl(ec.getContent())));
                out.close();
                doc.add(new Field("encoded", CompressionTools.compress(out.toByteArray()), Field.Store.YES));
                doc.add(new Field("path", relative, Field.Store.YES, Field.Index.NOT_ANALYZED));

                luceneWriter.addDocument(doc);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                Logger.getLogger(FileBasedIndex.class.getName()).log(Level.WARNING, null, t);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }

        public void remove(String relativePath) throws IOException {
            luceneWriter.deleteDocuments(new Term("path", relativePath));

            info.totalFiles--;
        }

        public void close() throws IOException {
            luceneWriter.optimize();
            luceneWriter.close();
            info.lastUpdate = System.currentTimeMillis();
            Pojson.save(info, infoFile);
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

    private static class BitSetCollector extends Collector {

        private int docBase;
        public final BitSet bits;

        public BitSetCollector(final BitSet bitSet) {
            assert bitSet != null;
            bits = bitSet;
        }

        // ignore scorer
        public void setScorer(Scorer scorer) {
        }

        // accept docs out of order (for a BitSet it doesn't matter)
        public boolean acceptsDocsOutOfOrder() {
          return true;
        }

        public void collect(int doc) {
          bits.set(doc + docBase);
        }

        public void setNextReader(IndexReader reader, int docBase) {
          this.docBase = docBase;
        }

    }

    private static final class NoAnalyzer extends Analyzer {

        @Override
        public TokenStream tokenStream(String string, Reader reader) {
            throw new UnsupportedOperationException("Should not be called");
        }

    }
    
    public static final String NAME = "jackpot30"; //NOI18N
}
