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
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexReader;
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
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.EncodingContext;
import org.netbeans.modules.jackpot30.spi.HintDescription.AdditionalQueryConstraints;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public abstract class AbstractLuceneIndex extends Index {

    public static final int MAJOR_VERSION = 1;
    //2: partial attribution added (erased types that occur in the file)
    public static final int MINOR_VERSION = 2;
    
    private final int  stripLength;
    private final boolean readOnly;
    private final boolean storeFullSourceCode;

    protected AbstractLuceneIndex(int stripLength, boolean readOnly, boolean storeFullSourceCode) {
        this.stripLength = stripLength;
        this.readOnly = readOnly;
        this.storeFullSourceCode = storeFullSourceCode;
     }

    public final IndexWriter openForWriting() throws IOException {
        if (readOnly) throw new UnsupportedOperationException();
         return new IndexWriterImpl();
     }

    protected abstract IndexReader createReader() throws IOException;
    protected abstract org.apache.lucene.index.IndexWriter createWriter() throws IOException;

    @Override
    public Collection<? extends String> findCandidates(BulkPattern pattern) throws IOException {
        IndexReader reader = createReader();

        if (reader == null) {
             return Collections.emptyList();
         }

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
                final Document doc = reader.document(docNum, new FieldSelector() {
                    public FieldSelectorResult accept(String string) {
                        return "encoded".equals(string) || "path".equals(string) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD;
                    }
                });
                
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
            
            AdditionalQueryConstraints additionalConstraints = pattern.getAdditionalConstraints().get(cntr);

            if (additionalConstraints != null && !additionalConstraints.requiredErasedTypes.isEmpty()) {
                BooleanQuery constraintsQuery = new BooleanQuery();

                constraintsQuery.add(new TermQuery(new Term("attributed", "false")), BooleanClause.Occur.SHOULD);

                BooleanQuery constr = new BooleanQuery();

                for (String tc : additionalConstraints.requiredErasedTypes) {
                    constr.add(new TermQuery(new Term("erasedTypes", tc)), BooleanClause.Occur.MUST);
                }

                constraintsQuery.add(constr, BooleanClause.Occur.SHOULD);
                emb.add(constraintsQuery, BooleanClause.Occur.MUST);
            }

            result.add(emb, BooleanClause.Occur.SHOULD);
        }

        return result;
    }

    public CharSequence getSourceCode(String relativePath) {
        try {
            IndexReader reader = createReader();

            if (reader == null) {
                return "";
            }

            Searcher s = new IndexSearcher(reader);
            TopDocs topDocs = s.search(new TermQuery(new Term("path", relativePath)), 1);

            if (topDocs.totalHits < 1) return "";

            Document doc = s.doc(topDocs.scoreDocs[0].doc, new FieldSelector() {
                public FieldSelectorResult accept(String string) {
                    return "sourceCode".equals(string) ? FieldSelectorResult.LOAD : FieldSelectorResult.NO_LOAD;
                }
            });
            
            return CompressionTools.decompressString(doc.getField("sourceCode").getBinaryValue());
        } catch (DataFormatException ex) {
            Exceptions.printStackTrace(ex);
            return "";
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return "";
        }
    }
    
    public abstract @NonNull IndexInfo getIndexInfo();
    protected abstract void storeIndexInfo(@NonNull IndexInfo info) throws IOException;

    private final class IndexWriterImpl extends IndexWriter {

        private final org.apache.lucene.index.IndexWriter luceneWriter;
        private final IndexInfo info;

        public IndexWriterImpl() throws IOException {
            luceneWriter = createWriter();

            info = getIndexInfo();
        }

        @Override
        public void record(URL source, final CompilationUnitTree cut, final AttributionWrapper attributed) throws IOException {
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
                if (storeFullSourceCode) {
                    doc.add(new Field("sourceCode", CompressionTools.compressString(cut.getSourceFile().getCharContent(false).toString()), Field.Store.YES));
                }
                doc.add(new Field("path", relative, Field.Store.YES, Field.Index.NOT_ANALYZED));

                if (attributed != null) {
                    final Set<String> erased = new HashSet<String>();

                    new TreePathScanner<Void, Void>() {
                        @Override
                        public Void scan(Tree tree, Void p) {
                            if (tree != null) {
                                TreePath tp = new TreePath(getCurrentPath(), tree);
                                TypeMirror type = attributed.trees.getTypeMirror(tp);

                                if (type != null) {
                                    if (type.getKind() == TypeKind.ARRAY) {
                                        erased.add(attributed.types.erasure(type).toString());
                                        type = ((ArrayType) type).getComponentType();
                                    }

                                    if (type.getKind().isPrimitive() || type.getKind() == TypeKind.DECLARED) {
                                        addErasedTypeAndSuperTypes(attributed, erased, type);
                                    }
                                }

                                //bounds for type variables!!!
                            }
                            return super.scan(tree, p);
                        }
                    }.scan(cut, null);

                    doc.add(new Field("attributed", "true", Field.Store.YES, Field.Index.NOT_ANALYZED));
                    doc.add(new Field("erasedTypes", new TokenStreamImpl(erased)));
                } else {
                    doc.add(new Field("attributed", "false", Field.Store.YES, Field.Index.NOT_ANALYZED));
                }
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
        }

        public void clear() throws IOException {
            luceneWriter.deleteAll();
        }

        public void close() throws IOException {
            luceneWriter.optimize();
            luceneWriter.close();
            info.majorVersion = MAJOR_VERSION;
            info.minorVersion = MINOR_VERSION;
            info.totalFiles = luceneWriter.numDocs();
            info.lastUpdate = System.currentTimeMillis();
            storeIndexInfo(info);
        }
    }

    private static void addErasedTypeAndSuperTypes(AttributionWrapper attributed, Set<String> types, TypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            if (types.add(attributed.types.erasure(type).toString())) {
                for (TypeMirror sup : attributed.types.directSupertypes(type)) {
                    addErasedTypeAndSuperTypes(attributed, types, sup);
                }
            }
        } else if (type.getKind().isPrimitive()) {
            types.add(type.toString());
        }
    }

    public static final class TokenStreamImpl extends TokenStream {

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

    public static class BitSetCollector extends Collector {

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

}
