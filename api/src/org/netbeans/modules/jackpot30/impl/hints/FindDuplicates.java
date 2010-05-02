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

package org.netbeans.modules.jackpot30.impl.hints;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.swing.SwingUtilities;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.JavaSource.Priority;
import org.netbeans.api.java.source.JavaSourceTaskFactory;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.support.CaretAwareJavaSourceTaskFactory;
import org.netbeans.api.java.source.support.SelectionAwareJavaSourceTaskFactory;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.impl.refactoring.FindDuplicatesRefactoringUI;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class FindDuplicates implements CancellableTask<CompilationInfo> {

    public void run(CompilationInfo info) throws Exception {
        Collection<? extends ErrorDescription> eds = computeErrorDescription(info);

        if (eds == null) {
            eds = Collections.emptyList();
        }

        HintsController.setErrors(info.getFileObject(), FindDuplicates.class.getName(), eds);
    }

    private Collection<? extends ErrorDescription> computeErrorDescription(CompilationInfo info) throws Exception {
        int[] span = SelectionAwareJavaSourceTaskFactory.getLastSelection(info.getFileObject());

        if (span == null) {
            return null;
        }

        TreePath selection = selectionForExpressionHack(info, span[0], span[1]);

        if (selection == null) {
            if (selectionForStatementsHack(info, span[0], span[1], new int[2]) == null) {
                return null;
            }
        }

        Fix f = new FixImpl(info.getFileObject(), span[0], span[1]);
        int caret = CaretAwareJavaSourceTaskFactory.getLastPosition(info.getFileObject());
        ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(Severity.HINT, "Look for Duplicates", Collections.singletonList(f), info.getFileObject(), caret, caret);

        return Collections.singletonList(ed);
    }

    public void cancel() {
        //XXX
    }

    static Tree resolveAndGeneralizePattern(CompilationInfo info, int start, int end) {
        TreePath selection = selectionForExpressionHack(info, start, end);

        if (selection == null) {
            int[] statementSpan = new int[2];
            TreePathHandle statementSelection = selectionForStatementsHack(info, start, end, statementSpan);

            if (statementSelection == null) {
                return null;
            }

            selection = statementSelection.resolve(info);

            if (selection == null) {
                return null; //XXX
            }

            return Utilities.generalizePattern(info, selection, statementSpan[0], statementSpan[1]);
        } else {
            return Utilities.generalizePattern(info, selection);
        }
    }

    private static final class FixImpl implements Fix {

        private final FileObject file;
        private final int start;
        private final int end;

        public FixImpl(FileObject file, int start, int end) {
            this.file = file;
            this.start = start;
            this.end = end;
        }

        public String getText() {
            return "Look for Duplicates in Opened Projects";
        }

        public ChangeInfo implement() throws Exception {
            JavaSource.forFileObject(file).runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController cc) throws Exception {
                    cc.toPhase(Phase.RESOLVED);

                    Tree generalized = resolveAndGeneralizePattern(cc, start, end);

                    if (generalized == null) {
                        return ;
                    }

                    final String pattern = generalized.toString(); //XXX

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            UI.openRefactoringUI(new FindDuplicatesRefactoringUI(pattern, Scope.createAllOpenedProjectsScope()));
                        }
                    });
                }
            }, true);
            
            return null;
        }

    }

    private static final Set<TypeKind> NOT_ACCEPTED_TYPES = EnumSet.of(TypeKind.NONE, TypeKind.OTHER);
    
    static TreePath selectionForExpressionHack(CompilationInfo info, int start, int end) {
        return validateSelection(info, start, end, NOT_ACCEPTED_TYPES);
    }

    static TreePathHandle selectionForStatementsHack(CompilationInfo info, int start, int end, int[] outSpan)  {
        return validateSelectionForIntroduceMethod(info, start, end, outSpan);
    }

    private static final Set<JavaTokenId> WHITESPACES = EnumSet.of(JavaTokenId.WHITESPACE, JavaTokenId.BLOCK_COMMENT, JavaTokenId.LINE_COMMENT, JavaTokenId.JAVADOC_COMMENT);
    static int[] ignoreWhitespaces(CompilationInfo ci, int start, int end) {
        TokenSequence<JavaTokenId> ts = ci.getTokenHierarchy().tokenSequence(JavaTokenId.language());

        if (ts == null) {
            return new int[] {start, end};
        }

        ts.move(start);

        if (ts.moveNext()) {
            boolean wasMoveNext = true;

            while (WHITESPACES.contains(ts.token().id()) && (wasMoveNext = ts.moveNext()))
                ;

            if (wasMoveNext && ts.offset() > start)
                start = ts.offset();
        }

        ts.move(end);

        while (ts.movePrevious() && WHITESPACES.contains(ts.token().id()) && ts.offset() < end)
            end = ts.offset();

        return new int[] {start, end};
    }
    
    private static boolean isInsideClass(TreePath tp) {
        while (tp != null) {
            if (tp.getLeaf().getKind() == Kind.CLASS)
                return true;

            tp = tp.getParentPath();
        }

        return false;
    }
    
    private static TreePath validateSelection(CompilationInfo ci, int start, int end, Set<TypeKind> ignoredTypes) {
        TreePath tp = ci.getTreeUtilities().pathFor((start + end) / 2 + 1);

        for ( ; tp != null; tp = tp.getParentPath()) {
            Tree leaf = tp.getLeaf();

            if (!ExpressionTree.class.isAssignableFrom(leaf.getKind().asInterface()))
               continue;

            long treeStart = ci.getTrees().getSourcePositions().getStartPosition(ci.getCompilationUnit(), leaf);
            long treeEnd   = ci.getTrees().getSourcePositions().getEndPosition(ci.getCompilationUnit(), leaf);

            if (treeStart != start || treeEnd != end) {
                continue;
            }

            TypeMirror type = ci.getTrees().getTypeMirror(tp);

            if (type != null && type.getKind() == TypeKind.ERROR) {
                type = ci.getTrees().getOriginalType((ErrorType) type);
            }

            if (type == null || ignoredTypes.contains(type.getKind()))
                continue;

            if(tp.getLeaf().getKind() == Kind.ASSIGNMENT)
                continue;

            if (tp.getLeaf().getKind() == Kind.ANNOTATION)
                continue;

            if (!isInsideClass(tp))
                return null;

            TreePath candidate = tp;

            tp = tp.getParentPath();

            while (tp != null) {
                switch (tp.getLeaf().getKind()) {
                    case VARIABLE:
                        VariableTree vt = (VariableTree) tp.getLeaf();
                        if (vt.getInitializer() == leaf) {
                            return candidate;
                        } else {
                            return null;
                        }
                    case NEW_CLASS:
                        NewClassTree nct = (NewClassTree) tp.getLeaf();

                        if (nct.getIdentifier().equals(candidate.getLeaf())) { //avoid disabling hint ie inside of anonymous class higher in treepath
                            for (Tree p : nct.getArguments()) {
                                if (p == leaf) {
                                    return candidate;
                                }
                            }

                            return null;
                        }
                }

                leaf = tp.getLeaf();
                tp = tp.getParentPath();
            }

            return candidate;
        }

        return null;
    }

    private static TreePathHandle validateSelectionForIntroduceMethod(CompilationInfo ci, int start, int end, int[] statementsSpan) {
        int[] span = ignoreWhitespaces(ci, Math.min(start, end), Math.max(start, end));

        start = span[0];
        end   = span[1];

        if (start >= end)
            return null;

        TreePath tpStart = ci.getTreeUtilities().pathFor(start);
        TreePath tpEnd = ci.getTreeUtilities().pathFor(end);

        if (tpStart.getLeaf() != tpEnd.getLeaf() || tpStart.getLeaf().getKind() != Kind.BLOCK) {
            //??? not in the same block:
            return null;
        }

        int from = -1;
        int to   = -1;

        BlockTree block = (BlockTree) tpStart.getLeaf();
        int index = 0;

        for (StatementTree s : block.getStatements()) {
            long sStart = ci.getTrees().getSourcePositions().getStartPosition(ci.getCompilationUnit(), s);

            if (sStart == start) {
                from = index;
            }

            if (end < sStart && to == (-1)) {
                to = index - 1;
            }

            index++;
        }

        if (from == (-1)) {
            return null;
        }

        if (to == (-1))
            to = block.getStatements().size() - 1;

        if (to < from) {
            return null;
        }

        statementsSpan[0] = from;
        statementsSpan[1] = to;

        return TreePathHandle.create(tpStart, ci);
    }

    @ServiceProvider(service=JavaSourceTaskFactory.class)
    public static final class FactoryImpl extends SelectionAwareJavaSourceTaskFactory {

        public FactoryImpl() {
            super(Phase.RESOLVED, Priority.LOW);
        }

        @Override
        protected CancellableTask<CompilationInfo> createTask(FileObject file) {
            return new FindDuplicates();
        }
        
    }

}
