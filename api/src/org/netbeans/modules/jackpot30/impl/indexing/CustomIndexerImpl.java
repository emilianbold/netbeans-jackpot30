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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.jackpot30.impl.indexing.Index.AttributionWrapper;
import org.netbeans.modules.jackpot30.impl.indexing.Index.IndexWriter;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class CustomIndexerImpl extends CustomIndexer {

    @Override
    protected void index(Iterable<? extends Indexable> files, Context context) {
        Collection<FileObject> toIndex = new LinkedList<FileObject>(); //XXX: better would be to use File

        for (Indexable i : files) {
            FileObject f = URLMapper.findFileObject(i.getURL());

            if (f != null) {
                toIndex.add(f);
            }
        }

        if (toIndex.isEmpty()) {
            return ;
        }
        
        try {
            doIndex(context.getRoot(), toIndex, Collections.<String>emptyList(), FileBasedIndex.get(context.getRootURI()));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static void doIndex(FileObject root, Collection<? extends FileObject> toIndex, Iterable<? extends String> deleted, Index index) {
        if (toIndex.isEmpty() && !deleted.iterator().hasNext()) {
            return ;
        }

        final IndexWriter[] w = new IndexWriter[1];

        try {
            ClasspathInfo cpInfo = ClasspathInfo.create(root);
            w[0] = index.openForWriting();

            for (String path : deleted) {
                w[0].remove(path);
            }

            if (!toIndex.isEmpty()) {
                JavaSource.create(cpInfo, toIndex).runUserActionTask(new Task<CompilationController>() {
                    public void run(final CompilationController cc) throws Exception {
                        if (cc.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0)
                            return ;

                        w[0].record(cc.getFileObject().getURL(), cc.getCompilationUnit(), null);
                    }
                }, true);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (w[0] != null) {
                try {
                    w[0].close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    public static final class CustomIndexerFactoryImpl extends CustomIndexerFactory {

        @Override
        public CustomIndexer createIndexer() {
            return new CustomIndexerImpl();
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            Collection<String> deletedPaths = new LinkedList<String>();

            for (Indexable i : deleted) {
                deletedPaths.add(i.getRelativePath());

            }
            try {
                doIndex(context.getRoot(), Collections.<FileObject>emptyList(), deletedPaths, FileBasedIndex.get(context.getRootURI()));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {}

        @Override
        public String getIndexerName() {
            return FileBasedIndex.NAME;
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return false;
        }

        @Override
        public int getIndexVersion() {
            return FileBasedIndex.VERSION;
        }

    }

}
