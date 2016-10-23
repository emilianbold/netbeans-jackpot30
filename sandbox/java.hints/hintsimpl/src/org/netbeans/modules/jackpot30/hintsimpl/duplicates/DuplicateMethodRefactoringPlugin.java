/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.hintsimpl.duplicates;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.util.TreePath;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.java.source.matching.Matcher;
import org.netbeans.api.java.source.matching.Occurrence;
import org.netbeans.api.java.source.matching.Pattern;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.java.spi.JavaRefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;

/**
 *
 * @author lahvac
 */
public class DuplicateMethodRefactoringPlugin extends JavaRefactoringPlugin {
    private final DuplicateMethodRefactoring refactoring;

    DuplicateMethodRefactoringPlugin(DuplicateMethodRefactoring refactoring) {
        this.refactoring = refactoring;
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

    @Override
    public Problem prepare(RefactoringElementsBag refactoringElements) {
        final TreePathHandle source = refactoring.getRefactoringSource().lookup(TreePathHandle.class);
        Set<URL> dependent = new HashSet<>();

        for (FileObject root : ClasspathInfo.create(source.getFileObject()).getClassPath(PathKind.SOURCE).getRoots()) {
            dependent.add(root.toURL());
            dependent.addAll(SourceUtils.getDependentRoots(root.toURL()));
        }

        Set<FileObject> files = new HashSet<>();

        for (URL root : dependent) {
            FileObject rootFO = URLMapper.findFileObject(root);

            if (rootFO == null) continue;

            for (Enumeration<? extends FileObject> en = rootFO.getChildren(true); en.hasMoreElements(); ){
                FileObject file = en.nextElement();

                if (file.isData() && "text/x-java".equals(FileUtil.getMIMEType(file, "text/x-java"))) {
                    files.add(file);
                }
            }
        }

        return createAndAddElements(files, new CancellableTask<WorkingCopy>() {
            @Override public void cancel() {
                //TODO - is used? if yes, implement.
            }
            @Override public void run(WorkingCopy parameter) throws Exception {
                if (parameter.toPhase(org.netbeans.api.java.source.JavaSource.Phase.RESOLVED).compareTo(org.netbeans.api.java.source.JavaSource.Phase.RESOLVED) < 0) {
                    return ;
                }

                Element methodElement = source.getElementHandle().resolve(parameter);
                TreePath method = parameter.getTrees().getPath(methodElement);

                if (method == null) {
                    //nothing to do
                    //TODO: create a problem
                    return ;
                }

                BlockTree content = ((MethodTree) method.getLeaf()).getBody();
                TreePath bodyPath = new TreePath(method, content);
                List<TreePath> statements = new ArrayList<>();

                for (StatementTree st : content.getStatements()) {
                    statements.add(new TreePath(bodyPath, st));
                }

                List<? extends VariableElement> formalParameters = ((ExecutableElement) methodElement).getParameters();
                Pattern pattern = Pattern.createPatternWithRemappableVariables(statements, formalParameters, true);
                Collection<? extends Occurrence> found = Matcher.create(parameter).setCancel(cancelRequested).setSearchRoot(new TreePath(parameter.getCompilationUnit())).match(pattern);
                TreeMaker make = parameter.getTreeMaker();

                for (Occurrence occ : found) {
                    List<ExpressionTree> newParams = new ArrayList<>();
                    for (VariableElement p : formalParameters) {
                        newParams.add((ExpressionTree) occ.getVariablesRemapToTrees().get(p).getLeaf());
                    }
                    MethodInvocationTree newCall = make.MethodInvocation(Collections.<ExpressionTree>emptyList(), make.QualIdent(methodElement), newParams);
                    parameter.rewrite(occ.getOccurrenceRoot().getLeaf(), make.ExpressionStatement(newCall));
                }
            }
        }, refactoringElements, refactoring);
    }

    @Override
    protected JavaSource getJavaSource(Phase p) {
        return null;
    }

}
