package org.netbeans.modules.jackpot30.impl.indexing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer.Result;

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

    public static Index get(URL sourceRoot) throws IOException {
        return new Index(sourceRoot, Cache.findCacheRoot(sourceRoot)); //XXX: new!
    }

    public IndexWriter openForWriting() throws IOException {
        return new IndexWriter();
    }

    public Collection<? extends String> findCandidates(Result r) throws IOException {
        Collection<? extends String> candidates = findCandidatesFromLucene(r);

        if (candidates.isEmpty()) {
            return candidates;
        }

        BulkPattern pattern = BulkSearch.create(r);

        for (Iterator<? extends String> it = candidates.iterator(); it.hasNext();) {
            String relative = it.next();
            StringBuilder text = new StringBuilder();
            Reader reader = new InputStreamReader(new FileInputStream(new File(new File(cacheRoot, "encoded"), relative)), "UTF-8");
            int read;

            while ((read = reader.read()) != (-1)) {
                text.append((char) read);
            }

            if (!BulkSearch.matches(text.toString(), pattern)) {
                it.remove();
            }
        }

        return candidates;
    }

    private Collection<? extends String> findCandidatesFromLucene(Result r) throws IOException {
        IndexReader reader = IndexReader.open(new File(cacheRoot, "fulltext"));
        Searcher s = new IndexSearcher(reader);

        Hits hits;
        try {
            hits = s.search(query(r));
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

    private Query query(Result r) throws ParseException {
        BooleanQuery q = new BooleanQuery();
        
        if (!r.identifiers.isEmpty()) {
            for (String a : r.identifiers) {
                q.add(new TermQuery(new Term("identifiers", a)), BooleanClause.Occur.MUST);
            }
        }
        if (!r.treeKinds.isEmpty()) {
            for (String t : r.treeKinds) {
                q.add(new TermQuery(new Term("treeKinds", t)), BooleanClause.Occur.MUST);
            }
        }

        return q;
    }
    
    public class IndexWriter {

        private final org.apache.lucene.index.IndexWriter luceneWriter;

        public IndexWriter() throws IOException {
            luceneWriter = new org.apache.lucene.index.IndexWriter(new File(cacheRoot, "fulltext"), new StandardAnalyzer());
        }

        public void record(URL source, Result r) throws IOException {
            String relative = source.getPath().substring(stripLength);
            Writer w = null;
            
            try {
                File cacheFile = new File(new File(cacheRoot, "encoded"), relative);

                cacheFile.getParentFile().mkdirs();

                w = new OutputStreamWriter(new FileOutputStream(cacheFile), "UTF-8");
                w.write(r.encoded);
            } finally {
                if (w != null) {
                    w.close();
                }
            }

            Document doc = new Document();

            doc.add(new Field("identifiers", new TokenStreamImpl(r.identifiers)));
            doc.add(new Field("treeKinds", new TokenStreamImpl(r.treeKinds)));
            doc.add(new Field("path", relative, Field.Store.YES, Field.Index.NO));

            luceneWriter.addDocument(doc);
        }

        public void close() throws IOException {
            luceneWriter.optimize();
            luceneWriter.close();
        }
    }

    private static final class TokenStreamImpl extends TokenStream {

        private final Iterator<String> tokens;

        public TokenStreamImpl(Iterable<String> tokens) {
            this.tokens = tokens.iterator();
        }

        @Override
        public Token next() throws IOException {
            if (!tokens.hasNext())
                return null;

            String t = tokens.next();

            return new Token(t, 0, t.length());
        }
    }
}
