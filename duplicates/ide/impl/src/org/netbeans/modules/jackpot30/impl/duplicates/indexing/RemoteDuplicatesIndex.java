/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.duplicates.indexing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleEntry;
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.FSDirectory;
import org.codeviation.pojson.Pojson;
import org.netbeans.modules.jackpot30.common.api.IndexAccess.NoAnalyzer;
import org.netbeans.modules.jackpot30.common.api.LuceneHelpers.BitSetCollector;
import org.netbeans.modules.jackpot30.impl.duplicates.ComputeDuplicates.DuplicateDescription;
import org.netbeans.modules.jackpot30.impl.duplicates.ComputeDuplicates.Span;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
@SuppressWarnings("ClassWithMultipleLoggers")
public class RemoteDuplicatesIndex {

    private static final Logger LOG = Logger.getLogger(RemoteDuplicatesIndex.class.getName());
    private static final Logger TIMER = Logger.getLogger("TIMER");

    public static List<DuplicateDescription> findDuplicates(Map<String, long[]> hashes, FileObject currentFile) throws IOException, URISyntaxException {
        return translate(hashes, findHashOccurrences(hashes.keySet(), currentFile), currentFile);
    }

    private static Map<String, Map<RemoteIndex, Collection<String>>> findHashOccurrences(Collection<? extends String> hashes, FileObject currentFile) throws IOException, URISyntaxException {
        Map<URI, Collection<RemoteIndex>> indices = new LinkedHashMap<URI, Collection<RemoteIndex>>();

        for (RemoteIndex ri : RemoteIndex.loadIndices()) {
            try {
                URI uri = ri.remote.toURI();
                Collection<RemoteIndex> list = indices.get(uri);

                if (list == null) {
                    indices.put(uri, list = new ArrayList<RemoteIndex>());
                }

                list.add(ri);
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        Map<String, Map<RemoteIndex, Collection<String>>> result = new LinkedHashMap<String, Map<RemoteIndex, Collection<String>>>();
        long localTime = 0;
        long remoteTime = 0;

        for (RemoteIndex ri : RemoteIndex.loadIndices()) {
            Set<String> toProcess = new LinkedHashSet<String>(hashes);
            Map<String, Map<String, Collection<? extends String>>> indexResult = new LinkedHashMap<String, Map<String, Collection<? extends String>>>();

            long locS = System.currentTimeMillis();
            indexResult.putAll(findHashOccurrencesInLocalCache(ri, toProcess));
            localTime += System.currentTimeMillis() - locS;

            toProcess.removeAll(indexResult.keySet());

            if (!toProcess.isEmpty()) {
                long remS = System.currentTimeMillis();
                Map<String, Map<String, Collection<? extends String>>> remoteResults = findHashOccurrencesRemote(ri.remote.toURI(), toProcess);
                remoteTime += System.currentTimeMillis() - remS;

                Map<String, Map<String, Collection<? extends String>>> toSave = new LinkedHashMap<String, Map<String, Collection<? extends String>>>(remoteResults);

                for (String hash : toProcess) {
                    if (!toSave.containsKey(hash)) {
                        toSave.put(hash, Collections.<String, Collection<? extends String>>emptyMap());
                    }
                }

                saveToLocalCache(ri, toSave);

                indexResult.putAll(remoteResults);
            }

            for (Entry<String, Map<String, Collection<? extends String>>> e : indexResult.entrySet()) {
                Map<RemoteIndex, Collection<String>> hashResult = result.get(e.getKey());

                if (hashResult == null) {
                    result.put(e.getKey(), hashResult = new LinkedHashMap<RemoteIndex, Collection<String>>());
                }

                for (Entry<String, Collection<? extends String>> insideHash : e.getValue().entrySet()) {
                    Collection<String> dupes = hashResult.get(ri);

                    if (dupes == null) {
                        hashResult.put(ri, dupes = new HashSet<String>());
                    }

                    dupes.addAll(insideHash.getValue());
                }
            }
        }

        TIMER.log(Level.FINE, "local hash duplicates", new Object[] {currentFile, localTime});
        TIMER.log(Level.FINE, "remote hash duplicates", new Object[] {currentFile, remoteTime});
        return result;
    }

    private static Map<String, Map<String, Collection<? extends String>>> findHashOccurrencesRemote(URI remoteIndex, Iterable<? extends String> hashes) {
        try {
            String indexURL = remoteIndex.toASCIIString();
            URI u = new URI(indexURL + "/duplicates/findDuplicates?hashes=" + WebUtilities.escapeForQuery(Pojson.save(hashes)));
            String hashesMap = WebUtilities.requestStringResponse(u);

            if (hashesMap == null) {
                //some kind of error while getting the duplicates (cannot access remote server)?
                //ignore:
                return Collections.emptyMap();
            }
            return Pojson.load(LinkedHashMap.class, hashesMap);
        } catch (URISyntaxException ex) {
            //XXX: better handling?
            Exceptions.printStackTrace(ex);
            return Collections.emptyMap();
        }
    }

    private static final Map<URI, IndexReader> readerCache = new HashMap<URI, IndexReader>();

    private static File findLocalCacheDir(RemoteIndex ri) throws IOException {
        return new File(FileUtil.toFile(FileUtil.createFolder(CacheFolder.getDataFolder(ri.remote), "remote-duplicates")), ri.remoteSegment);
    }

    private static final long VERSION_CHECK_PERIOD = 60 * 60 * 1000;
    private static final Map<Entry<URI, String>, Long> lastVersionCheck = new HashMap<Entry<URI, String>, Long>();
    
    private static synchronized Map<String, Map<String, Collection<? extends String>>> findHashOccurrencesInLocalCache(RemoteIndex ri, Iterable<? extends String> hashes) throws IOException, URISyntaxException {
        URI uri = ri.remote.toURI();
        SimpleEntry<URI, String> versionCheckKey = new SimpleEntry<URI, String>(uri, ri.remoteSegment);
        Long lastCheck = lastVersionCheck.get(versionCheckKey);

        if (lastCheck == null || (System.currentTimeMillis() - lastCheck) > VERSION_CHECK_PERIOD) {
            File dir = findLocalCacheDir(ri);
            File remoteVersion = new File(dir, "remoteVersion");
            FileObject remoteVersionFO = FileUtil.toFileObject(remoteVersion);
            String previousVersion = remoteVersionFO != null ? remoteVersionFO.asText("UTF-8") : null;
            URI infoURI = new URI(ri.remote.toExternalForm() + "/info?path=" + WebUtilities.escapeForQuery(ri.remoteSegment));
            String infoContent = WebUtilities.requestStringResponse(infoURI);

            if (infoContent != null) {
                Object buildId = Pojson.load(LinkedHashMap.class, infoContent).get("BUILD_ID");

                if (buildId != null && !(buildId = buildId.toString()).equals(previousVersion)) {
                    remoteVersion.getParentFile().mkdirs();
                    OutputStream out = new FileOutputStream(remoteVersion);
                    try {
                        out.write(buildId.toString().getBytes("UTF-8"));
                    } finally {
                        out.close();
                    }

                    LOG.log(Level.FINE, "Deleting local cache");
                    delete(new File(dir, "index"));

                    IndexReader reader = readerCache.remove(uri);
                    if (reader != null)
                        reader.close();
                    
                }
            }

            lastVersionCheck.put(versionCheckKey, System.currentTimeMillis());
        }

        IndexReader reader = readerCache.get(uri);

        if (reader == null) {
            File dir = new File(findLocalCacheDir(ri), "index");

            if (dir.listFiles() != null && dir.listFiles().length > 0) {
                readerCache.put(uri, reader = IndexReader.open(FSDirectory.open(dir), true));
            }
        }

        if (reader == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Collection<String>>> result = new LinkedHashMap<String, Map<String, Collection<String>>>();

        for (Entry<String, Collection<? extends String>> e : containsHash(reader, hashes).entrySet()) {
            Map<String, Collection<String>> forHash = result.get(e.getKey());

            if (forHash == null) {
                result.put(e.getKey(), forHash = new LinkedHashMap<String, Collection<String>>());
            }

            for (String path : e.getValue()) {
                String segment = path.substring(0, path.indexOf('/'));

                path = path.substring(path.indexOf('/') + 1);

                Collection<String> list = forHash.get(segment);

                if (list == null) {
                    forHash.put(segment, list = new LinkedList<String>());
                }

                list.add(path);
            }
        }

        return (Map)result; //XXX
    }

    private static synchronized void saveToLocalCache(RemoteIndex ri, Map<String, Map<String, Collection<? extends String>>> what) throws IOException, URISyntaxException {
        IndexReader r = readerCache.remove(ri.remote.toURI());

        if (r != null) {
            r.close();
        }
        
        IndexWriter w = new IndexWriter(FSDirectory.open(new File(findLocalCacheDir(ri), "index")), new NoAnalyzer(), MaxFieldLength.UNLIMITED);

        for (Entry<String, Map<String, Collection<? extends String>>> e : what.entrySet()) {
            Document doc = new Document();

            doc.add(new Field("hash", e.getKey(), Store.YES, Index.NOT_ANALYZED));

            for (Entry<String, Collection<? extends String>> pe : e.getValue().entrySet()) {
                for (String path : pe.getValue()) {
                    doc.add(new Field("path", pe.getKey() + "/" + path, Store.YES, Index.NO));
                }
            }

            w.addDocument(doc);
        }

        w.optimize();
        w.close();
    }
    
    private static List<DuplicateDescription> translate(Map<String, long[]> hashes, Map<String, Map<RemoteIndex, Collection<String>>> occ, FileObject currentFile) {
        Map<String, Map<RemoteIndex, Collection<String>>> sorted = hashMap();
        Map<long[], DuplicateDescription> result = new LinkedHashMap<long[], DuplicateDescription>();
        List<long[]> seen = new LinkedList<long[]>();

        sorted.putAll(occ);

        OUTER: for (Entry<String, Map<RemoteIndex, Collection<String>>> e : occ.entrySet()) {
            long[] currentSpan = hashes.get(e.getKey());

            for (Iterator<Entry<long[], DuplicateDescription>> it = result.entrySet().iterator(); it.hasNext();) {
                Entry<long[], DuplicateDescription> span = it.next();

                if (span.getKey()[0] <= currentSpan[0] && span.getKey()[1] >= currentSpan[1]) {
                    continue OUTER;
                }

                if (currentSpan[0] <= span.getKey()[0] && currentSpan[1] >= span.getKey()[1]) {
                    it.remove();
                }
            }

            if (currentSpan[0] == (-1) || currentSpan[1] == (-1)) continue;
            
            seen.add(currentSpan);
            
            String longest = e.getKey();
            List<Span> foundDuplicates = new LinkedList<Span>();

            for (Entry<RemoteIndex, Collection<String>> root2Occurrences : e.getValue().entrySet()) {
                FileObject localRoot = URLMapper.findFileObject(root2Occurrences.getKey().getLocalFolder());

                for (String cand : root2Occurrences.getValue()) {
                    FileObject o = localRoot.getFileObject(cand);

                    if (o == null) continue; //XXX log!
                    if (areEquivalent(currentFile, o)) continue;
                    
                    foundDuplicates.add(new Span(o, -1, -1));
                }
            }

            if (foundDuplicates.isEmpty()) continue;
            
            DuplicateDescription current = DuplicateDescription.of(foundDuplicates, getValue(longest), longest);

            result.put(currentSpan, current);
        }

        return new LinkedList<DuplicateDescription>(result.values());
    }

    private static boolean areEquivalent(FileObject f1, FileObject f2) {
        return f1.equals(f2);
    }

    private static long getValue(String encoded) {
        return Long.parseLong(encoded.substring(encoded.lastIndexOf(":") + 1));
    }

    private static <T> TreeMap<String, T> hashMap() {
        return new TreeMap<String, T>(new Comparator<String>() {
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

    public static Map<String, Collection<? extends String>> containsHash(IndexReader reader, Iterable<? extends String> hashes) throws IOException {
        Map<String, Collection<? extends String>> result = new LinkedHashMap<String, Collection<? extends String>>();

        for (String hash : hashes) {
            Collection<String> found = new LinkedList<String>();
            Query query = new TermQuery(new Term("hash", hash));
            Searcher s = new IndexSearcher(reader);
            BitSet matchingDocuments = new BitSet(reader.maxDoc());
            Collector c = new BitSetCollector(matchingDocuments);

            s.search(query, c);

            boolean wasFound = false;

            for (int docNum = matchingDocuments.nextSetBit(0); docNum >= 0; docNum = matchingDocuments.nextSetBit(docNum + 1)) {
                final Document doc = reader.document(docNum);

                found.addAll(Arrays.asList(doc.getValues("path")));
                wasFound = true;
            }

            if (wasFound) {
                result.put(hash, found);
            }
        }

        return result;
    }

    private static void delete(File file) {
        File[] c = file.listFiles();

        if (c != null) {
            for (File cc : c) {
                delete(cc);
            }
        }

        file.delete();
    }
}
