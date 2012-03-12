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

package org.netbeans.modules.jackpot30.refactoring;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.java.spi.JavaRefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 * @author Jan Lahoda
 */
public class RenameRefactoringPluginImpl extends JavaRefactoringPlugin {

    private final RenameRefactoring rr;

    public RenameRefactoringPluginImpl(RenameRefactoring rr) {
        this.rr = rr;
    }

    @Override
    public Problem preCheck() {
        return null;
    }

    @Override
    public Problem checkParameters() {
        return null;
    }

    @Override
    public Problem fastCheckParameters() {
        return null;
    }

    @Override
    public void cancelRequest() {
    }

    public Problem prepare(final RefactoringElementsBag refactoringElements) {
        final ExtraData data = rr.getContext().lookup(ExtraData.class);

        if (data == null || !data.generateUpgradeScript) {
            return null;
        }
        
        Lookup source = rr.getRefactoringSource();
        final TreePathHandle toRename = source.lookup(TreePathHandle.class);

        if (toRename == null) {
            return null;
        }
        
        JavaSource js = JavaSource.forFileObject(toRename.getFileObject());

        if (js != null) {
            CancellableTask<WorkingCopy> work = new CancellableTask<WorkingCopy>() {
                public void run(WorkingCopy parameter) throws Exception {
                    parameter.toPhase(JavaSource.Phase.ELEMENTS_RESOLVED);
                    TreePath tp = toRename.resolve(parameter);

                    if (tp == null) {
                        return ;
                    }

                    Element e = parameter.getTrees().getElement(tp);

                    if (e == null || e.getKind() != ElementKind.METHOD) {
                        return ;
                    }

                    MethodTree m = (MethodTree) tp.getLeaf();
                    ClassTree ct = (ClassTree) tp.getParentPath().getLeaf();

                    TreeMaker make = parameter.getTreeMaker();

                    List<ExpressionTree> args = new LinkedList<ExpressionTree>();

                    for (VariableTree vt : m.getParameters()) {
                        args.add(make.Identifier(vt.getName()));
                    }

                    MethodInvocationTree mit = make.MethodInvocation(Collections.<ExpressionTree>emptyList(), make.Identifier(rr.getNewName()), args);

                    StatementTree invoke;

                    if (((ExecutableElement) e).getReturnType().getKind() == TypeKind.VOID) {
                        invoke = make.ExpressionStatement(mit);
                    } else {
                        invoke = make.Return(mit);
                    }
                    
                    BlockTree body = make.Block(Collections.<StatementTree>singletonList(invoke), false);
//                    ModifiersTree mods = make.addModifiersAnnotation(m.getModifiers(), constructTransformation(parameter, make, rr.getNewName(), (ExecutableElement) e));
                    ModifiersTree mods = m.getModifiers();

                    if (data.makeOriginalDeprecated) {
                        mods = make.addModifiersAnnotation(mods, make.Annotation(make.QualIdent(parameter.getElements().getTypeElement("java.lang.Deprecated")), Collections.<ExpressionTree>emptyList()));
                    }

                    if (data.makeOriginalPrivate) {
                        Set<Modifier> modSet = EnumSet.noneOf(Modifier.class);

                        modSet.addAll(mods.getFlags());
                        modSet.removeAll(EnumSet.of(Modifier.PUBLIC, Modifier.PROTECTED));
                        modSet.add(Modifier.PRIVATE);

                        mods = make.Modifiers(modSet, mods.getAnnotations());
                    }

                    MethodTree nue = make.Method(mods, m.getName(), m.getReturnType(), m.getTypeParameters(), m.getParameters(), m.getThrows(), body, null);

                    ClassTree nueClass = make.insertClassMember(ct, ct.getMembers().indexOf(m), nue);

                    parameter.rewrite(ct, nueClass);

                    FileObject root = parameter.getClasspathInfo().getClassPath(PathKind.SOURCE).findOwnerRoot(parameter.getFileObject());

                    if (root != null) {
                        File f = FileUtil.toFile(root);
                        String rule = ScriptGenerator.constructRenameRule(parameter, rr.getNewName(), e);
                        File ruleFile = new File(f, "META-INF/upgrade/" + ((TypeElement) e.getEnclosingElement()).getQualifiedName().toString() + ".hint");
                        
                        refactoringElements.add(rr, new RefactoringElementImpl( ruleFile, rule));
                    }
                }
                public void cancel() {}
            };

            createAndAddElements(Collections.singleton(toRename.getFileObject()), work, refactoringElements, rr);
        }

        return null;
    }

    @Override
    protected JavaSource getJavaSource(Phase p) {
        return null;
    }

}
