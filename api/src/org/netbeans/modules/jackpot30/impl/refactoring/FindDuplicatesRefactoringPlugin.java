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
import javax.swing.text.Position.Bias;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.refactoring.api.Problem;
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

public class FindDuplicatesRefactoringPlugin implements RefactoringPlugin {

    private final FindDuplicatesRefactoring refactoring;

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
        //TODO
    }

    public Problem prepare(RefactoringElementsBag refactoringElements) {
        BatchResult candidates = BatchSearch.findOccurrences(refactoring.getPattern(), refactoring.getScope());

        for (Iterable<? extends Resource> it :candidates.projectId2Resources.values()) {
            for (Resource r : it) {
                refactoringElements.addAll(refactoring, createRefactoringElementImplementation(r, refactoring.isVerify()));
            }
        }

        return null;
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