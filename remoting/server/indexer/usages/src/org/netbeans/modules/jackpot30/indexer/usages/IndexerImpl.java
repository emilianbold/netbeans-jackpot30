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
package org.netbeans.modules.jackpot30.indexer.usages;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePathScanner;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.lang.model.element.Element;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.parsing.lucene.support.DocumentIndex;
import org.netbeans.modules.parsing.lucene.support.IndexDocument;
import org.netbeans.modules.parsing.lucene.support.IndexManager;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexer;
import org.netbeans.modules.parsing.spi.indexing.CustomIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class IndexerImpl extends CustomIndexer {

    private static final String KEY_SIGNATURES = "signatures";
    
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

        doIndex(context, toIndex, Collections.<String>emptyList());
    }

    public static void doIndex(final Context context, Collection<? extends FileObject> toIndex, Iterable<? extends String> deleted) {
        if (toIndex.isEmpty() && !deleted.iterator().hasNext()) {
            return ;
        }

        final DocumentIndex[] idx = new DocumentIndex[1];

        try {
            idx[0] = IndexManager.createDocumentIndex(FileUtil.toFile(context.getIndexFolder()));
            
            ClasspathInfo cpInfo = ClasspathInfo.create(context.getRoot());

            for (String path : deleted) {
                idx[0].removeDocument(path);
            }

            if (!toIndex.isEmpty()) {
                JavaSource.create(cpInfo, toIndex).runUserActionTask(new Task<CompilationController>() {
                    public void run(final CompilationController cc) throws Exception {
                        if (cc.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                            return ;

                        final IndexDocument doc = IndexManager.createDocument(FileUtil.getRelativePath(context.getRoot(), cc.getFileObject()));

                        new TreePathScanner<Void, Void>() {
                            private final Set<String> SEEN_SIGNATURES = new HashSet<String>();
                            @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                                handleNode();
                                return super.visitIdentifier(node, p);
                            }
                            @Override public Void visitMemberSelect(MemberSelectTree node, Void p) {
                                handleNode();
                                return super.visitMemberSelect(node, p);
                            }
                            private void handleNode() {
                                Element el = cc.getTrees().getElement(getCurrentPath());

                                if (el != null && Common.SUPPORTED_KINDS.contains(el.getKind())) {
                                    String serialized = Common.serialize(ElementHandle.create(el));

                                    if (SEEN_SIGNATURES.add(serialized)) {
                                        doc.addPair(KEY_SIGNATURES, serialized, true, true);
                                    }
                                }
                            }
                        }.scan(cc.getCompilationUnit(), null);

                        idx[0].addDocument(doc);
                    }
                }, true);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (idx[0] != null) {
                try {
                    idx[0].store(true);
                    idx[0].close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    @MimeRegistration(mimeType="text/x-java", service=CustomIndexerFactory.class)
    public static final class FactoryImpl extends CustomIndexerFactory {

        @Override
        public CustomIndexer createIndexer() {
            return new IndexerImpl();
        }

        @Override
        public boolean supportsEmbeddedIndexers() {
            return false;
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            Collection<String> deletedPaths = new LinkedList<String>();

            for (Indexable i : deleted) {
                deletedPaths.add(i.getRelativePath());

            }

            doIndex(context, Collections.<FileObject>emptyList(), deletedPaths);
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
        }

        @Override
        public String getIndexerName() {
            return "javausages";
        }

        @Override
        public int getIndexVersion() {
            return 1;
        }

    }

}
