/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.batch;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.diff.builtin.provider.BuiltInDiffProvider;
import org.netbeans.modules.diff.builtin.visualizer.TextDiffVisualizer;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.jackpot30.impl.batch.JavaFixImpl.Accessor;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.jackpot30.spi.ProjectDependencyUpgrader;
import org.netbeans.modules.java.editor.semantic.SemanticHighlighter;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.java.source.parsing.CompilationInfoImpl;
import org.netbeans.modules.java.source.save.ElementOverlay;
import org.netbeans.spi.diff.DiffProvider;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
public class BatchUtilities {

    private static final Logger LOG = Logger.getLogger(BatchUtilities.class.getName());
    
    public static Collection<ModificationResult> applyFixes(BatchResult candidates, @NonNull ProgressHandleWrapper progress, AtomicBoolean cancel, final Collection<? super MessageImpl> problems) {
        final Map<Project, Set<String>> processedDependencyChanges = new IdentityHashMap<Project, Set<String>>();
        final Map<FileObject, List<ModificationResult.Difference>> result = new LinkedHashMap<FileObject, List<ModificationResult.Difference>>();

        BatchSearch.VerifiedSpansCallBack callback = new BatchSearch.VerifiedSpansCallBack() {
            private ElementOverlay overlay;
            public void groupStarted() {
                overlay = new ElementOverlay();
            }
            public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                Constructor<WorkingCopy> wcConstr = WorkingCopy.class.getDeclaredConstructor(CompilationInfoImpl.class, ElementOverlay.class);
                wcConstr.setAccessible(true);

//                final WorkingCopy copy = new WorkingCopy(JavaSourceAccessor.getINSTANCE().getCompilationInfoImpl(parameter), overlay);
                WorkingCopy copy = wcConstr.newInstance(JavaSourceAccessor.getINSTANCE().getCompilationInfoImpl(wc), overlay);
                Method setJavaSource = CompilationInfo.class.getDeclaredMethod("setJavaSource", JavaSource.class);
                setJavaSource.setAccessible(true);

//                copy.setJavaSource(JavaSource.this);
                setJavaSource.invoke(copy, wc.getJavaSource());

                copy.toPhase(Phase.RESOLVED);
                
                if (applyFixes(copy, processedDependencyChanges, hints, problems)) {
                    return false;
                }

                final JavacTaskImpl jt = JavaSourceAccessor.getINSTANCE().getJavacTask(copy);
                Log.instance(jt.getContext()).nerrors = 0;
                Method getChanges = WorkingCopy.class.getDeclaredMethod("getChanges", Map.class);
                getChanges.setAccessible(true);

                result.put(copy.getFileObject(), (List<ModificationResult.Difference>) getChanges.invoke(copy, new HashMap<Object, int[]>()));

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "fixes applied to: {0}", FileUtil.getFileDisplayName(wc.getFileObject()));
                }

