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

package org.netbeans.modules.jackpot30.impl.hints;

import com.sun.source.tree.Tree;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.swing.text.Document;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.editor.GuardedDocument;
import org.netbeans.editor.MarkBlock;
import org.netbeans.editor.MarkBlockChain;
import org.openide.filesystems.FileObject;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder.VariableAssignments;
import org.netbeans.modules.jackpot30.impl.pm.Pattern;
import org.netbeans.modules.jackpot30.spi.Hacks;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Condition;
import org.netbeans.modules.jackpot30.spi.HintDescription.CustomCondition;
import org.netbeans.modules.jackpot30.spi.HintDescription.DeclarativeFixDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.ErrorDescriptionAcceptor;
import org.netbeans.modules.jackpot30.spi.HintDescription.FixAcceptor;
import org.netbeans.modules.jackpot30.spi.HintDescription.Literal;
import org.netbeans.modules.jackpot30.spi.HintDescription.MarkCondition;
import org.netbeans.modules.jackpot30.spi.HintDescription.MarksWorker;
import org.netbeans.modules.jackpot30.spi.HintDescription.Operator;
import org.netbeans.modules.jackpot30.spi.HintDescription.OtherwiseCondition;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Selector;
import org.netbeans.modules.jackpot30.spi.HintDescription.Value;
import org.netbeans.modules.jackpot30.spi.HintMetadata;
import org.netbeans.modules.jackpot30.spi.HintMetadata.HintSeverity;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class HintsInvoker {

    private final Map<String, Long> timeLog = new HashMap<String, Long>();

    private final CompilationInfo info;
    private final int caret;
    private final int from;
    private final int to;
    private final AtomicBoolean cancel;

    public HintsInvoker(CompilationInfo info, AtomicBoolean cancel) {
        this(info, -1, cancel);
    }

    public HintsInvoker(CompilationInfo info, int caret, AtomicBoolean cancel) {
        this(info, caret, -1, -1, cancel);
    }

    public HintsInvoker(CompilationInfo info, int from, int to, AtomicBoolean cancel) {
        this(info, -1, from, to, cancel);
    }

    private HintsInvoker(CompilationInfo info, int caret, int from, int to, AtomicBoolean cancel) {
        this.info = info;
        this.caret = caret;
        this.from = from;
        this.to = to;
        this.cancel = cancel;
    }

    public List<ErrorDescription> computeHints(CompilationInfo info) {
        return computeHints(info, new TreePath(info.getCompilationUnit()));
    }

    private List<ErrorDescription> computeHints(CompilationInfo info, TreePath startAt) {
        List<HintDescription> descs = new LinkedList<HintDescription>();
        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : RulesManager.getInstance().allHints.entrySet()) {
            HintMetadata m = e.getKey();

            if (!RulesManager.getInstance().isHintEnabled(m)) {
                continue;
            }

            if (caret != -1) {
                if (m.kind == HintMetadata.Kind.SUGGESTION || m.kind == HintMetadata.Kind.SUGGESTION_NON_GUI) {
                    descs.addAll(e.getValue());
                } else {
                    if (RulesManager.getInstance().getHintSeverity(m) == HintSeverity.CURRENT_LINE_WARNING) {
                        descs.addAll(e.getValue());
                    }
                }
            } else {
                if (m.kind == HintMetadata.Kind.HINT || m.kind == HintMetadata.Kind.HINT_NON_GUI) {
                    if (RulesManager.getInstance().getHintSeverity(m) != HintSeverity.CURRENT_LINE_WARNING || from != (-1)) {
                        descs.addAll(e.getValue());
                    }
                }
            }
        }

        Map<Kind, List<HintDescription>> kindHints = new HashMap<Kind, List<HintDescription>>();
        Map<PatternDescription, List<HintDescription>> patternHints = new HashMap<PatternDescription, List<HintDescription>>();

        RulesManager.sortOut(descs, kindHints, patternHints);

        long elementBasedStart = System.currentTimeMillis();

        RulesManager.computeElementBasedHintsXXX(info, cancel, kindHints, patternHints);

        long elementBasedEnd = System.currentTimeMillis();

        timeLog.put("Computing Element Based Hints", elementBasedEnd - elementBasedStart);

        List<ErrorDescription> errors = join(computeHints(info, startAt, kindHints, patternHints, new LinkedList<MessageImpl>()));

        dumpTimeSpentInHints();
        
        return errors;
    }

    public List<ErrorDescription> computeHints(CompilationInfo info,
                                        Map<Kind, List<HintDescription>> hints,
                                        Map<PatternDescription, List<HintDescription>> patternHints) {
        return computeHints(info, hints, patternHints, new LinkedList<MessageImpl>());
    }

    public List<ErrorDescription> computeHints(CompilationInfo info,
                                        Map<Kind, List<HintDescription>> hints,
                                        Map<PatternDescription, List<HintDescription>> patternHints,
                                        Collection<? super MessageImpl> problems) {
        return join(computeHints(info, new TreePath(info.getCompilationUnit()), hints, patternHints, problems));
    }

    public Map<HintDescription, List<ErrorDescription>> computeHints(CompilationInfo info,
                                        TreePath startAt,
                                        Map<Kind, List<HintDescription>> hints,
                                        Map<PatternDescription, List<HintDescription>> patternHints,
                                        Collection<? super MessageImpl> problems) {
        if (caret != -1) {
            TreePath tp = info.getTreeUtilities().pathFor(caret);
            return computeSuggestions(info, tp, hints, patternHints, problems);
        } else {
            if (from != (-1) && to != (-1)) {
                return computeHintsInSpan(info, hints, patternHints, problems);
            } else {
                return computeHintsImpl(info, startAt, hints, patternHints, problems);
            }
        }
    }

    Map<HintDescription, List<ErrorDescription>> computeHintsImpl(CompilationInfo info,
                                        TreePath startAt,
                                        Map<Kind, List<HintDescription>> hints,
                                        Map<PatternDescription, List<HintDescription>> patternHints,
                                        Collection<? super MessageImpl> problems) {
        Map<HintDescription, List<ErrorDescription>> errors = new HashMap<HintDescription, List<ErrorDescription>>();

        long kindCount = 0;

        for (Entry<Kind, List<HintDescription>> e : hints.entrySet()) {
            kindCount += e.getValue().size();
        }

        timeLog.put("[C] Kind Based Hints", kindCount);

        if (!hints.isEmpty()) {
            long kindStart = System.currentTimeMillis();

            new ScannerImpl(info, cancel, hints).scan(startAt, errors);

            long kindEnd = System.currentTimeMillis();

            timeLog.put("Kind Based Hints", kindEnd - kindStart);
        }

        timeLog.put("[C] Pattern Based Hints", (long) patternHints.size());

        long patternStart = System.currentTimeMillis();

        Map<String, List<PatternDescription>> patternTests = computePatternTests(patternHints);

        long bulkPatternStart = System.currentTimeMillis();

        BulkPattern bulkPattern = BulkSearch.getDefault().create(info, patternTests.keySet());

        long bulkPatternEnd = System.currentTimeMillis();

        timeLog.put("Bulk Pattern preparation", bulkPatternEnd - bulkPatternStart);

        long bulkStart = System.currentTimeMillis();

        Map<String, Collection<TreePath>> occurringPatterns = BulkSearch.getDefault().match(info, startAt, bulkPattern, timeLog);

        long bulkEnd = System.currentTimeMillis();

        timeLog.put("Bulk Search", bulkEnd - bulkStart);

        mergeAll(errors, doComputeHints(info, occurringPatterns, patternTests, patternHints, problems));

        long patternEnd = System.currentTimeMillis();

        timeLog.put("Pattern Based Hints", patternEnd - patternStart);

        return errors;
    }

    Map<HintDescription, List<ErrorDescription>> computeHintsInSpan(CompilationInfo info,
                                        Map<Kind, List<HintDescription>> hints,
                                        Map<PatternDescription, List<HintDescription>> patternHints,
                                        Collection<? super MessageImpl> problems) {

        TreePath path = info.getTreeUtilities().pathFor((from + to) / 2);

        while (path.getLeaf().getKind() != Kind.COMPILATION_UNIT) {
            int start = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), path.getLeaf());
            int end = (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), path.getLeaf());

            if (start <= from && end >= to) {
                break;
            }

            path = path.getParentPath();
        }

        Map<HintDescription, List<ErrorDescription>> errors = new HashMap<HintDescription, List<ErrorDescription>>();

        if (!hints.isEmpty()) {
            long kindStart = System.currentTimeMillis();

            new ScannerImpl(info, cancel, hints).scan(path, errors);

            long kindEnd = System.currentTimeMillis();

            timeLog.put("Kind Based Hints", kindEnd - kindStart);
        }

        if (!patternHints.isEmpty()) {
            long patternStart = System.currentTimeMillis();

            Map<String, List<PatternDescription>> patternTests = computePatternTests(patternHints);

            long bulkStart = System.currentTimeMillis();

            BulkPattern bulkPattern = BulkSearch.getDefault().create(info, patternTests.keySet());
            Map<String, Collection<TreePath>> occurringPatterns = BulkSearch.getDefault().match(info, path, bulkPattern, timeLog);

            long bulkEnd = System.currentTimeMillis();

            timeLog.put("Bulk Search", bulkEnd - bulkStart);

            mergeAll(errors, doComputeHints(info, occurringPatterns, patternTests, patternHints, problems));

            long patternEnd = System.currentTimeMillis();

            timeLog.put("Pattern Based Hints", patternEnd - patternStart);
        }

        return errors;
    }

    Map<HintDescription, List<ErrorDescription>> computeSuggestions(CompilationInfo info,
                                        TreePath workOn,
                                        Map<Kind, List<HintDescription>> hints,
                                        Map<PatternDescription, List<HintDescription>> patternHints,
                                        Collection<? super MessageImpl> problems) {
        Map<HintDescription, List<ErrorDescription>> errors = new HashMap<HintDescription, List<ErrorDescription>>();

        if (!hints.isEmpty()) {
            long kindStart = System.currentTimeMillis();

            TreePath proc = workOn;

            while (proc != null) {
                new ScannerImpl(info, cancel, hints).scanDoNotGoDeeper(proc, errors);
                proc = proc.getParentPath();
            }

            long kindEnd = System.currentTimeMillis();

            timeLog.put("Kind Based Suggestions", kindEnd - kindStart);
        }

        if (!patternHints.isEmpty()) {
            long patternStart = System.currentTimeMillis();

            Map<String, List<PatternDescription>> patternTests = computePatternTests(patternHints);

            //pretend that all the patterns occur on all treepaths from the current path
            //up (probably faster than using BulkSearch over whole file)
            //TODO: what about machint trees under the current path?
            Set<TreePath> paths = new HashSet<TreePath>();

            TreePath tp = workOn;

            while (tp != null) {
                paths.add(tp);
                tp = tp.getParentPath();
            }

            Map<String, Collection<TreePath>> occurringPatterns = new HashMap<String, Collection<TreePath>>();

            for (String p : patternTests.keySet()) {
                occurringPatterns.put(p, paths);
            }

//            long bulkStart = System.currentTimeMillis();
//
//            BulkPattern bulkPattern = BulkSearch.getDefault().create(info, patternTests.keySet());
//            Map<String, Collection<TreePath>> occurringPatterns = BulkSearch.getDefault().match(info, new TreePath(info.getCompilationUnit()), bulkPattern, timeLog);
//
//            long bulkEnd = System.currentTimeMillis();
//
//            Set<Tree> acceptedLeafs = new HashSet<Tree>();
//
//            TreePath tp = workOn;
//
//            while (tp != null) {
//                acceptedLeafs.add(tp.getLeaf());
//                tp = tp.getParentPath();
//            }
//
//            for (Entry<String, Collection<TreePath>> e : occurringPatterns.entrySet()) {
//                for (Iterator<TreePath> it = e.getValue().iterator(); it.hasNext(); ) {
//                    if (!acceptedLeafs.contains(it.next().getLeaf())) {
//                        it.remove();
//                    }
//                }
//            }
//
//            timeLog.put("Bulk Search", bulkEnd - bulkStart);

            mergeAll(errors, doComputeHints(info, occurringPatterns, patternTests, patternHints, problems));

            long patternEnd = System.currentTimeMillis();

            timeLog.put("Pattern Based Hints", patternEnd - patternStart);
        }

        return errors;
    }

    public Map<HintDescription, List<ErrorDescription>> doComputeHints(CompilationInfo info, Map<String, Collection<TreePath>> occurringPatterns, Map<String, List<PatternDescription>> patterns, Map<PatternDescription, List<HintDescription>> patternHints) throws IllegalStateException {
        return doComputeHints(info, occurringPatterns, patterns, patternHints, new LinkedList<MessageImpl>());
    }

    public static Map<String, List<PatternDescription>> computePatternTests(Map<PatternDescription, List<HintDescription>> patternHints) {
        Map<String, List<PatternDescription>> patternTests = new HashMap<String, List<PatternDescription>>();
        for (Entry<PatternDescription, List<HintDescription>> e : patternHints.entrySet()) {
            String p = e.getKey().getPattern();
            List<PatternDescription> descs = patternTests.get(p);
            if (descs == null) {
                patternTests.put(p, descs = new LinkedList<PatternDescription>());
            }
            descs.add(e.getKey());
        }
        return patternTests;
    }

    private Map<HintDescription, List<ErrorDescription>> doComputeHints(CompilationInfo info, Map<String, Collection<TreePath>> occurringPatterns, Map<String, List<PatternDescription>> patterns, Map<PatternDescription, List<HintDescription>> patternHints, Collection<? super MessageImpl> problems) throws IllegalStateException {
        Map<HintDescription, List<ErrorDescription>> errors = new HashMap<HintDescription, List<ErrorDescription>>();
        List<HintEvaluationData> evaluationData = new LinkedList<HintEvaluationData>();

        for (Entry<String, Collection<TreePath>> occ : occurringPatterns.entrySet()) {
            for (PatternDescription d : patterns.get(occ.getKey())) {
                Map<String, TypeMirror> constraints = new HashMap<String, TypeMirror>();

                for (Entry<String, String> e : d.getConstraints().entrySet()) {
                    constraints.put(e.getKey(), Hacks.parseFQNType(info, e.getValue()));
                }

                Pattern p = Pattern.compile(info, occ.getKey(), constraints, d.getImports());
                TreePath toplevel = new TreePath(info.getCompilationUnit());
                TreePath patt = new TreePath(toplevel, p.getPattern());

                for (TreePath candidate : occ.getValue()) {
                    VariableAssignments verified = CopyFinder.computeVariables(info, patt, candidate, cancel, p.getConstraints());

                    if (verified == null) {
                        continue;
                    }

                    Set<String> suppressedWarnings = new HashSet<String>(Utilities.findSuppressedWarnings(info, candidate));

                    for (HintDescription hd : patternHints.get(d)) {
                        HintMetadata hm = hd.getMetadata();
                        HintContext c = new HintContext(info, hm, candidate, verified.variables, verified.multiVariables, verified.variables2Names, constraints, problems);

                        if (!Collections.disjoint(suppressedWarnings, hm.suppressWarnings))
                            continue;

                        if (hd.getWorker() instanceof MarksWorker) {
                            HintContext workerContext = new HintContext(c);

                            workerContext.enterScope();
                            
                            MarksWorker mw = (MarksWorker) hd.getWorker();
                            List<FixEvaluationData> fixData = new LinkedList<FixEvaluationData>();

                            for (DeclarativeFixDescription fd : mw.fixes) {
                                HintContext fixContext = new HintContext(workerContext);

                                fixContext.enterScope();
                                fixData.add(new FixEvaluationData(fixContext, new LinkedList<Condition>(fd.marks), fd.acceptor));
                            }

                            HintEvaluationData data = new HintEvaluationData(workerContext, hd, new LinkedList<Condition>(mw.marks), mw.acceptor, fixData);

                            evaluationData.add(data);
                            continue;
                        }

                        Collection<? extends ErrorDescription> workerErrors = runHint(hd, c);

                        if (workerErrors != null) {
                            merge(errors, hd, workerErrors);
                        }
                    }
                }
            }
        }

        Map<Tree, Map<String, Object>> marks = new HashMap<Tree, Map<String, Object>>();

        for (HintEvaluationData hed : evaluationData) {
            enterSpeculativeAssignments(hed.ctx, hed.marks, marks);

            for (FixEvaluationData fed : hed.fixDescriptions) {
                //XXX: there is currently no test that would test this: (seee testSpeculativeAssignmentForFixes)
                enterSpeculativeAssignments(hed.ctx, fed.marks, marks);
            }
        }

        boolean wasChange = true;

        while (wasChange /*!evaluationData.isEmpty()*/) {
            boolean currentWasChange = false;

            for (Iterator<HintEvaluationData> it = evaluationData.iterator(); it.hasNext(); ) {
                HintEvaluationData hed = it.next();
                int origMarksSize = hed.marks.size();

                Boolean hres = processConditions(hed.ctx, marks, hed.marks, -1, -1);

                currentWasChange |= origMarksSize != hed.marks.size();

                if (hres == null) {
                    //XXX???
                    continue;
                }

                if (hres != null && !hres) {
                    currentWasChange = true;
                    clearSpeculativeAssignments(hed.ctx, hed.marks, marks);
                    for (FixEvaluationData fed : hed.fixDescriptions) {
                        clearSpeculativeAssignments(hed.ctx, fed.marks, marks);
                    }
                    it.remove();
                    continue;
                }

                for (Iterator<FixEvaluationData> fixes = hed.fixDescriptions.iterator(); fixes.hasNext(); ) {
                    FixEvaluationData fed = fixes.next();
                    int o = fed.marks.size();
                    Boolean res = processConditions(fed.ctx, marks, fed.marks, hed.fixDescriptions.size(), hed.createdFixes.size());

                    currentWasChange |= o != fed.marks.size();

                    if (res == null) continue;

                    if (res) {
                        Fix fix = fed.acceptor.accept(fed.ctx);

                        if (fix != null) {
                            hed.createdFixes.add(fix);
                        }
                    } else {
                        clearSpeculativeAssignments(fed.ctx, fed.marks, marks);
                    }

                    fixes.remove();
                    currentWasChange = true;
                }

                if (!wasChange && !currentWasChange) {
                    hed.fixDescriptions.clear();
                }

                if (hed.fixDescriptions.isEmpty()) {
                    //XXX: @SuppressWarnings!
                    ErrorDescription ed = hed.acceptor.accept(hed.ctx);

                    ed = ErrorDescriptionFactory.createErrorDescription(ed.getSeverity(), ed.getDescription(), hed.createdFixes, ed.getFile(), ed.getRange().getBegin().getOffset(), ed.getRange().getEnd().getOffset());

                    if (ed != null) {
                        merge(errors, hed.hd, ed);
                    }
                    it.remove();
                }
            }

            wasChange = currentWasChange;
        }

        return errors;
    }

