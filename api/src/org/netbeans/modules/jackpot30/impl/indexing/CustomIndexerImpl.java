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

package org.netbeans.modules.jackpot30.impl.indexing;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
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

    public static final Set<URL> indices = new HashSet<URL>(); //XXX
    
    @Override
    protected void index(Iterable<? extends Indexable> files, Context context) {
        indices.add(context.getRootURI());
        final IndexWriter[] w = new IndexWriter[1];
        
        try {
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

            ClasspathInfo cpInfo = ClasspathInfo.create(context.getRoot());
            w[0] = Index.get(context.getRootURI()).openForWriting();

            JavaSource.create(cpInfo, toIndex).runUserActionTask(new Task<CompilationController>() {
                public void run(final CompilationController cc) throws Exception {
                    if (cc.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                        return ;

                    w[0].record(cc, cc.getFileObject().getURL(), cc.getCompilationUnit());
                }
            }, true);
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
            IndexWriter w = null;

            try {
                w = Index.get(context.getRootURI()).openForWriting();

                for (Indexable i : deleted) {
                    w.remove(i.getRelativePath());
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                if (w != null) {
                    try {
                        w.close();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {}

        @Override
        public String getIndexerName() {
            return Cache.NAME;
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return false;
        }

        @Override
        public int getIndexVersion() {
            return Cache.VERSION;
        }

        @Override
        public void rootsRemoved(Iterable<? extends URL> removedRoots) {
            for (URL u : removedRoots) {
                indices.remove(u);
            }
        }
        
    }

}
