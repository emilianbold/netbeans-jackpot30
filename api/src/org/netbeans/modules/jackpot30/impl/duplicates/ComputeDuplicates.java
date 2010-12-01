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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.Position.Bias;
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
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesCustomIndexerImpl;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesIndex;
import org.netbeans.modules.jackpot30.impl.indexing.AbstractLuceneIndex.BitSetCollector;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.PositionBounds;
import org.openide.text.PositionRef;
import org.openide.util.Exceptions;


/**
 *
 * @author lahvac
 */
public class ComputeDuplicates {

    public Collection<? extends DuplicateDescription> computeDuplicatesForAllOpenedProjects(ProgressHandle progress, AtomicBoolean cancel) throws IOException {
        Set<URL> urls = new HashSet<URL>();

        for (ClassPath cp : GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)) {
            for (ClassPath.Entry e : cp.entries()) {
                urls.add(e.getURL());
            }
        }

        return computeDuplicates(urls, progress, cancel);
    }

    public Collection<? extends DuplicateDescription> computeDuplicates(Set<URL> forURLs, ProgressHandle progress, AtomicBoolean cancel) throws IOException {
        Map<IndexReader, FileObject> readers2Roots = new LinkedHashMap<IndexReader, FileObject>();

        progress.progress("Updating indices");
        
        for (URL u : forURLs) {
            try {
                //TODO: needs to be removed for server mode
                new DuplicatesCustomIndexerImpl.FactoryImpl().updateIndex(u, cancel); //TODO: show updating progress to the user
                
                File cacheRoot = Cache.findCache(DuplicatesIndex.NAME).findCacheRoot(u);

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

        Set<String> of2 = new TreeSet<String>(new Comparator<String>() {
            public int compare(String arg0, String arg1) {
                long value0 = Long.parseLong(arg0.substring(arg0.lastIndexOf(":") + 1));
                long value1 = Long.parseLong(arg1.substring(arg1.lastIndexOf(":") + 1));
                
                return (int) Math.signum(value1 - value0);
            }
        });
        
        CONT: for (String gen : getFieldValueFrequencies(r, "generalized", cancel)) {
            if (cancel.get()) return Collections.emptyList();
            
            for (Iterator<String> it = of2.iterator(); it.hasNext(); )  {
                String n = stripValue(it.next());
                String fValue = stripValue(gen);

                if (n.contains(fValue)) {
                    continue CONT;
                }
                if (fValue.contains(n)) {
                    it.remove();
                }
            }
            of2.add(gen);
        }

        List<String> dd = new ArrayList<String>(of2);

        //TODO: only show valuable duplicates?:
//        dd = dd.subList(0, dd.size() / 10 + 1);

        progress.switchToDeterminate(dd.size());
        
        List<DuplicateDescription> result = new LinkedList<DuplicateDescription>();
        int done = 0;

        for (String longest : dd) {
            List<Span> foundDuplicates = new LinkedList<Span>();

            Query query = new TermQuery(new Term("generalized", longest));

            for (Entry<IndexReader, FileObject> e : readers2Roots.entrySet()) {
                if (cancel.get()) return Collections.emptyList();

                Searcher s = new IndexSearcher(e.getKey());
                BitSet matchingDocuments = new BitSet(e.getKey().maxDoc());
                Collector c = new BitSetCollector(matchingDocuments);

                s.search(query, c);

                for (int docNum = matchingDocuments.nextSetBit(0); docNum >= 0; docNum = matchingDocuments.nextSetBit(docNum + 1)) {
                    final Document doc = e.getKey().document(docNum);
                    String spanSpec = doc.getValues("positions")[Arrays.binarySearch(doc.getValues("generalized"), longest)];
                    String relPath = doc.getField("path").stringValue();

                    for (String spanPart : spanSpec.split(";")) {
                        Span span = Span.of(e.getValue().getFileObject(relPath), spanPart);

                        if (span != null) {
                            foundDuplicates.add(span);
                        }
                    }
                }
            }

            if (foundDuplicates.size() >= 2) {
                result.add(DuplicateDescription.of(foundDuplicates));
            }

            progress.progress(++done);
        }

        progress.finish();

        return result;
    }

    private static List<String> getFieldValueFrequencies(IndexReader ir, String field, AtomicBoolean cancel) throws IOException {
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

    private static String stripValue(String encoded) {
        return encoded.substring(0, encoded.lastIndexOf(':'));
    }

    public static final class DuplicateDescription {

        public final List<Span> dupes;

        private DuplicateDescription(List<Span> dupes) {
            this.dupes = dupes;
        }

        public static DuplicateDescription of(List<Span> dupes) {
            return new DuplicateDescription(dupes);
        }
    }

    public static final class Span {
        public final FileObject file;
        public final PositionBounds span;

        private Span(FileObject file, PositionBounds span) {
            this.file = file;
            this.span = span;
        }

        public static @CheckForNull Span of(FileObject file, String spanSpec) {
            try {
                String[] split = spanSpec.split(":");
                int start = Integer.valueOf(split[0]);
                int end = Integer.valueOf(split[1]);
                if (start == (-1) || end == (-1)) return null; //XXX
                
                DataObject od = DataObject.find(file);
                CloneableEditorSupport ces = (CloneableEditorSupport) od.getLookup().lookup(EditorCookie.class);

                PositionRef startRef = ces.createPositionRef(start, Bias.Forward);
                PositionRef endRef   = ces.createPositionRef(end,   Bias.Backward);

                return new Span(file, new PositionBounds(startRef, endRef));
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
        }

    }
}
