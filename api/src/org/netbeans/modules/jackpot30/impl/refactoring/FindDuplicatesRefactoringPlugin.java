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

package org.netbeans.modules.jackpot30.impl.refactoring;

import java.io.CharConversionException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.Position.Bias;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.ModificationResult.Difference;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Container;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.jackpot30.impl.batch.BatchUtilities;
import org.netbeans.modules.jackpot30.impl.batch.ProgressHandleWrapper;
import org.netbeans.modules.jackpot30.impl.batch.ProgressHandleWrapper.ProgressHandleAbstraction;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.java.spi.DiffElement;
import org.netbeans.modules.refactoring.spi.ProgressProviderAdapter;
import org.netbeans.modules.refactoring.spi.RefactoringElementImplementation;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.Line;
import org.openide.text.PositionBounds;
import org.openide.text.PositionRef;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;

public class FindDuplicatesRefactoringPlugin extends ProgressProviderAdapter implements RefactoringPlugin, ProgressHandleAbstraction {

    private final FindDuplicatesRefactoring refactoring;
    private final AtomicBoolean cancel = new AtomicBoolean();

    public FindDuplicatesRefactoringPlugin(FindDuplicatesRefactoring refactoring) {
        this.refactoring = refactoring;
    }

    public Problem preCheck() {
        return null;
    }

    public Problem checkParameters() {
        return null;
    }

    public Problem fastCheckParameters() {
        return null;
    }

    public void cancelRequest() {
        cancel.set(true);
    }

     public Problem prepare(RefactoringElementsBag refactoringElements) {
        cancel.set(false);

        Collection<MessageImpl> problems = refactoring.isQuery() ? performSearchForPattern(refactoringElements) : performApplyPattern(refactoringElements);
        Problem current = null;

        for (MessageImpl problem : problems) {
            Problem p = new Problem(problem.kind == MessageKind.ERROR, problem.text);

            if (current != null)
                p.setNext(current);
            current = p;
        }

        return current;
    }

    private List<MessageImpl> performSearchForPattern(RefactoringElementsBag refactoringElements) {
        ProgressHandleWrapper w = new ProgressHandleWrapper(this, 50, 50);
        BatchResult candidates = BatchSearch.findOccurrences(refactoring.getPattern(), refactoring.getScope(), w);
        List<MessageImpl> problems = new LinkedList<MessageImpl>(candidates.problems);

        int[] parts = new int[candidates.projectId2Resources.size()];
        int   index = 0;

        for (Entry<? extends Container, ? extends Collection<? extends Resource>> e : candidates.projectId2Resources.entrySet()) {
            parts[index++] = e.getValue().size();
        }

        ProgressHandleWrapper inner = w.startNextPartWithEmbedding(parts);

        for (Collection<? extends Resource> it :candidates.projectId2Resources.values()) {
            inner.startNextPart(it.size());

            if (refactoring.isVerify()) {
                BatchSearch.getVerifiedSpans(it, problems);
            }

            for (Resource r : it) {
                refactoringElements.addAll(refactoring, createRefactoringElementImplementation(r, refactoring.isVerify()));
                inner.tick();
            }
        }

        w.finish();

        return problems;
     }

