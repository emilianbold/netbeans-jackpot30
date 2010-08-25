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

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.indexing.AbstractLuceneIndex;
import org.netbeans.modules.jackpot30.impl.indexing.Index.IndexWriter;
import org.netbeans.modules.jackpot30.impl.indexing.IndexInfo;

/**
 *
 * @author lahvac
 */
public class NFABasedBulkSearchTest extends BulkSearchTestPerformer {

    public NFABasedBulkSearchTest(String name) {
        super(name);
    }

//    public static TestSuite suite() {
//        NbTestSuite r = new NbTestSuite();
//
//        r.addTest(new NFABasedBulkSearchTest("testField1"));
//        r.addTest(new NFABasedBulkSearchTest("testField2"));
//
//        return r;
//    }

    @Override
    protected BulkSearch createSearch() {
        return new BulkSearchImpl(false);
    }

    private static class BulkSearchImpl extends BulkSearch {

        public BulkSearchImpl(boolean requiresLightweightVerification) {
            super(requiresLightweightVerification);
        }

        @Override
        public Map<String, Collection<TreePath>> match(CompilationInfo info, TreePath toSearch, BulkPattern pattern, Map<String, Long> timeLog) {
            try {
                IndexImpl ii = new IndexImpl();
                IndexWriter writer = ii.openForWriting();

                writer.record(toSearch.getCompilationUnit().getSourceFile().toUri().toURL(), toSearch.getCompilationUnit());
                writer.close();

                if (!ii.findCandidates(pattern).isEmpty()) {
                    return new NFABasedBulkSearch().match(info, toSearch, pattern, timeLog);
                } else {
                    assertTrue(new NFABasedBulkSearch().match(info, toSearch, pattern, timeLog).isEmpty());
                    return Collections.emptyMap();
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public boolean matches(InputStream encoded, BulkPattern pattern) {
            return new NFABasedBulkSearch().matches(encoded, pattern);
        }

        @Override
        public boolean matches(CompilationInfo info, TreePath toSearch, BulkPattern pattern) {
            return new NFABasedBulkSearch().matches(info, toSearch, pattern);
        }

        @Override
        public void encode(Tree tree, EncodingContext ctx) {
            new NFABasedBulkSearch().encode(tree, ctx);
        }

        @Override
        public BulkPattern create(Collection<? extends String> code, Collection<? extends Tree> patterns) {
            return new NFABasedBulkSearch().create(code, patterns);
        }
    }

    private static final class IndexImpl extends AbstractLuceneIndex {

        private final Directory dir = new RAMDirectory();

        public IndexImpl() {
            super(0, false, true);
        }

        @Override
        protected IndexReader createReader() throws IOException {
            return IndexReader.open(dir, true);
        }

        @Override
        protected org.apache.lucene.index.IndexWriter createWriter() throws IOException {
            return new org.apache.lucene.index.IndexWriter(dir, new NoAnalyzer(), MaxFieldLength.UNLIMITED);
        }

        @Override
        public IndexInfo getIndexInfo() {
            return IndexInfo.empty();
        }

        @Override
        protected void storeIndexInfo(IndexInfo info) throws IOException {}
    }

    private static final class NoAnalyzer extends Analyzer {

        @Override
        public TokenStream tokenStream(String string, Reader reader) {
            throw new UnsupportedOperationException("Should not be called");
        }

    }

}
