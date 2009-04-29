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

package org.netbeans.modules.jackpot30.hints.pm;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.classpath.ClassPathFactory;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;

/**
 *
 * @author lahvac
 */
public class StringRewriteHint {

    public static class HintImpl extends AbstractHint {
        
        private String id;
        private String from;
        private String to;

        private Kind kind;

        public HintImpl(String id, final String from, String to) throws IOException {
            super(true, false, HintSeverity.WARNING);
            this.id = id;
            this.from = from;
            this.to = to;

//            ClassPath empty = ClassPathSupport.createClassPath(new FileObject[0]);
//            JavaSource.create(ClasspathInfo.create(empty, empty, empty)).runUserActionTask(new Task<CompilationController>() {
//                public void run(CompilationController parameter) throws Exception {
//                    Tree fromTree = parameter.getTreeUtilities().parseExpression(from, new SourcePositions[0]);
//
//                    kind = fromTree.getKind();
//                }
//            }, true);

            kind = Kind.METHOD_INVOCATION;
        }

        @Override
        public String getDescription() {
            return id;
        }

        public Set<Kind> getTreeKinds() {
            return EnumSet.of(kind);
        }

        public List<ErrorDescription> run(CompilationInfo info, TreePath treePath) {
            Tree patternTree = parseExpressionPattern(info, from);

//            Map<String, TreePath> variables = CopyFinder.computeVariables(info, new TreePath(new TreePath(info.getCompilationUnit()), patternTree), treePath, new AtomicBoolean());

//            if (variables == null) {
                return null;
//            }

//            int start = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), treePath.getLeaf());
//            int end   = (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), treePath.getLeaf());

//            return Collections.singletonList(ErrorDescriptionFactory.createErrorDescription(getSeverity().toEditorSeverity(), "Use of Utilities.loadImage", Collections.<Fix>singletonList(new FixImpl("something else", from, to, TreePathHandle.create(treePath, info))), info.getFileObject(), start, end));
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return id;
        }

        public void cancel() {
        }
        
    }

    private static Tree parseExpressionPattern(CompilationInfo info, String pattern) {
        StringBuilder text = new StringBuilder();

        text.append("{ String $1;\n");
        text.append(pattern);
        text.append(";\n}");

        Tree patternTree = info.getTreeUtilities().parseStatement(text.toString(), new SourcePositions[1]);
        Scope scope = info.getTrees().getScope(new TreePath(info.getCompilationUnit()));
        info.getTreeUtilities().attributeTree(patternTree, scope);

        BlockTree bt = (BlockTree) patternTree;

        return ((ExpressionStatementTree) bt.getStatements().get(bt.getStatements().size() - 1)).getExpression();
    }
    
//    private static final class FixImpl implements Fix {
//
//        private String text;
//        private String from;
//        private String to;
//        private TreePathHandle tp;
//
//        public FixImpl(String text, String from, String to, TreePathHandle tp) {
//            this.text = text;
//            this.from = from;
//            this.to = to;
//            this.tp = tp;
//        }
//
//        public String getText() {
//            return text;
//        }
//
//        public ChangeInfo implement() throws Exception {
//            JavaSource.forFileObject(tp.getFileObject()).runModificationTask(new Task<WorkingCopy>() {
//                public void run(final WorkingCopy copy) throws Exception {
//                    copy.toPhase(Phase.RESOLVED);
//                    TreePath treePath = tp.resolve(copy);
//                    Tree patternTree = parseExpressionPattern(copy, from);
//
//                    final Map<String, TreePath> variables = CopyFinder.computeVariables(copy, new TreePath(new TreePath(copy.getCompilationUnit()), patternTree), treePath, new AtomicBoolean());
//
//                    Tree target = parseExpressionPattern(copy, to);
//
//                    new TreePathScanner<Void, Void>() {
//                        @Override
//                        public Void visitIdentifier(IdentifierTree node, Void p) {
//                            TreePath tp = variables.get(node.getName().toString());
//
//                            if (tp != null) {
//                                copy.rewrite(node, tp.getLeaf());
//                            }
//
//                            return super.visitIdentifier(node, p);
//                        }
//                        @Override
//                        public Void visitMemberSelect(MemberSelectTree node, Void p) {
//                            Element e = copy.getTrees().getElement(getCurrentPath());
//
//                            if (e == null || (e.getKind() == ElementKind.CLASS && ((TypeElement) e).asType().getKind() == TypeKind.ERROR)) {
//                                return super.visitMemberSelect(node, p);
//                            }
//
//                            copy.rewrite(node, copy.getTreeMaker().QualIdent(e));
//
//                            return null;
//                        }
//                    }.scan(new TreePath(new TreePath(copy.getCompilationUnit()), target), null);
//
//                    copy.rewrite(treePath.getLeaf(), target);
//                }
//            }).commit();
//
//            return null;
//        }
//
//    }
}
