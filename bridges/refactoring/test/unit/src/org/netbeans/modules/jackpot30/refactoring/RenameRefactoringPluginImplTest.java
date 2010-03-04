/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.refactoring;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.openide.util.lookup.Lookups;

public class RenameRefactoringPluginImplTest extends RefactoringTestBase {

    public RenameRefactoringPluginImplTest(String name) {
        super(name);
    }

    public void testSimple1() throws Exception {
        writeFilesAndWaitForScan(src,
                                 new File("t/A.java", "package t; public class A {\n public void test() {\n}\n }"));
        final RenameRefactoring[] r = new RenameRefactoring[1];

        JavaSource.forFileObject(src.getFileObject("t/A.java")).runUserActionTask(new Task<CompilationController>() {

            public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(JavaSource.Phase.RESOLVED);
                CompilationUnitTree cut = parameter.getCompilationUnit();

                MethodTree var = (MethodTree) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(1);

                TreePath tp = TreePath.getPath(cut, var);
                r[0] = new RenameRefactoring(Lookups.singleton(TreePathHandle.create(tp, parameter)));
            }
        }, true);

        r[0].setNewName("foo");
        r[0].getContext().add(new ExtraData(true, true, true));

        performRefactoring(r[0],
                           new File("t/A.java", "package t; public class A { @Deprecated private void test() { foo(); } public void foo() { } }"),
                           new File("META-INF/upgrade/t.A.hint", " $0.test() :: $0 instanceof t.A => $0.foo() ;; "));
    }

    public void testSimple2() throws Exception {
        writeFilesAndWaitForScan(src,
                                 new File("t/A.java", "package t; public class A {\n public void test() {\n}\n }"),
                                 new File("META-INF/upgrade/t.A.hint", "$0.isDirectory();; "));
        final RenameRefactoring[] r = new RenameRefactoring[1];

        JavaSource.forFileObject(src.getFileObject("t/A.java")).runUserActionTask(new Task<CompilationController>() {

            public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(JavaSource.Phase.RESOLVED);
                CompilationUnitTree cut = parameter.getCompilationUnit();

                MethodTree var = (MethodTree) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(1);

                TreePath tp = TreePath.getPath(cut, var);
                r[0] = new RenameRefactoring(Lookups.singleton(TreePathHandle.create(tp, parameter)));
            }
        }, true);

        r[0].setNewName("foo");
        r[0].getContext().add(new ExtraData(true, /*XXX: false will fail badly here!!*/true, false));

        performRefactoring(r[0],
                           new File("t/A.java", "package t; public class A { private void test() { foo(); } public void foo() { } }"),
                           new File("META-INF/upgrade/t.A.hint", "$0.isDirectory();; $0.test() :: $0 instanceof t.A => $0.foo() ;; "));
    }

}
