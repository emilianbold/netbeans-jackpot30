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
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.java.editor.semantic.SemanticHighlighter;
import org.netbeans.modules.java.hints.options.HintsSettings;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Jan Lahoda
 */
public class BatchApply {

    private static final RequestProcessor WORKER = new RequestProcessor("Batch Hint Apply");
    
    public static String applyFixes(final Lookup context, final List<HintDescription> hints, boolean progress) {
        assert !progress || SwingUtilities.isEventDispatchThread();

        final AtomicBoolean cancel = new AtomicBoolean();
        final ProgressHandle handle = ProgressHandleFactory.createHandle("Batch Hint Apply", new Cancellable() {
            public boolean cancel() {
                cancel.set(true);

                return true;
            }
        });

        try {
            if (progress) {
                    DialogDescriptor dd = new DialogDescriptor(ProgressHandleFactory.createProgressComponent(handle),
                                                               "Batch Hint Apply",
                                                               true,
                                                               new Object[] {DialogDescriptor.CANCEL_OPTION},
                                                               DialogDescriptor.CANCEL_OPTION,
                                                               DialogDescriptor.DEFAULT_ALIGN,
                                                               null,
                                                               new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            cancel.set(true);
                        }
                    });
                    final Dialog d = DialogDisplayer.getDefault().createDialog(dd);
                    final String[] result = new String[1];

                    Runnable exec = new Runnable() {

                        public void run() {
                            result[0] = applyFixesImpl(context, hints, handle, cancel);

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    d.setVisible(false);
                                }
                            });
                        }
                    };

                    WORKER.post(exec);

                    d.setVisible(true);

                    return result[0];
            } else {
                return applyFixesImpl(context, hints, handle, cancel);
            }
        } finally {
            handle.finish();
        }
    }

    private static final ClassPath EMPTY = ClassPathSupport.createClassPath(new FileObject[0]);
    
    private static BulkPattern prepareBulkPattern(final Collection<? extends String> patterns) {
        JavaPlatform select = JavaPlatform.getDefault();

        for (JavaPlatform p : JavaPlatformManager.getDefault().getInstalledPlatforms()) {
            if (p.getSpecification().getVersion().compareTo(select.getSpecification().getVersion()) > 0) {
                select = p;
            }
        }

        ClasspathInfo cpInfo = ClasspathInfo.create(select.getBootstrapLibraries(), EMPTY, EMPTY);
        JavaSource js = JavaSource.create(cpInfo);
        final BulkPattern[] bp = new BulkPattern[1];

        try {
            js.runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController cc) throws Exception {
                    bp[0] = BulkSearch.create(cc, patterns);
                }
            }, true);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        return bp[0];
    }

    private static String applyFixesImpl(Lookup context, List<HintDescription> hints, ProgressHandle h, AtomicBoolean cancel) {
        ProgressHandleWrapper handle = new ProgressHandleWrapper(h, new int[] {20, 40, 40});

        final Map<PatternDescription, List<HintDescription>> patterns = new HashMap<PatternDescription, List<HintDescription>>();

        RulesManager.sortOut(hints, new HashMap<Kind, List<HintDescription>>(), patterns);
        
        final Map<String, List<PatternDescription>> patternTests = HintsInvoker.computePatternTests(patterns);

        BulkPattern bp = prepareBulkPattern(patternTests.keySet());
        List<ErrorDescription> eds = new LinkedList<ErrorDescription>();
        Collection<FileObject> toProcess = toProcess(context);

        handle.startNextPart(toProcess.size());
        
        Collection<FileObject> allSources = findAllSources(toProcess);
        Map<ClasspathInfo, Collection<FileObject>> sortedFiles = sortFiles(allSources);

        handle.startNextPart(allSources.size());

        for (Entry<ClasspathInfo, Collection<FileObject>> e: sortedFiles.entrySet()) {
            if (cancel.get()) return null;
            
            eds.addAll(processFiles(e.getKey(), e.getValue(), bp, patterns, patternTests, handle, cancel));
        }

        Map<ErrorDescription, Fix> fixes = new IdentityHashMap<ErrorDescription, Fix>();
        Map<FileObject, List<JavaFix>> fastFixes = new HashMap<FileObject, List<JavaFix>>();
        List<ErrorDescription> edsWithSlowsFixes = new LinkedList<ErrorDescription>();

        //verify that there is exactly one fix for each ED:
        for (ErrorDescription ed : eds) {
            if (cancel.get()) return null;
            
            if (!ed.getFixes().isComputed()) {
                return "Not computed fixes for: " + ed.getDescription();
            }

            Fix fix = null;

            for (Fix f : ed.getFixes().getFixes()) {
//                if (!(f instanceof SuppressWarningsFixer.FixImpl)) {
                    if (fix != null) {
                        fix = null;
                        break;
                    }

                    fix = f;
//                }
            }

            if (fix == null) {
                return "Not exactly one fix for: " + ed.getDescription() + ", fixes=" + ed.getFixes().getFixes();
            }

            if (fix instanceof JavaFixImpl) {
                JavaFixImpl ajf = (JavaFixImpl) fix;
                FileObject file = JavaFixImpl.Accessor.INSTANCE.getFile(ajf.jf);
                List<JavaFix> fs = fastFixes.get(file);

                if (fs == null) {
                    fastFixes.put(file, fs = new LinkedList<JavaFix>());
                }

                fs.add(ajf.jf);
            } else {
                fixes.put(ed, fix);
                edsWithSlowsFixes.add(ed);
            }
        }

        handle.startNextPart(eds.size());

        try {
            List<ModificationResult> results = performFastFixes(fastFixes, handle, cancel);

            if (cancel.get()) return null;

            performSlowFixes(edsWithSlowsFixes, fixes, handle);

            if (cancel.get()) return null;

            List<FileObject> fastFiles = new LinkedList<FileObject>();

            for (ModificationResult r : results) {
                r.commit();
                fastFiles.addAll(r.getModifiedFileObjects());
            }

            Map<ClasspathInfo, Collection<FileObject>> sortedFastFiles = sortFiles(fastFiles);

            for (Entry<ClasspathInfo, Collection<FileObject>> e : sortedFastFiles.entrySet()) {
                JavaSource.create(e.getKey(), e.getValue()).runModificationTask(new RemoveUnusedImports()).commit();
            }

            LifecycleManager.getDefault().saveAll();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return ex.getLocalizedMessage();
        }
        
        return null;
    }

    private static List<ErrorDescription> processFiles(final ClasspathInfo cpInfo, Collection<FileObject> toProcess, final BulkPattern bulkPattern, final Map<PatternDescription, List<HintDescription>> patterns, final Map<String, List<PatternDescription>> patternTests, final ProgressHandleWrapper handle, final AtomicBoolean cancel) {
        ClassPath sourceCP = cpInfo.getClassPath(PathKind.SOURCE);
        boolean indexed = GlobalPathRegistry.getDefault().getSourceRoots().containsAll(sourceCP.entries());
        List<ErrorDescription> result = new LinkedList<ErrorDescription>();

        if (!indexed) {
            Set<FileObject> done = new HashSet<FileObject>();

            result.addAll(doProcessFiles(cpInfo, toProcess, bulkPattern, patterns, patternTests, handle, cancel, indexed, done));

            if (toProcess.size() == done.size()) {
                return result;
            }
            
            toProcess = new LinkedHashSet<FileObject>(toProcess);
            toProcess.removeAll(done);
        }

        if (!indexed) {
            GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {sourceCP});
            try {
                waitScanFinished();
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        try {
            result.addAll(doProcessFiles(cpInfo, toProcess, bulkPattern, patterns, patternTests, handle, cancel, true, new LinkedList<FileObject>()));
        } finally {
            if (!indexed) {
                GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, new ClassPath[] {sourceCP});
            }
        }

        return result;
    }

    private static List<ErrorDescription> doProcessFiles(final ClasspathInfo cpInfo, Collection<FileObject> toProcess, final BulkPattern bulkPattern, final Map<PatternDescription, List<HintDescription>> patterns, final Map<String, List<PatternDescription>> patternTests, final ProgressHandleWrapper handle, final AtomicBoolean cancel, final boolean indexed, final Collection<FileObject> done) {
        final List<ErrorDescription> eds = new LinkedList<ErrorDescription>();
        final boolean[] stop = new boolean[1];
        JavaSource js = JavaSource.create(cpInfo, toProcess);

        try {
            js.runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController cc) throws Exception {
                    if (cancel.get() || stop[0]) return ;

                    Document doc = indexed ? cc.getSnapshot().getSource().getDocument(true) : null;

                    try {
                        if (cc.toPhase(JavaSource.Phase.PARSED).compareTo(JavaSource.Phase.PARSED) < 0) {
                            return;
                        }

                        Map<String, Collection<TreePath>> matchingPatterns = BulkSearch.match(cc, cc.getCompilationUnit(), bulkPattern);

                        if (matchingPatterns.isEmpty()) {
                            done.add(cc.getFileObject());
                            handle.tick();
                            return ;
                        }

                        if (!indexed) {
                            stop[0] = true;
                            return ;
                        }

                        if (cc.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
                            return;
                        }

                        eds.addAll(new HintsInvoker().doComputeHints(cc, matchingPatterns, patternTests, patterns));
                        done.add(cc.getFileObject());
                    } finally {
                        HintsSettings.setPreferencesOverride(null);
                    }

                    handle.tick();
                }
            }, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return eds;
    }

    private static String performSlowFixes(List<ErrorDescription> edsWithSlowsFixes, Map<ErrorDescription, Fix> fixes, ProgressHandleWrapper handle) throws Exception {
        for (ErrorDescription ed : edsWithSlowsFixes) {
            try {
                DataObject d = DataObject.find(ed.getFile());
                EditorCookie ec = d.getLookup().lookup(EditorCookie.class);
                Document doc = ec.openDocument();

                fixes.get(ed).implement();

                JavaSource.forFileObject(ed.getFile()).runModificationTask(new RemoveUnusedImports()).commit();
                
                SaveCookie sc = d.getLookup().lookup(SaveCookie.class);

                if (sc != null) {
                    sc.save();
                }
            } catch (Exception ex) {
                Exceptions.attachMessage(ex, FileUtil.getFileDisplayName(ed.getFile()));
                
                throw ex;
            }

            handle.tick();
        }
        return null;
    }

    private static final class RemoveUnusedImports implements Task<WorkingCopy> {
        public void run(WorkingCopy wc) throws IOException {
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

    private static List<ModificationResult> performFastFixes(Map<FileObject, List<JavaFix>> fastFixes, ProgressHandleWrapper handle, AtomicBoolean cancel) {
        Map<ClasspathInfo, Collection<FileObject>> sortedFilesForFixes = sortFiles(fastFixes.keySet());
        List<ModificationResult> results = new LinkedList<ModificationResult>();

        for (Entry<ClasspathInfo, Collection<FileObject>> e : sortedFilesForFixes.entrySet()) {
            if (cancel.get()) return null;
            
            Map<FileObject, List<JavaFix>> filtered = new HashMap<FileObject, List<JavaFix>>();

            for (FileObject f : e.getValue()) {
                filtered.put(f, fastFixes.get(f));
            }

            ModificationResult r = performFastFixes(e.getKey(), filtered, handle, cancel);
            
            if (r != null) {
                results.add(r);
            }
        }

        return results;
    }

    private static ModificationResult performFastFixes(ClasspathInfo cpInfo, final Map<FileObject, List<JavaFix>> toProcess, final ProgressHandleWrapper handle, final AtomicBoolean cancel) {
        JavaSource js = JavaSource.create(cpInfo, toProcess.keySet());

        try {
            return js.runModificationTask(new Task<WorkingCopy>() {
                public void run(WorkingCopy wc) throws Exception {
                    if (cancel.get()) return ;
                    
                    if (wc.toPhase(Phase.RESOLVED).compareTo(Phase.RESOLVED) < 0)
                        return ;

                    for (JavaFix f : toProcess.get(wc.getFileObject())) {
                        if (cancel.get()) return ;
                        
                        JavaFixImpl.Accessor.INSTANCE.process(f, wc, new JavaFix.UpgradeUICallback() {
                            public boolean shouldUpgrade(String comment) {
                                return true;
                            }
                        });
                    }

                    handle.tick();
                }
            });
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    public static Collection<FileObject> toProcess(Lookup l) {
        List<FileObject> result = new LinkedList<FileObject>();

        result.addAll(l.lookupAll(FileObject.class));

        for (SourceGroup sg : l.lookupAll(SourceGroup.class)) {
            result.add(sg.getRootFolder());
        }
        
        for (Project p : l.lookupAll(Project.class)) {
            Sources s = ProjectUtils.getSources(p);

            for (SourceGroup sg : s.getSourceGroups("java")) {
                result.add(sg.getRootFolder());
            }
        }

        return result;
    }

    private static Collection<FileObject> findAllSources(Collection<FileObject> from) {
        List<FileObject> result = new LinkedList<FileObject>();
        Queue<FileObject> q = new LinkedList<FileObject>();

        q.addAll(from);

        while (!q.isEmpty()) {
            FileObject f = q.poll();

            if (f.isData() && "text/x-java".equals(FileUtil.getMIMEType(f))) {
                result.add(f);
            }

            if (f.isFolder()) {
                q.addAll(Arrays.asList(f.getChildren()));
            }
        }

        return result;
    }

    private static Map<ClasspathInfo, Collection<FileObject>> sortFiles(Collection<FileObject> from) {
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

    @SuppressWarnings("deprecation")
    private static void waitScanFinished() throws InterruptedException {
        SourceUtils.waitScanFinished();
    }

    private static final class ProgressHandleWrapper {

        private static final int TOTAL = 1000;
        
        private final ProgressHandle handle;
        private final int[]          parts;

        private       int            currentPart = (-1);
        private       int            currentPartTotalWork;
        private       int            currentPartWorkDone;
        private       long           currentPartStartTime;

        private       int            currentOffset;

        public ProgressHandleWrapper(int[] parts) {
            this(null, parts);
        }
        
        public ProgressHandleWrapper(ProgressHandle handle, int[] parts) {
            this.handle = handle;

            if (handle == null) {
                this.parts = null;
            } else {
                int total = 0;

                for (int i : parts) {
                    total += i;
                }

                this.parts = new int[parts.length];

                for (int cntr = 0; cntr < parts.length; cntr++) {
                    this.parts[cntr] = (TOTAL * parts[cntr]) / total;
                }
            }
        }

        public void startNextPart(int totalWork) {
            if (handle == null) return ;
            
            if (currentPart == (-1)) {
                handle.start(TOTAL);
            } else {
                currentOffset += parts[currentPart];
            }

            currentPart++;

            currentPartTotalWork = totalWork;
            currentPartWorkDone  = 0;
            currentPartStartTime = System.currentTimeMillis();

            setAutomatedMessage();
        }

        public void tick() {
            if (handle == null) return ;

            currentPartWorkDone++;

            handle.progress(currentOffset + (parts[currentPart] * currentPartWorkDone) / currentPartTotalWork);

            setAutomatedMessage();
        }

        public void setMessage(String message) {
            if (handle == null) return ;

            handle.progress(message);
        }

        private void setAutomatedMessage() {
            if (handle == null || currentPart == (-1)) return ;

            long spentTime = System.currentTimeMillis() - currentPartStartTime;
            double timePerUnit = ((double) spentTime) / currentPartWorkDone;
            String timeString;

            if (spentTime > 0) {
                double totalTime = currentPartTotalWork * timePerUnit;
                
                timeString = toHumanReadableString(spentTime) + "/" + toHumanReadableString(totalTime);
            } else {
                timeString = "No estimate";
            }
            
            handle.progress("Part " + (currentPart + 1) + "/" + parts.length + ", " + currentPartWorkDone + "/" + currentPartTotalWork + ", " + timeString);
        }

        private static String toHumanReadableString(double d) {
            StringBuilder result = new StringBuilder();
            long inSeconds = (long) (d / 1000);
            int seconds = (int) (inSeconds % 60);
            long inMinutes = inSeconds / 60;

            result.append(inMinutes);
            result.append("m");
            result.append(seconds);
            result.append("s");

            return result.toString();
        }
    }
}