//    public static void computeHints(URI file, ProcessingEnvironment env, CompilationUnitTree cut, RulesManager m) {
//        Map<Kind, HintDescription> hints = m.getKindBasedHints();
//
//        if (hints.isEmpty()) {
//            return ;
//        }
//
//        List<ErrorDescription> errors = new  LinkedList<ErrorDescription>();
//
//        File af = new File(file.getPath());
//        FileObject f = FileUtil.toFileObject(af);
//
//        new ScannerImpl(f, env, hints).scan(cut, errors);
//
//        for (ErrorDescription ed : errors) {
//            Diagnostic.Kind k;
//
//            switch (ed.getSeverity()) {
//                case ERROR:
//                    k = Diagnostic.Kind.ERROR;
//                    break;
//                default:
//                    k = Diagnostic.Kind.WARNING;
//                    break;
//            }
//
//            env.getMessager().printMessage(k, ed.getDescription());
//        }
//    }

    public Map<String, Long> getTimeLog() {
        return timeLog;
    }

    private final class ScannerImpl extends CancellableTreePathScanner<Void, Map<HintDescription, List<ErrorDescription>>> {

        private final Stack<Set<String>> suppresWarnings = new Stack<Set<String>>();
        private final CompilationInfo info;
        private final FileObject file;
        private final ProcessingEnvironment env;
        private final Map<Kind, List<HintDescription>> hints;

        public ScannerImpl(CompilationInfo info, AtomicBoolean cancel, Map<Kind, List<HintDescription>> hints) {
            super(cancel);
            this.info = info;
            this.file = null;
            this.env  = null;
            this.hints = hints;
        }

        public ScannerImpl(FileObject file, ProcessingEnvironment env, Map<Kind, List<HintDescription>> hints) {
            super(new AtomicBoolean());
            this.info = null;
            this.file = file;
            this.env = env;
            this.hints = hints;
        }

        private void runAndAdd(TreePath path, List<HintDescription> rules, Map<HintDescription, List<ErrorDescription>> d) {
            if (rules != null && !isInGuarded(info, path)) {
                OUTER: for (HintDescription hd : rules) {
                    if (isCanceled()) {
                        return ;
                    }

                    HintMetadata hm = hd.getMetadata();

                    for (String wname : hm.suppressWarnings) {
                        if( !suppresWarnings.empty() && suppresWarnings.peek().contains(wname)) {
                            continue OUTER;
                        }
                    }

                    HintContext c = new HintContext(info, hm, path, Collections.<String, TreePath>emptyMap(), Collections.<String, Collection<? extends TreePath>>emptyMap(), Collections.<String, String>emptyMap());
                    Collection<? extends ErrorDescription> errors = runHint(hd, c);

                    if (errors != null) {
                        merge(d, hd, errors);
                    }
                }
            }
        }

        @Override
        public Void scan(Tree tree, Map<HintDescription, List<ErrorDescription>> p) {
            if (tree == null)
                return null;

            TreePath tp = new TreePath(getCurrentPath(), tree);
            Kind k = tree.getKind();

            boolean b = pushSuppressWarrnings(tp);
            try {
                runAndAdd(tp, hints.get(k), p);

                if (isCanceled()) {
                    return null;
                }

                return super.scan(tree, p);
            } finally {
                if (b) {
                    suppresWarnings.pop();
                }
            }
        }

        @Override
        public Void scan(TreePath path, Map<HintDescription, List<ErrorDescription>> p) {
            Kind k = path.getLeaf().getKind();
            boolean b = pushSuppressWarrnings(path);
            try {
                runAndAdd(path, hints.get(k), p);

                if (isCanceled()) {
                    return null;
                }

                return super.scan(path, p);
            } finally {
                if (b) {
                    suppresWarnings.pop();
                }
            }
        }

        public void scanDoNotGoDeeper(TreePath path, Map<HintDescription, List<ErrorDescription>> p) {
            Kind k = path.getLeaf().getKind();
            runAndAdd(path, hints.get(k), p);
        }

        private boolean pushSuppressWarrnings(TreePath path) {
            switch(path.getLeaf().getKind()) {
                case CLASS:
                case METHOD:
                case VARIABLE:
                    Set<String> current = suppresWarnings.size() == 0 ? null : suppresWarnings.peek();
                    Set<String> nju = current == null ? new HashSet<String>() : new HashSet<String>(current);

                    Element e = getTrees().getElement(path);

                    if ( e != null) {
                        for (AnnotationMirror am : e.getAnnotationMirrors()) {
                            String name = ((TypeElement)am.getAnnotationType().asElement()).getQualifiedName().toString();
                            if ( "java.lang.SuppressWarnings".equals(name) ) { // NOI18N
                                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = am.getElementValues();
                                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                                    if( "value".equals(entry.getKey().getSimpleName().toString()) ) { // NOI18N
                                        Object value = entry.getValue().getValue();
                                        if ( value instanceof List) {
                                            for (Object av : (List)value) {
                                                if( av instanceof AnnotationValue ) {
                                                    Object wname = ((AnnotationValue)av).getValue();
                                                    if ( wname instanceof String ) {
                                                        nju.add((String)wname);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    suppresWarnings.push(nju);
                    return true;
            }
            return false;
        }

        private Trees getTrees() {
            return info != null ? info.getTrees() : Trees.instance(env);
        }
    }

    static boolean isInGuarded(CompilationInfo info, TreePath tree) {
        if (info == null) {
            return false;
        }

        try {
            Document doc = info.getDocument();

            if (doc instanceof GuardedDocument) {
                int start = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), tree.getLeaf());
                int end = (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), tree.getLeaf());
                GuardedDocument gdoc = (GuardedDocument) doc;
                MarkBlockChain guardedBlockChain = gdoc.getGuardedBlockChain();
                if (guardedBlockChain.compareBlock(start, end) == MarkBlock.INNER) {
                    return true;
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return false;
    }

    private Collection<? extends ErrorDescription> runHint(HintDescription hd, HintContext ctx) {
        long start = System.nanoTime();

        try {
            return hd.getWorker().createErrors(ctx);
        } finally {
            long end = System.nanoTime();
            reportSpentTime(hd.getMetadata().id, end - start);
        }
    }

    public static <K, V> Map<K, List<V>> merge(Map<K, List<V>> to, K key, V value) {
        List<V> toColl = to.get(key);

        if (toColl == null) {
            to.put(key, toColl = new LinkedList<V>());
        }

        toColl.add(value);

        return to;
    }

    public static <K, V> Map<K, List<V>> merge(Map<K, List<V>> to, K key, Collection<? extends V> value) {
        List<V> toColl = to.get(key);

        if (toColl == null) {
            to.put(key, toColl = new LinkedList<V>());
        }

        toColl.addAll(value);

        return to;
    }

    public static <K, V> Map<K, List<V>> mergeAll(Map<K, List<V>> to, Map<? extends K, ? extends Collection<? extends V>> what) {
        for (Entry<? extends K, ? extends Collection<? extends V>> e : what.entrySet()) {
            List<V> toColl = to.get(e.getKey());

            if (toColl == null) {
                to.put(e.getKey(), toColl = new LinkedList<V>());
            }

            toColl.addAll(e.getValue());
        }

        return to;
    }

    public static List<ErrorDescription> join(Map<?, ? extends List<? extends ErrorDescription>> errors) {
        List<ErrorDescription> result = new LinkedList<ErrorDescription>();

        for (Entry<?, ? extends Collection<? extends ErrorDescription>> e : errors.entrySet()) {
            result.addAll(e.getValue());
        }

        return result;
    }

    private static final boolean logTimeSpentInHints = Boolean.getBoolean("java.HintsInvoker.time.in.hints");
    private final Map<String, Long> hint2SpentTime = new HashMap<String, Long>();

    private void reportSpentTime(String id, long nanoTime) {
        if (!logTimeSpentInHints) return;
        
        Long prev = hint2SpentTime.get(id);

        if (prev == null) {
            prev = (long) 0;
        }

        hint2SpentTime.put(id, prev + nanoTime);
    }

    private void dumpTimeSpentInHints() {
        if (!logTimeSpentInHints) return;

        List<Entry<String, Long>> l = new ArrayList<Entry<String, Long>>(hint2SpentTime.entrySet());

        Collections.sort(l, new Comparator<Entry<String, Long>>() {
            @Override
            public int compare(Entry<String, Long> o1, Entry<String, Long> o2) {
                return (int) Math.signum(o1.getValue() - o2.getValue());
            }
        });

        for (Entry<String, Long> e : l) {
            System.err.println(e.getKey() + "=" + String.format("%3.2f", e.getValue() / 1000000.0));
        }
    }


    private void enterSpeculativeAssignments(HintContext ctx, List<Condition> conditions, Map<Tree, Map<String, Object>> marks) {
        for (Condition c : conditions) {
            if (!(c instanceof MarkCondition)) continue;

            MarkCondition mc = (MarkCondition) c;

            if (mc.op != Operator.ASSIGN) {
                continue;
            }

            assert mc.left instanceof Selector;

            Selector s = (Selector) mc.left;
            String treeName = s.selected.get(0);
            String markName = s.selected.get(1);
            TreePath tree = ctx.getVariables().get(treeName);

            assert tree != null;

            System.err.println("speculative assignment to: " + tree.getLeaf());

            Map<String, Object> variables = marks.get(tree.getLeaf());

            if (variables == null) {
                marks.put(tree.getLeaf(), variables = new HashMap<String, Object>());
            }

            PossibleValue pv = (PossibleValue) variables.get(markName);

            if (pv == null) {
                variables.put(markName, pv = new PossibleValue());
            }

            pv.add(mc);
        }
    }

    private void clearSpeculativeAssignments(HintContext ctx, List<Condition> conditions, Map<Tree, Map<String, Object>> marks) {
        for (Condition c : conditions) {
            if (!(c instanceof MarkCondition)) continue;

            MarkCondition mc = (MarkCondition) c;

            if (mc.op != Operator.ASSIGN) {
                continue;
            }

            assert mc.left instanceof Selector;

            Selector s = (Selector) mc.left;
            String treeName = s.selected.get(0);
            String markName = s.selected.get(1);
            TreePath tree = ctx.getVariables().get(treeName);

            assert tree != null;

            System.err.println("clearing speculative assignment from: " + tree.getLeaf());

            Map<String, Object> variables = marks.get(tree.getLeaf());

            assert variables != null;

            Object value = variables.get(markName);

            if (!(value instanceof PossibleValue))
                continue;//XXX: correct?

            PossibleValue pv = (PossibleValue) value;

            assert pv != null;

            pv.remove(mc);

            if (pv.isEmpty()) {
                variables.remove(markName);
            }
        }
    }

    /**true==accepted
     * false==rejected
     * null==nothing
     *
     * @return
     */
    private static Boolean processConditions(HintContext ctx, Map<Tree, Map<String, Object>> marks, List<Condition> cond, int candidatesCount, int confirmedCount) {
        if (cond.isEmpty()) {
            return true; //implicitly accept
        }

        for (Iterator<Condition> it = cond.iterator(); it.hasNext(); ) {
            Condition c = it.next();

            if (c instanceof CustomCondition) {
                if (!((CustomCondition) c).holds(ctx)) {
                    return false;
                }
                it.remove();
                continue;
            }

            if (c instanceof OtherwiseCondition) {
                if (candidatesCount > 1) return null;
                if (confirmedCount > 0) return false;
                it.remove();
                continue;
            }

            MarkCondition mc = (MarkCondition) c;

            switch (mc.op) {
                case ASSIGN:
                    assert mc.left instanceof Selector;

                    Object value = readValue(ctx, marks, mc.right);

                    assert value != null;

                    Selector s = (Selector) mc.left;
                    String treeName = s.selected.get(0);
                    String markName = s.selected.get(1);
                    TreePath tree = ctx.getVariables().get(treeName);

                    assert tree != null; //more gracefull handling, in some case may warn during parsing

                    Map<String, Object> variables = marks.get(tree.getLeaf());

                    if (variables == null) {
                        marks.put(tree.getLeaf(), variables = new HashMap<String, Object>());
                    }

                    variables.put(markName, value);
                    break;
                case EQUALS: {
                    Object left = readValue(ctx, marks, mc.left);
                    Object right = readValue(ctx, marks, mc.right);

                    System.err.println("left=" + left);
                    System.err.println("right=" + right);

                    if (left == null || right == null) {
//                        System.err.println("marks=" + marks);
//                        System.err.println("left=" + left);
//                        System.err.println("right=" + right);
                        //can never be true:
                        return false;
                    }

                    if (left instanceof PossibleValue || right instanceof PossibleValue) {
                        //nothing to set yet.
                        return null;
                    }

                    if (!left.equals(right)) {
                        return false;
                    }

                    break;
                }
                case NOT_EQUALS: {
                    Object left = readValue(ctx, marks, mc.left);
                    Object right = readValue(ctx, marks, mc.right);

                    if (left instanceof PossibleValue || right instanceof PossibleValue) {
                        //nothing to set yet.
                        return null;
                    }

                    if (left == right || (left != null && left.equals(right))) {
                        return false;
                    }

                    break;
                }
                default:
                    throw new UnsupportedOperationException();
            }

            it.remove();
        }

        assert cond.isEmpty();

        return true;
    }

    private static Object readValue(HintContext ctx, Map<Tree, Map<String, Object>> marks, Value v) {
        if (v instanceof Selector) {
            Selector s = (Selector) v;

            if (s.selected.size() == 1) {
                String name = s.selected.get(0);
                TreePath tree = ctx.getVariables().get(name);

                assert tree != null;

                return ctx.getInfo().getTrees().getElement(tree);
            }

            if (s.selected.size() == 2) {
                String treeName = s.selected.get(0);
                String markName = s.selected.get(1);
                TreePath tree = ctx.getVariables().get(treeName);

                assert tree != null; //more gracefull handling, in some case may warn during parsing

                System.err.println("reading=" + tree.getLeaf() + "." + markName);

                Map<String, Object> variables = marks.get(tree.getLeaf());

                if (variables == null) return null;

                return variables.get(markName);
            }

            assert false;
        }

        //XXX: not tested!
        if (v instanceof Literal) {
            return ((Literal) v).value;
        }

        assert false;

        return null;
    }

    private static final class PossibleValue extends HashSet<MarkCondition> {}

    private static final class HintEvaluationData {
        public final HintContext ctx;
        public final HintDescription hd;
        public final List<Condition> marks;
        public final ErrorDescriptionAcceptor acceptor;
        public final List<FixEvaluationData> fixDescriptions;
        public final List<Fix> createdFixes = new LinkedList<Fix>();
        public HintEvaluationData(HintContext ctx, HintDescription hd, List<Condition> marks, ErrorDescriptionAcceptor acceptor, List<FixEvaluationData> fixDescriptions) {
            this.ctx = ctx;
            this.hd = hd;
            this.marks = marks;
            this.acceptor = acceptor;
            this.fixDescriptions = fixDescriptions;
        }
    }

    private static final class FixEvaluationData {
        public final HintContext ctx;
        public final List<Condition> marks;
        public final FixAcceptor acceptor;
        public FixEvaluationData(HintContext ctx, List<Condition> marks, FixAcceptor acceptor) {
            this.ctx = ctx;
            this.marks = marks;
            this.acceptor = acceptor;
        }
    }

}
