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
package org.netbeans.modules.jackpot30.impl.duplicates;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.text.Position.Bias;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.codeviation.lutz.Lutz;
import org.codeviation.lutz.Lutz.FieldConversion;
import org.codeviation.lutz.Search;
import org.codeviation.strast.IndexingStorage;
import org.codeviation.strast.Strast;
import org.codeviation.strast.model.Frequency;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.indexing.CustomIndexerImpl;
import org.netbeans.modules.jackpot30.impl.indexing.Index.Data;
import org.netbeans.modules.jackpot30.impl.indexing.Index.DuplicatesIndexRecord;
import org.netbeans.modules.jackpot30.impl.indexing.Index.MultiData;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
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

    public Collection<? extends DuplicateDescription> computeDuplicatesForAllOpenedProjects() throws IOException {
        List<IndexReader> readers = new LinkedList<IndexReader>();
        Map<IndexingStorage, FileObject> storages = new IdentityHashMap<IndexingStorage, FileObject>();
        
        for (URL u : CustomIndexerImpl.indices) { //XXX: synchronization
            try {
                FileObject fd = CacheFolder.getDataFolder(u);
                File cacheRoot = new File(FileUtil.toFile(fd), Cache.NAME + "/" + Cache.VERSION);

                if (new File(cacheRoot, "duplicates/objects").exists()) {
                    IndexingStorage s = Strast.createIndexingStorage(new File(cacheRoot, "duplicates/objects"), new File(cacheRoot, "duplicates/index"));

                    readers.add(s.getQueries().getIndexReader("di"));
                    storages.put(s, URLMapper.findFileObject(u));
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
        MultiReader r = new MultiReader(readers.toArray(new IndexReader[0]));

        Set<String> of2 = new TreeSet<String>(new Comparator<String>() {
            public int compare(String arg0, String arg1) {
                long value0 = Long.parseLong(arg0.substring(arg0.lastIndexOf(":") + 1));
                long value1 = Long.parseLong(arg1.substring(arg1.lastIndexOf(":") + 1));
                
                return (int) Math.signum(value1 - value0);
            }
        });
        
        CONT: for (Frequency f : getFieldValueFrequencies(r, "data", FieldConversion.NONE)) {
            if (f.frequency > 1) {
                for (Iterator<String> it = of2.iterator(); it.hasNext(); )  {
                    String n = stripValue(it.next());
                    String fValue = stripValue(f.value);

                    if (n.contains(fValue)) {
                        continue CONT;
                    }
                    if (fValue.contains(n)) {
                        it.remove();
                    }
                }
                of2.add(f.value);
            }
        }

        List<String> dd = new ArrayList<String>(of2);

        //TODO: only show valuable duplicates?:
//        dd = dd.subList(0, dd.size() / 10 + 1);

        List<DuplicateDescription> result = new LinkedList<DuplicateDescription>();

        for (String longest : dd) {
            List<Span> foundDuplicates = new LinkedList<Span>();

            for (Entry<IndexingStorage, FileObject> e : storages.entrySet()) {
                for (Document doc : e.getKey().getQueries().fieldQuery("di", "data", longest, Search.FieldQueryType.EXACT)) {
                    String relPath = doc.getField("strast.path").stringValue();
                    DuplicatesIndexRecord dir = e.getKey().get(DuplicatesIndexRecord.class, relPath.split("/"));

                    int index = (int) (long) (Long) (Object) dir.key2Index.get(longest);
                    MultiData md = dir.index2Data[index];

                    for (Data d : md.data) {
                        Span span = Span.of(e.getValue().getFileObject(relPath), d);

                        if (span != null)
                            foundDuplicates.add(span);
                    }
                }
            }

            if (foundDuplicates.size() >= 2)
                result.add(DuplicateDescription.of(foundDuplicates));
        }

        return result;
    }

    private static List<Frequency> getFieldValueFrequencies(IndexReader ir, String field, Lutz.FieldConversion conversion) throws IOException {
        conversion = conversion == null ? Lutz.FieldConversion.NONE : conversion;

        List<Frequency> values = new ArrayList<Frequency>();
        TermEnum terms = ir.terms( new Term(field));
        //while (terms.next()) {
        do {
            final Term term =  terms.term();

            if ( !field.equals( term.field() ) ) {
                break;
            }

            Frequency f = new Frequency();
            f.value = conversion.decode(term.text());
            f.frequency = terms.docFreq();

            values.add(f);
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

        public static @CheckForNull Span of(FileObject file, Data d) {
            try {
                if (d.start == (-1) || d.end == (-1)) return null; //XXX
                
                DataObject od = DataObject.find(file);
                CloneableEditorSupport ces = (CloneableEditorSupport) od.getLookup().lookup(EditorCookie.class);

                PositionRef start = ces.createPositionRef((int) d.start, Bias.Forward);
                PositionRef end   = ces.createPositionRef((int) d.end,   Bias.Backward);

                return new Span(file, new PositionBounds(start, end));
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
        }

    }
}
