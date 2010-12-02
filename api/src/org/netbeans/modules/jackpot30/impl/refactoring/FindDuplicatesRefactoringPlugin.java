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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import javax.swing.text.Position.Bias;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Container;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.jackpot30.impl.batch.ProgressHandleWrapper;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.RefactoringElementImplementation;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.CloneableEditorSupport;
import org.openide.text.Line;
import org.openide.text.PositionBounds;
import org.openide.text.PositionRef;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;

public class FindDuplicatesRefactoringPlugin extends AbstractApplyHintsRefactoringPlugin {

    private final FindDuplicatesRefactoring refactoring;

    public FindDuplicatesRefactoringPlugin(FindDuplicatesRefactoring refactoring) {
        super(refactoring);
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

    private List<MessageImpl> performSearchForPattern(final RefactoringElementsBag refactoringElements) {
        ProgressHandleWrapper w = new ProgressHandleWrapper(this, 50, 50);
        BatchResult candidates = BatchSearch.findOccurrences(refactoring.getPattern(), refactoring.getScope(), w);
        List<MessageImpl> problems = new LinkedList<MessageImpl>(candidates.problems);

        final boolean verify = refactoring.isVerify();

        if (verify) {
            BatchSearch.getVerifiedSpans(candidates, w, new BatchSearch.VerifiedSpansCallBack() {
                public void groupStarted() {}
                public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                    List<PositionBounds> spans = new LinkedList<PositionBounds>();

                    for (ErrorDescription ed : hints) {
                        spans.add(ed.getRange());
                    }

                    refactoringElements.addAll(refactoring, createRefactoringElementImplementation(r.getResolvedFile(), spans, verify));

                    return true;
                }
                public void groupFinished() {}
                public void cannotVerifySpan(Resource r) {
                    refactoringElements.addAll(refactoring, createRefactoringElementImplementation(r.getResolvedFile(), prepareSpansFor(r), verify));
                }
            }, problems);
        } else {
            int[] parts = new int[candidates.projectId2Resources.size()];
            int   index = 0;

            for (Entry<? extends Container, ? extends Collection<? extends Resource>> e : candidates.projectId2Resources.entrySet()) {
                parts[index++] = e.getValue().size();
            }

            ProgressHandleWrapper inner = w.startNextPartWithEmbedding(parts);

            for (Collection<? extends Resource> it :candidates.projectId2Resources.values()) {
                inner.startNextPart(it.size());

                for (Resource r : it) {
                    refactoringElements.addAll(refactoring, createRefactoringElementImplementation(r.getResolvedFile(), prepareSpansFor(r), verify));
                    inner.tick();
                }
            }
        }

        w.finish();

        return problems;
     }

    private static List<PositionBounds> prepareSpansFor(Resource r) {
        List<PositionBounds> spans = new ArrayList<PositionBounds>();

        try {
            FileObject file = r.getResolvedFile();
            DataObject d = DataObject.find(file);
            EditorCookie ec = d.getLookup().lookup(EditorCookie.class);
            CloneableEditorSupport ces = (CloneableEditorSupport) ec;

            spans = new LinkedList<PositionBounds>();

            for (int[] span : r.getCandidateSpans()) {
                PositionRef start = ces.createPositionRef(span[0], Bias.Forward);
                PositionRef end = ces.createPositionRef(span[1], Bias.Forward);

                spans.add(new PositionBounds(start, end));
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }

        return spans;
    }

    private static Collection<RefactoringElementImplementation> createRefactoringElementImplementation(FileObject file, List<PositionBounds> spans, boolean verified) {
        List<RefactoringElementImplementation> result = new LinkedList<RefactoringElementImplementation>();

        try {
            DataObject d = DataObject.find(file);
            LineCookie lc = d.getLookup().lookup(LineCookie.class);

            for (PositionBounds bound : spans) {
                PositionRef start = bound.getBegin();
                PositionRef end = bound.getEnd();
                Line l = lc.getLineSet().getCurrent(start.getLine());
                String lineText = l.getText();

                int boldStart = start.getColumn();
                int boldEnd   = end.getLine() == start.getLine() ? end.getColumn() : lineText.length();

                StringBuilder displayName = new StringBuilder();

                if (!verified) {
                    displayName.append("(not verified) ");
                }

                displayName.append(escapedSubstring(lineText, 0, boldStart).replaceAll("^[ ]*", ""));
                displayName.append("<b>");
                displayName.append(escapedSubstring(lineText, boldStart, boldEnd));
                displayName.append("</b>");
                displayName.append(escapedSubstring(lineText, boldEnd, lineText.length()));

                result.add(new RefactoringElementImpl(file, bound, displayName.toString()));
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return result;
    }

    private Collection<MessageImpl> performApplyPattern(RefactoringElementsBag refactoringElements) {
        return performApplyPattern(refactoring.getPattern(), refactoring.getScope(), refactoringElements);
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

    private static final class RefactoringElementImpl extends SimpleRefactoringElementImplementation {

        private final FileObject file;
        private final PositionBounds span;
        private final String displayName;

        private final Lookup lookup;
        
        public RefactoringElementImpl(FileObject file, PositionBounds span, String displayName) {
            this.file = file;
            this.span = span;
            this.lookup = Lookups.fixed(file);
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
            return file;
        }

        public PositionBounds getPosition() {
            return span;
        }
        
    }
    
}