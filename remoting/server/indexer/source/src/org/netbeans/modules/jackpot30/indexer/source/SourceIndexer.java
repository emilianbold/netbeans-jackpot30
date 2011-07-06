/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.indexer.source;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.CompressionTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.jackpot30.backend.impl.spi.IndexAccessor;
import org.netbeans.modules.parsing.lucene.support.DocumentIndex;
import org.netbeans.modules.parsing.lucene.support.IndexManager;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;

/**
 *
 * @author lahvac
 */
public class SourceIndexer extends CustomIndexer {

    private static final String KEY_CONTENT = "content";

    @Override
    protected void index(Iterable<? extends Indexable> files, Context context) {
        try {
            for (Indexable i : files) {
                FileObject f = URLMapper.findFileObject(i.getURL());

                if (f == null) continue;

                String relPath = IndexAccessor.getCurrent().getPath(f);

                if (relPath == null) continue;
                
                Document doc = new Document();

                doc.add(new Field("relativePath", relPath, Store.YES, Index.NOT_ANALYZED));
                doc.add(new Field(KEY_CONTENT, CompressionTools.compressString(readFully(i.getURL())), Store.YES));

                IndexAccessor.getCurrent().getIndexWriter().addDocument(doc);
            }
        } catch (IOException ex) {
            Logger.getLogger(SourceIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String readFully(URL source) throws IOException {
        Reader in = null;
        StringBuilder result = new StringBuilder();

        try {
            in = new BufferedReader(new InputStreamReader(source.openStream(), "UTF-8"));

            int read;

            while ((read = in.read()) != (-1)) {
                result.append((char) read);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(SourceIndexer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return result.toString();
    }

    @MimeRegistration(mimeType="", service=CustomIndexerFactory.class)
    public static final class FactoryImpl extends CustomIndexerFactory {

        @Override
        public CustomIndexer createIndexer() {
            return new SourceIndexer();
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return true;
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            try {
                DocumentIndex idx = IndexManager.createDocumentIndex(FileUtil.toFile(context.getIndexFolder()));

                for (Indexable i : deleted) {
                    idx.removeDocument(i.getRelativePath());
                }

                idx.close();
            } catch (IOException ex) {
                Logger.getLogger(SourceIndexer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
        }

        @Override
        public String getIndexerName() {
            return "fullsource";
        }

        @Override
        public int getIndexVersion() {
            return 1;
        }

    }
}
