/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.batch;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.java.editor.semantic.SemanticHighlighter;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class BatchUtilities {

    private static final Logger LOG = Logger.getLogger(BatchUtilities.class.getName());
    
    public static Collection<? extends ModificationResult> applyFixes(BatchResult candidates, @NonNull ProgressHandleWrapper progress, AtomicBoolean cancel, Collection<? super MessageImpl> problems) {
        ProgressHandleWrapper innerProgress = progress.startNextPartWithEmbedding(60, 5, 35);
        
        Map<FileObject, Collection<ErrorDescription>> file2eds = new HashMap<FileObject, Collection<ErrorDescription>>();
        
        innerProgress.startNextPart(candidates.projectId2Resources.size());
        
        for (Iterable<? extends Resource> it : candidates.projectId2Resources.values()) {
            BatchSearch.getVerifiedSpans(it, problems);

            for (Resource r : it) {
                List<ErrorDescription> eds = new LinkedList<ErrorDescription>();

                Iterable<? extends ErrorDescription> current = r.getVerifiedSpans(problems);

                if (current == null) {
                    //XXX: warn?
                    continue;
                }

                for (ErrorDescription ed : current) {
                    eds.add(ed);
                }

                if (!eds.isEmpty()) {
                    file2eds.put(r.getResolvedFile(), eds);
                }
            }

            innerProgress.tick();
        }

        Map<FileObject, List<JavaFix>> file2Fixes = new HashMap<FileObject, List<JavaFix>>();

        innerProgress.startNextPart(file2eds.size());
        
        for (final Entry<FileObject, Collection<ErrorDescription>> e : file2eds.entrySet()) {
            LinkedList<JavaFix> fixes = new LinkedList<JavaFix>();

            file2Fixes.put(e.getKey(), fixes);

            for (ErrorDescription ed : e.getValue()) {
                if (!ed.getFixes().isComputed()) {
                    throw new IllegalStateException();//TODO: should be problem
                }

                if (ed.getFixes().getFixes().size() != 1) {
                    if (ed.getFixes().getFixes().isEmpty()) {
                        problems.add(new MessageImpl(MessageKind.WARNING, "No fix for: " + ed.getDescription() + " at " + positionToString(ed) + "."));
                        continue;
                    }
                    
                    problems.add(new MessageImpl(MessageKind.WARNING, "More than one fix for: " + ed.getDescription() + " at " + positionToString(ed) + ", only the first one will be used."));
                }

                Fix f = ed.getFixes().getFixes().get(0);

                if (!(f instanceof JavaFixImpl)) {
                    throw new IllegalStateException();//TODO: should be problem
                }


                fixes.add(((JavaFixImpl) f).jf);
            }

            innerProgress.tick();
        }

        return BatchUtilities.performFastFixes(file2Fixes, innerProgress, cancel);
    }

    private static String positionToString(ErrorDescription ed) {
        try {
            return ed.getFile().getNameExt() + ":" + ed.getRange().getBegin().getLine();
        } catch (IOException ex) {
            LOG.log(Level.FINE, null, ex);
            return ed.getFile().getNameExt();
        }
    }

    public static void removeUnusedImports(Collection<? extends FileObject> files) throws IOException {
        Map<ClasspathInfo, Collection<FileObject>> sortedFastFiles = sortFiles(files);

        for (Entry<ClasspathInfo, Collection<FileObject>> e : sortedFastFiles.entrySet()) {
            JavaSource.create(e.getKey(), e.getValue()).runModificationTask(new RemoveUnusedImports()).commit();
        }
    }

    private static final class RemoveUnusedImports implements Task<WorkingCopy> {
        public void run(WorkingCopy wc) throws IOException {
            Document doc = wc.getSnapshot().getSource().getDocument(true);
            
            if (wc.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0) {
                return;
            }

            //compute imports to remove:
            List<TreePathHandle> unusedImports = SemanticHighlighter.computeUnusedImports(wc);
            CompilationUnitTree cut = wc.getCompilationUnit();
            // make the changes to the source
            for (TreePathHandle handle : unusedImports) {
                TreePath path = handle.resolve(wc);
                assert path != null;
                cut = wc.getTreeMaker().removeCompUnitImport(cut,
                        (ImportTree) path.getLeaf());
            }

            if (!unusedImports.isEmpty()) {
                wc.rewrite(wc.getCompilationUnit(), cut);
            }
        }
    }

    private static List<ModificationResult> performFastFixes(Map<FileObject, List<JavaFix>> fastFixes, @NonNull ProgressHandleWrapper handle, AtomicBoolean cancel) {
        ProgressHandleWrapper innerProgress = handle.startNextPartWithEmbedding(20, 80);

        innerProgress.startNextPart(1);

        Map<ClasspathInfo, Collection<FileObject>> sortedFilesForFixes = sortFiles(fastFixes.keySet());

        innerProgress.tick();
        
        List<ModificationResult> results = new LinkedList<ModificationResult>();

        innerProgress.startNextPart(fastFixes.size());

        for (Entry<ClasspathInfo, Collection<FileObject>> e : sortedFilesForFixes.entrySet()) {
            if (cancel.get()) return null;
            
            Map<FileObject, List<JavaFix>> filtered = new HashMap<FileObject, List<JavaFix>>();

            for (FileObject f : e.getValue()) {
                filtered.put(f, fastFixes.get(f));
            }

            ModificationResult r = performFastFixes(e.getKey(), filtered, innerProgress, cancel);
            
            if (r != null) {
                results.add(r);
            }
        }

        return results;
    }

    private static ModificationResult performFastFixes(ClasspathInfo cpInfo, final Map<FileObject, List<JavaFix>> toProcess, @NullAllowed final ProgressHandleWrapper handle, final AtomicBoolean cancel) {
        JavaSource js = JavaSource.create(cpInfo, toProcess.keySet());

        try {
            return js.runModificationTask(new Task<WorkingCopy>() {
                public void run(WorkingCopy wc) throws Exception {
                    if (cancel.get()) return ;
                    
                    if (wc.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                        return ;

                    for (JavaFix f : toProcess.get(wc.getFileObject())) {
                        if (cancel.get()) return ;
                        
                        JavaFixImpl.Accessor.INSTANCE.process(f, wc, false);
                    }

                    if (handle != null) {
                        handle.tick();
                    }
                }
            });
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    public static Collection<FileObject> getSourceGroups(Iterable<? extends Project> prjs) {
        List<FileObject> result = new LinkedList<FileObject>();
        
        for (Project p : prjs) {
            Sources s = ProjectUtils.getSources(p);

            for (SourceGroup sg : s.getSourceGroups("java")) {
                result.add(sg.getRootFolder());
            }
        }

        return result;
    }

    public static Map<ClasspathInfo, Collection<FileObject>> sortFiles(Collection<? extends FileObject> from) {
        Map<List<ClassPath>, Collection<FileObject>> m = new HashMap<List<ClassPath>, Collection<FileObject>>();

        for (FileObject f : from) {
            List<ClassPath> cps = new ArrayList<ClassPath>(3);

            cps.add(ClassPath.getClassPath(f, ClassPath.BOOT));
            cps.add(ClassPath.getClassPath(f, ClassPath.COMPILE));
            cps.add(ClassPath.getClassPath(f, ClassPath.SOURCE));

            Collection<FileObject> files = m.get(cps);

            if (files == null) {
                m.put(cps, files = new LinkedList<FileObject>());
            }

            files.add(f);
        }

        Map<ClasspathInfo, Collection<FileObject>> result = new HashMap<ClasspathInfo, Collection<FileObject>>();

        for (Entry<List<ClassPath>, Collection<FileObject>> e : m.entrySet()) {
            result.put(ClasspathInfo.create(e.getKey().get(0), e.getKey().get(1), e.getKey().get(2)), e.getValue());
        }

        return result;
    }

}
