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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.FSDirectory;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public final class FileBasedIndex extends AbstractLuceneIndex {

    public static Index create(URL sourceRoot, File indexRoot) {
        return new FileBasedIndex(sourceRoot, indexRoot, false);
    }

    public static @CheckForNull Index get(URL sourceRoot) throws IOException {
        return new FileBasedIndex(sourceRoot, Cache.findCache(FileBasedIndex.NAME).findCacheRoot(sourceRoot), false); //XXX: new!
    }

    public static @NonNull Index create(URL sourceRoot, boolean storeSources) throws IOException {
        return new FileBasedIndex(sourceRoot, Cache.findCache(FileBasedIndex.NAME).findCacheRoot(sourceRoot), storeSources);
    }

    private final URL  sourceRoot;
    private final File cacheRoot;

    private FileBasedIndex(URL sourceRoot, File cacheRoot, boolean storeSources) {
        super(sourceRoot.getPath().length(), false, storeSources);
        this.sourceRoot = sourceRoot;
        this.cacheRoot = cacheRoot;
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

    @Override
    protected IndexReader createReader() throws IOException {
        File dir = new File(cacheRoot, "fulltext");

        if (dir.listFiles() == null || dir.listFiles().length == 0) {
            return null;
        }

        return IndexReader.open(FSDirectory.open(dir), true);
    }

    @Override
    protected org.apache.lucene.index.IndexWriter createWriter() throws IOException {
        return new org.apache.lucene.index.IndexWriter(FSDirectory.open(new File(cacheRoot, "fulltext")), new NoAnalyzer(), MaxFieldLength.UNLIMITED);
    }

    @Override
    protected void storeIndexInfo(IndexInfo info) throws IOException {
        File infoFile = new File(cacheRoot, "info");

        Pojson.save(info, infoFile);
    }

    public static final class NoAnalyzer extends Analyzer {

        @Override
        public TokenStream tokenStream(String string, Reader reader) {
            throw new UnsupportedOperationException("Should not be called");
        }

    }
    
    public static final String NAME = "jackpot30"; //NOI18N
}
