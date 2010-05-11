/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.refactoring.noconstructor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.netbeans.api.java.source.GeneratorUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.jackpot30.spi.refactoring.JackpotBasedRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class ReplaceConstructorRefactoringPluginImpl extends JackpotBasedRefactoring {

    private final ReplaceConstructorRefactoring replaceConstructorRefactoring;

    public ReplaceConstructorRefactoringPluginImpl(ReplaceConstructorRefactoring replaceConstructorRefactoring) {
        super(replaceConstructorRefactoring);
        this.replaceConstructorRefactoring = replaceConstructorRefactoring;
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
    protected void prepareAndConstructRule(final Context result) {
        final TreePathHandle constr = replaceConstructorRefactoring.getConstructor();

        try {
            ModificationResult mod = JavaSource.forFileObject(constr.getFileObject()).runModificationTask(new Task<WorkingCopy>() {

                @Override
                public void run(WorkingCopy parameter) throws Exception {
                    parameter.toPhase(Phase.RESOLVED);
                    TreePath constrPath = constr.resolve(parameter);
                    MethodTree constructor = (MethodTree) constrPath.getLeaf();
                    TypeElement parent = (TypeElement) parameter.getTrees().getElement(constrPath.getParentPath());
                    TreeMaker make = parameter.getTreeMaker();
                    StringBuilder parameters = new StringBuilder();
                    StringBuilder constraints = new StringBuilder();
                    StringBuilder realParameters = new StringBuilder();
                    int count = 1;
                    for (VariableTree vt : constructor.getParameters()) {
                        if (count > 1) {
                            parameters.append(", ");
                            constraints.append(" && ");
                            realParameters.append(", ");
                        }
                        realParameters.append(vt.getName());
                        parameters.append("$").append(count);
                        constraints.append("$").append(count).append(" instanceof ").append(parameter.getTrees().getTypeMirror(new TreePath(new TreePath(constrPath, vt), vt.getType())));
                        count++;
                    }
                    EnumSet<Modifier> factoryMods = EnumSet.of(Modifier.STATIC);
                    factoryMods.addAll(constructor.getModifiers().getFlags());
                    MethodTree factory = make.Method(make.Modifiers(factoryMods), replaceConstructorRefactoring.getFactoryName(), make.QualIdent(parent), Collections.<TypeParameterTree>emptyList(), constructor.getParameters(), Collections.<ExpressionTree>emptyList(), "{ return new " + parent.getSimpleName() + "(" + realParameters + "); }", null);
                    parameter.rewrite(constrPath.getParentPath().getLeaf(), GeneratorUtilities.get(parameter).insertClassMember((ClassTree) constrPath.getParentPath().getLeaf(), factory));
                    EnumSet<Modifier> constructorMods = EnumSet.of(Modifier.PRIVATE);
                    parameter.rewrite(constructor.getModifiers(), make.Modifiers(constructorMods));
                    StringBuilder rule = new StringBuilder();
                    rule.append("new ").append(parent.getQualifiedName()).append("(").append(parameters).append(")");
                    if (constraints.length() > 0) {
                        rule.append(" :: ").append(constraints);
                    }
                    rule.append(" => ").append(parent.getQualifiedName()).append(".").append(replaceConstructorRefactoring.getFactoryName()).append("(").append(parameters).append(");;");
                    result.addScript(parent.getQualifiedName().toString(), rule.toString(), ScriptOptions.RUN, ScriptOptions.STORE);
                }
            });

            result.addModificationResult(mod);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}
