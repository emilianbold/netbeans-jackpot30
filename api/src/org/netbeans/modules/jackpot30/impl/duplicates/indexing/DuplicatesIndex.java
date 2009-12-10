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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.JavaCompiler.CompilationTask;
import org.codeviation.commons.patterns.Caches;
import org.codeviation.commons.patterns.Factory;
import org.codeviation.lutz.Lutz.SuppressIndexing;
import org.codeviation.pojson.Pojson.SuppressStoring;
import org.codeviation.strast.IndexingStorage;
import org.codeviation.strast.Strast;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
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

        private final IndexingStorage s;

        public IndexWriter() throws IOException {
            s = Strast.createIndexingStorage(new File(cacheRoot, "duplicates/objects"), new File(cacheRoot, "duplicates/index"));
        }

        public void record(final CompilationInfo info, URL source, final CompilationUnitTree cut) throws IOException {
            record(JavaSourceAccessor.getINSTANCE().getJavacTask(info), source, cut);
        }

        public void record(final CompilationTask task, URL source, final CompilationUnitTree cut) throws IOException {
            String relative = source.getPath().substring(stripLength);
            
            try {
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
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {
                Logger.getLogger(DuplicatesIndex.class.getName()).log(Level.WARNING, null, t);
            }
        }

        public void remove(String relativePath) throws IOException {
            s.delete(relativePath);
        }
        
        public void close() throws IOException {
            s.close();
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

    public static final String NAME = "duplicates"; //NOI18N
}