    public static Collection<RefactoringElementImplementation> createRefactoringElementImplementation(Resource r, boolean verify) {
        FileObject file = r.getResolvedFile();

        if (file == null) {
            //TODO???
            return null;
        }

        List<RefactoringElementImplementation> result = new LinkedList<RefactoringElementImplementation>();
        
        try {
            List<PositionBounds> spans = null;
            boolean faded = false;

            if (verify) {
                Iterable<? extends ErrorDescription> errors = r.getVerifiedSpans(new LinkedList<MessageImpl>());

                if (errors != null) {
                    spans = new LinkedList<PositionBounds>();
                    
                    for (ErrorDescription ed : errors) {
                        spans.add(ed.getRange());
                    }
                } else {
                    faded = true;
                }
            }

            DataObject d = DataObject.find(file);
            LineCookie lc = d.getLookup().lookup(LineCookie.class);
            
            if (spans == null) {
                EditorCookie ec = d.getLookup().lookup(EditorCookie.class);
                CloneableEditorSupport ces = (CloneableEditorSupport) ec;

                spans = new LinkedList<PositionBounds>();
                
                for (int[] span : r.getCandidateSpans()) {
                    PositionRef start = ces.createPositionRef(span[0], Bias.Forward);
                    PositionRef end = ces.createPositionRef(span[1], Bias.Forward);
                    
                    spans.add(new PositionBounds(start, end));
                }
            }

            for (PositionBounds bound : spans) {
                PositionRef start = bound.getBegin();
                PositionRef end = bound.getEnd();
                Line l = lc.getLineSet().getCurrent(start.getLine());
                String lineText = l.getText();

                int boldStart = start.getColumn();
                int boldEnd   = end.getLine() == start.getLine() ? end.getColumn() : lineText.length();

                StringBuilder displayName = new StringBuilder();

                if (faded) {
                    displayName.append("(not verified) ");
                }
                
                displayName.append(escapedSubstring(lineText, 0, boldStart).replaceAll("^[ ]*", ""));
                displayName.append("<b>");
                displayName.append(escapedSubstring(lineText, boldStart, boldEnd));
                displayName.append("</b>");
                displayName.append(escapedSubstring(lineText, boldEnd, lineText.length()));

                result.add(new RefactoringElementImpl(r, bound, displayName.toString()));
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return result;
    }

    private Collection<MessageImpl> performApplyPattern(RefactoringElementsBag refactoringElements) {
        ProgressHandleWrapper w = new ProgressHandleWrapper(this, 30, 70);
        BatchResult candidates = BatchSearch.findOccurrences(refactoring.getPattern(), refactoring.getScope(), w);
        Collection<MessageImpl> problems = new LinkedList<MessageImpl>(candidates.problems);
        Collection<? extends ModificationResult> res = BatchUtilities.applyFixes(candidates, w, /*XXX*/new AtomicBoolean(), problems);

        refactoringElements.registerTransaction(new RetoucheCommit(new LinkedList<ModificationResult>(res)));

        for (ModificationResult mr : res) {
            for (FileObject file : mr.getModifiedFileObjects()) {
                for (Difference d : mr.getDifferences(file)) {
                    refactoringElements.add(refactoring, DiffElement.create(d, file, mr));
                }
            }
        }

        w.finish();

        return problems;
    }

    private static String escapedSubstring(String str, int start, int end) {
        String substring = str.substring(start, end);
        
        try {
            return XMLUtil.toElementContent(substring);
        } catch (CharConversionException ex) {
            Exceptions.printStackTrace(ex);
            return substring;
        }
    }

    public void start(int totalWork) {
        fireProgressListenerStart(-1, totalWork);
        lastWorkDone = 0;
    }

    private int lastWorkDone;
    public void progress(int currentWorkDone) {
        while (lastWorkDone < currentWorkDone) {
            fireProgressListenerStep(currentWorkDone);
            lastWorkDone++;
        }
    }

    public void progress(String message) {
        //ignored
    }

    public void finish() {
        fireProgressListenerStop();
    }

    private static final class RefactoringElementImpl extends SimpleRefactoringElementImplementation {

        private final Resource resource;
        private final PositionBounds span;
        private final String displayName;

        private final Lookup lookup;
        
        public RefactoringElementImpl(Resource resource, PositionBounds span, String displayName) {
            this.resource = resource;
            this.span = span;
            this.lookup = Lookups.fixed(resource);
            this.displayName = displayName;
        }

        public String getText() {
            return getDisplayText();
        }

        public String getDisplayText() {
            return displayName;
        }

        public void performChange() {
            throw new IllegalStateException();
        }

        public Lookup getLookup() {
            return lookup;
        }

        public FileObject getParentFile() {
            return resource.getResolvedFile();
        }

        public PositionBounds getPosition() {
            return span;
        }
        
    }
    
}