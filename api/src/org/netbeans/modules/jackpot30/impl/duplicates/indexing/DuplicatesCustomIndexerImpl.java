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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesIndex.IndexWriter;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileAlreadyLockedException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class DuplicatesCustomIndexerImpl extends CustomIndexer {

    public static void updateIndex(final URL root) throws IOException {
        final FileObject rootFO = URLMapper.findFileObject(root);
        final ClasspathInfo cpInfo = ClasspathInfo.create(rootFO);
        
        JavaSource.create(cpInfo).runUserActionTask(new Task<CompilationController>() {
           public void run(CompilationController parameter) throws Exception {
                final IndexWriter[] w = new IndexWriter[1];

                try {
                    File cacheRoot = Cache.findCache(DuplicatesIndex.NAME).findCacheRoot(root);
                    FileObject deletedFile = FileUtil.toFileObject(new File(cacheRoot, "deleted"));
                    Set<String> deletedFiles = deletedFile != null ? new HashSet<String>(deletedFile.asLines("UTF-8")) : Collections.<String>emptySet();

                    FileObject modifiedFile = FileUtil.toFileObject(new File(cacheRoot, "modified"));
                    Set<String> modifiedFiles = modifiedFile != null ? new HashSet<String>(modifiedFile.asLines("UTF-8")) : Collections.<String>emptySet();

                    Set<FileObject> toIndex = new HashSet<FileObject>();

                    for (String r : modifiedFiles) {
                        FileObject f = rootFO.getFileObject(r);

                        if (f != null) {
                            toIndex.add(f);
                        }
                    }

                    if (!toIndex.isEmpty() || !modifiedFiles.isEmpty()) {
                        w[0] = DuplicatesIndex.get(root).openForWriting();

                        for (String r : deletedFiles) {
                            w[0].remove(r);
                        }

                        JavaSource.create(cpInfo, toIndex).runUserActionTask(new Task<CompilationController>() {
                            public void run(final CompilationController cc) throws Exception {
                                if (cc.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                                    return ;

                                w[0].record(cc, cc.getFileObject().getURL(), cc.getCompilationUnit());
                            }
                        }, true);
                    }

                    if (deletedFile != null)
                        deletedFile.delete();
                    if (modifiedFile != null)
                        modifiedFile.delete();
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
        }, true);
    }

    @Override
    protected void index(Iterable<? extends Indexable> files, Context context) {
        update(context.getRootURI(), files, Collections.<Indexable>emptyList());
    }

    private static void dump(File where, Iterable<? extends String> lines) {
        Writer out = null;

        try {
            out = new BufferedWriter(new OutputStreamWriter(FileUtil.createData(where).getOutputStream(), "UTF-8"));
            
            for (String line : lines) {
                out.write(line);
                out.write("\n");
            }
        } catch (FileAlreadyLockedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    private static Set<String> gatherRelativePaths(Iterable<? extends Indexable> it) {
        Set<String> result = new HashSet<String>();

        for (Indexable i : it) {
            result.add(i.getRelativePath());
        }

        return result;
    }

    private static void update(URL root, Iterable<? extends Indexable> modified, Iterable<? extends Indexable> deleted) {
        try {
            Set<String> mod = gatherRelativePaths(modified);
            Set<String> del = gatherRelativePaths(deleted);

            File cacheRoot = Cache.findCache(DuplicatesIndex.NAME).findCacheRoot(root);
            
            File modifiedFile = new File(cacheRoot, "modified");
            FileObject modifiedFileFO = FileUtil.toFileObject(modifiedFile);
            Set<String> modifiedFiles = modifiedFileFO != null ? new HashSet<String>(modifiedFileFO.asLines("UTF-8")) : new HashSet<String>();
            boolean modifiedFilesChanged = modifiedFiles.removeAll(del);

            modifiedFilesChanged |= modifiedFiles.addAll(mod);

            if (modifiedFilesChanged) {
                dump(modifiedFile, modifiedFiles);
            }

            File deletedFile = new File(cacheRoot, "deleted");
            FileObject deletedFileFO = FileUtil.toFileObject(deletedFile);
            Set<String> deletedFiles = deletedFileFO != null ? new HashSet<String>(deletedFileFO.asLines("UTF-8")) : new HashSet<String>();

            boolean deletedFilesChanged = deletedFiles.removeAll(mod);

            deletedFilesChanged |= deletedFiles.addAll(del);

            if (deletedFilesChanged) {
                dump(deletedFile, deletedFiles);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    
    public static final class FactoryImpl extends CustomIndexerFactory {

        @Override
        public CustomIndexer createIndexer() {
            return new DuplicatesCustomIndexerImpl();
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            update(context.getRootURI(), Collections.<Indexable>emptyList(), deleted);
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {}

        @Override
        public String getIndexerName() {
            return DuplicatesIndex.NAME;
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return false;
        }

        @Override
        public int getIndexVersion() {
            return Cache.VERSION;
        }
    }

}
