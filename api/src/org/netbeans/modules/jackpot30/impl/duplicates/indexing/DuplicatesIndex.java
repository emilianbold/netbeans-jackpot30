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

package org.netbeans.modules.jackpot30.impl.duplicates.indexing;

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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler.CompilationTask;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.indexing.FileBasedIndex.NoAnalyzer;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.EncodingContext;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class DuplicatesIndex {

    private final URL  sourceRoot;
    private final int  stripLength;
    private final File cacheRoot;

    private DuplicatesIndex(URL sourceRoot, File cacheRoot) {
        this.sourceRoot = sourceRoot;
        this.stripLength = sourceRoot.getPath().length();
        this.cacheRoot = cacheRoot;
    }

    public static @CheckForNull DuplicatesIndex get(URL sourceRoot) throws IOException {
        return new DuplicatesIndex(sourceRoot, Cache.findCache(NAME).findCacheRoot(sourceRoot)); //XXX: new!
    }

    public IndexWriter openForWriting() throws IOException {
        return new IndexWriter();
    }

    public class IndexWriter {

        private final org.apache.lucene.index.IndexWriter luceneWriter;

        public IndexWriter() throws IOException {
            luceneWriter = new org.apache.lucene.index.IndexWriter(FSDirectory.open(new File(cacheRoot, "fulltext")), new NoAnalyzer(), MaxFieldLength.UNLIMITED);
        }

        public void record(final CompilationInfo info, URL source, final CompilationUnitTree cut) throws IOException {
            record(JavaSourceAccessor.getINSTANCE().getJavacTask(info), source, cut);
        }

        public void record(final CompilationTask task, URL source, final CompilationUnitTree cut) throws IOException {
            String relative = source.getPath().substring(stripLength);

            try {
                final Document doc = new Document();

                doc.add(new Field("path", relative, Field.Store.YES, Field.Index.NOT_ANALYZED));

                final SourcePositions sp = Trees.instance(task).getSourcePositions();
                final Map<String, StringBuilder> positions = new TreeMap<String, StringBuilder>();

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
                                        StringBuilder spanSpecs = positions.get(enc);
                                        if (spanSpecs == null) {
                                            positions.put(enc, spanSpecs = new StringBuilder());
                                        } else {
                                            spanSpecs.append(";");
                                        }
                                        long start = sp.getStartPosition(cut, tree);
                                        spanSpecs.append(start).append(":").append(sp.getEndPosition(cut, tree) - start);
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

                for (Entry<String, StringBuilder> e : positions.entrySet()) {
                    doc.add(new Field("generalized", e.getKey(), Store.YES, Index.NOT_ANALYZED));
                    doc.add(new Field("positions", e.getValue().toString(), Store.YES, Index.NO));
                }

                luceneWriter.addDocument(doc);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                Logger.getLogger(DuplicatesIndex.class.getName()).log(Level.WARNING, null, t);
            }
        }

        public void remove(String relativePath) throws IOException {
            luceneWriter.deleteDocuments(new Term("path", relativePath));
        }
        
        public void close() throws IOException {
            luceneWriter.optimize();
            luceneWriter.close();
        }
    }

    private static final int MINIMAL_VALUE = 10;

    public static final String NAME = "duplicates"; //NOI18N
}
