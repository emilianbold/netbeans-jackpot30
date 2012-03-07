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

package org.netbeans.modules.jackpot30.server.indexer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesIndex;
import org.netbeans.modules.jackpot30.impl.indexing.AbstractLuceneIndex.BitSetCollector;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.indexing.FileBasedIndex;
import org.netbeans.modules.java.hints.jackpot.impl.Utilities;
import org.netbeans.modules.java.hints.jackpot.impl.pm.BulkSearch;
import org.netbeans.modules.java.hints.jackpot.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.java.hints.jackpot.spi.HintDescription;
import org.netbeans.modules.java.hints.jackpot.spi.HintDescription.AdditionalQueryConstraints;
import org.netbeans.modules.java.hints.jackpot.spi.PatternConvertor;
import org.netbeans.modules.java.hints.jackpot.spi.Trigger.PatternDescription;

/**
 *
 * @author lahvac
 */
public class StandaloneFinder {

    public static Collection<? extends String> findCandidates(File sourceRoot, String pattern) throws IOException {
        BulkPattern bulkPattern = preparePattern(pattern, null);
        
        return FileBasedIndex.get(sourceRoot.toURI().toURL()).findCandidates(bulkPattern);
    }

    public static int[] findCandidateOccurrenceSpans(File sourceRoot, String relativePath, String pattern) throws IOException {
        BulkPattern bulkPattern = preparePattern(pattern, null);
        CharSequence source = FileBasedIndex.get(sourceRoot.toURI().toURL()).getSourceCode(relativePath).toString().replaceAll("\r\n", "\n");
        JavacTaskImpl jti = prepareJavacTaskImpl();
        CompilationUnitTree cut = jti.parse(new JFOImpl(source)).iterator().next();
        Collection<TreePath> paths = new LinkedList<TreePath>();
        
        for (Collection<TreePath> c : BulkSearch.getDefault().match(null, new TreePath(cut), bulkPattern).values()) {
            paths.addAll(c);
        }

        Trees t = Trees.instance(jti);
        int[] result = new int[2 * paths.size()];
        int i = 0;

        for (TreePath tp : paths) {
            result[i++] = (int) t.getSourcePositions().getStartPosition(cut, tp.getLeaf());
            result[i++] = (int) t.getSourcePositions().getEndPosition(cut, tp.getLeaf());
        }

        return result;
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> parseAndReportErrors(String pattern) {
        Collection<Diagnostic<? extends JavaFileObject>> errors = new LinkedList<Diagnostic<? extends JavaFileObject>>();

        preparePattern(pattern, errors);

        return errors;
    }

    public static Map<String, Collection<? extends String>> containsHash(File sourceRoot, Iterable<? extends String> hashes) throws IOException {
        File cacheRoot = Cache.findCache(DuplicatesIndex.NAME, DuplicatesIndex.VERSION).findCacheRoot(sourceRoot.toURI().toURL());
        File dir = new File(cacheRoot, "fulltext");

        if (dir.listFiles() != null && dir.listFiles().length > 0) {
            IndexReader reader = IndexReader.open(FSDirectory.open(dir), true);
            Map<String, Collection<? extends String>> result = new HashMap<String, Collection<? extends String>>();

            for (String hash : hashes) {
                Collection<String> found = new LinkedList<String>();
                Query query = new TermQuery(new Term("generalized", hash));
                Searcher s = new IndexSearcher(reader);
                BitSet matchingDocuments = new BitSet(reader.maxDoc());
                Collector c = new BitSetCollector(matchingDocuments);

                s.search(query, c);

                for (int docNum = matchingDocuments.nextSetBit(0); docNum >= 0; docNum = matchingDocuments.nextSetBit(docNum + 1)) {
                    final Document doc = reader.document(docNum);

                    found.add(doc.getField("path").stringValue());
                }

                result.put(hash, found);
            }
            
            return result;
        }

        return Collections.emptyMap();
    }
    
    private static BulkPattern preparePattern(String pattern, Collection<Diagnostic<? extends JavaFileObject>> errors) {
        return preparePattern(PatternConvertor.create(pattern), errors);
    }

    //XXX: copied from BatchSearch, may be possible to merge once CompilationInfo is accessible in server mode
    private static BulkPattern preparePattern(final Iterable<? extends HintDescription> patterns, Collection<Diagnostic<? extends JavaFileObject>> errors) {
        JavacTaskImpl javac = prepareJavacTaskImpl();
        Collection<String> code = new LinkedList<String>();
        Collection<Tree> trees = new LinkedList<Tree>();
        Collection<AdditionalQueryConstraints> additionalConstraints = new LinkedList<AdditionalQueryConstraints>();

        for (HintDescription pattern : patterns) {
            String textPattern = ((PatternDescription) pattern.getTrigger()).getPattern();

            code.add(textPattern);
            trees.add(Utilities.parseAndAttribute(javac, textPattern, errors));
            additionalConstraints.add(pattern.getAdditionalConstraints());
        }

        return BulkSearch.getDefault().create(code, trees, additionalConstraints);
    }

    private static JavacTaskImpl prepareJavacTaskImpl() {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();

        assert tool != null;

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov"), null, Collections.<JavaFileObject>emptyList());
        
        return ct;
    }

    private static final class JFOImpl extends SimpleJavaFileObject {
        private final CharSequence code;
        public JFOImpl(CharSequence code) {
            super(URI.create(""), Kind.SOURCE);
            this.code = code;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }
    }
}