                return true;
            }

            public void groupFinished() {
                overlay = null;
            }

            public void cannotVerifySpan(Resource r) {
                problems.add(new MessageImpl(MessageKind.WARNING, "Cannot parse: " + r.getRelativePath()));
            }
        };

        BatchSearch.getVerifiedSpans(candidates, progress, callback, problems);
        
        return Collections.singletonList(JavaSourceAccessor.getINSTANCE().createModificationResult(result, Collections.<Object, int[]>emptyMap()));
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

    public static boolean applyFixes(WorkingCopy copy, Map<Project, Set<String>> processedDependencyChanges, Collection<? extends ErrorDescription> hints, Collection<? super MessageImpl> problems) throws IllegalStateException, Exception {
        List<JavaFix> fixes = new ArrayList<JavaFix>();
        for (ErrorDescription ed : hints) {
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
        if (fixDependencies(copy.getFileObject(), fixes, processedDependencyChanges)) {
            return true;
        }
        for (JavaFix f : fixes) {
//                    if (cancel.get()) return ;

            JavaFixImpl.Accessor.INSTANCE.process(f, copy, false);
        }
        return false;
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
        Map<List<Object>, Collection<FileObject>> m = new HashMap<List<Object>, Collection<FileObject>>();

        for (FileObject f : from) {
            List<Object> cps = new ArrayList<Object>(4);

            cps.add(ClassPath.getClassPath(f, ClassPath.BOOT));
            cps.add(ClassPath.getClassPath(f, ClassPath.COMPILE));

            ClassPath sourceCP = ClassPath.getClassPath(f, ClassPath.SOURCE);

            cps.add(sourceCP);
            cps.add(sourceCP != null ? sourceCP.findOwnerRoot(f) : null);

            Collection<FileObject> files = m.get(cps);

            if (files == null) {
                m.put(cps, files = new LinkedList<FileObject>());
            }

            files.add(f);
        }

        Map<ClasspathInfo, Collection<FileObject>> result = new IdentityHashMap<ClasspathInfo, Collection<FileObject>>();

        for (Entry<List<Object>, Collection<FileObject>> e : m.entrySet()) {
            result.put(ClasspathInfo.create((ClassPath) e.getKey().get(0), (ClassPath) e.getKey().get(1), (ClassPath) e.getKey().get(2)), e.getValue());
        }

        return result;
    }

    public static boolean fixDependencies(FileObject file, List<JavaFix> toProcess, Map<Project, Set<String>> alreadyProcessed) {
        boolean modified = false;
//        for (FileObject file : toProcess.keySet()) {
            for (JavaFix fix : toProcess) {
                String updateTo = Accessor.INSTANCE.getOptions(fix).get(JavaFix.ENSURE_DEPENDENCY);

                if (updateTo != null) {
                    Project p = FileOwnerQuery.getOwner(file);

                    if (p != null) {
                        Set<String> seen = alreadyProcessed.get(p);

                        if (seen == null) {
                            alreadyProcessed.put(p, seen = new HashSet<String>());
                        }

                        if (seen.add(updateTo)) {
                            for (ProjectDependencyUpgrader up : Lookup.getDefault().lookupAll(ProjectDependencyUpgrader.class)) {
                                if (up.ensureDependency(p, updateTo, false)) { //XXX: should check whether the given project was actually modified
                                    modified = true;
                                    break;
                                }
                            }
                            //TODO: fail if cannot update the dependency?
                        }
                    }
                }
            }

            return modified;
//        }
    }

    public static void exportDiff(ModificationResult result, @NullAllowed FileObject relativeTo, Writer out) throws IOException {
        for (FileObject f : result.getModifiedFileObjects()) {
            Charset c = FileEncodingQuery.getEncoding(f);
            String orig = new String(f.asBytes(), c);
            String nue = result.getResultingSource(f);

            if (orig.equals(nue)) {
                continue;
            }

            String name = relativeTo != null ? FileUtil.getRelativePath(relativeTo, f) : FileUtil.toFile(f).getAbsolutePath();
            
            doExportDiff(name, orig, nue, out);
        }
    }

    //copied from the diff module:
    private static void doExportDiff(String name, String original, String modified, Writer out) throws IOException {
        DiffProvider diff = new BuiltInDiffProvider();//(DiffProvider) Lookup.getDefault().lookup(DiffProvider.class);

        Reader r1 = null;
        Reader r2 = null;
        org.netbeans.api.diff.Difference[] differences;

        try {
            r1 = new StringReader(original);
            r2 = new StringReader(modified);
            differences = diff.computeDiff(r1, r2);
        } finally {
            if (r1 != null) try { r1.close(); } catch (Exception e) {}
            if (r2 != null) try { r2.close(); } catch (Exception e) {}
        }

        try {
            r1 = new StringReader(original);
            r2 = new StringReader(modified);
            TextDiffVisualizer.TextDiffInfo info = new TextDiffVisualizer.TextDiffInfo(
                name, // NOI18N
                name,  // NOI18N
                null,
                null,
                r1,
                r2,
                differences
            );
            info.setContextMode(true, 3);
            String diffText;
//            if (format == unifiedFilter) {
                diffText = TextDiffVisualizer.differenceToUnifiedDiffText(info);
//            } else {
//                diffText = TextDiffVisualizer.differenceToNormalDiffText(info);
//            }
            out.write(diffText);
        } finally {
            if (r1 != null) try { r1.close(); } catch (Exception e) {}
            if (r2 != null) try { r2.close(); } catch (Exception e) {}
        }
    }

}
