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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.jackpot30.hintsimpl.borrowed.RefactoringTestBase;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RefactoringSession;

/**
 *
 * @author lahvac
 */
public class DuplicateMethodRefactoringPluginTest extends RefactoringTestBase {

    public DuplicateMethodRefactoringPluginTest(String name) {
        super(name);
    }

    public void testSimple() throws Exception {
        writeFilesAndWaitForScan(src,
                                 new File("test/Test.java",
                                          "package test;\n" +
                                          "public class Test {\n" +
                                          "    public static void toCall() {\n" +
                                          "        System.err.println(1);\n" +
                                          "    }\n" +
                                          "    public static void test1() {\n" +
                                          "        System.err.println(1);\n" +
                                          "    }\n" +
                                          "    public static void test2() {\n" +
                                          "        System.out.println(1);\n" +
                                          "    }\n" +
                                          "    public static void test3() {\n" +
                                          "        System.err.println(2);\n" +
                                          "    }\n" +
                                          "}\n"));
        performDuplicateMethod("toCall");
        verifyContent(src,
                      new File("test/Test.java",
                               "package test;\n" +
                               "public class Test {\n" +
                               "    public static void toCall() {\n" +
                               "        System.err.println(1);\n" +
                               "    }\n" +
                               "    public static void test1() {\n" +
                               "        toCall();\n" +
                               "    }\n" +
                               "    public static void test2() {\n" +
                               "        System.out.println(1);\n" +
                               "    }\n" +
                               "    public static void test3() {\n" +
                               "        System.err.println(2);\n" +
                               "    }\n" +
                               "}\n"));
    }

    public void testParams() throws Exception {
        writeFilesAndWaitForScan(src,
                                 new File("test/Test.java",
                                          "package test;\n" +
                                          "public class Test {\n" +
                                          "    public static void toCall(int i) {\n" +
                                          "        System.err.println(i);\n" +
                                          "    }\n" +
                                          "    public static void test1(int j) {\n" +
                                          "        System.err.println(1 + j);\n" +
                                          "    }\n" +
                                          "    public static void test3() {\n" +
                                          "        System.err.println(2);\n" +
                                          "    }\n" +
                                          "}\n"));
        performDuplicateMethod("toCall");
        verifyContent(src,
                      new File("test/Test.java",
                               "package test;\n" +
                               "public class Test {\n" +
                               "    public static void toCall(int i) {\n" +
                               "        System.err.println(i);\n" +
                               "    }\n" +
                               "    public static void test1(int j) {\n" +
                               "        toCall(1 + j);\n" +
                               "    }\n" +
                               "    public static void test3() {\n" +
                               "        toCall(2);\n" +
                               "    }\n" +
                               "}\n"));
    }

    private void performDuplicateMethod(final String methodName, Problem... expectedProblems) throws Exception {
        final DuplicateMethodRefactoring[] r = new DuplicateMethodRefactoring[1];

        JavaSource.forFileObject(src.getFileObject("test/Test.java")).runUserActionTask(new Task<CompilationController>() {
            @Override public void run(CompilationController javac) throws Exception {
                javac.toPhase(JavaSource.Phase.RESOLVED);

                for (ExecutableElement method : ElementFilter.methodsIn(javac.getTopLevelElements().get(0).getEnclosedElements())) {
                    if (method.getSimpleName().contentEquals(methodName)) {
                        r[0] = new DuplicateMethodRefactoring(TreePathHandle.create(javac.getTrees().getPath(method), javac));
                    }
                }
            }
        }, true);

        RefactoringSession rs = RefactoringSession.create("Session");
        List<Problem> problems = new LinkedList<Problem>();

        addAllProblems(problems, r[0].preCheck());
        if (!problemIsFatal(problems)) {
            addAllProblems(problems, r[0].prepare(rs));
        }
        if (!problemIsFatal(problems)) {
            addAllProblems(problems, rs.doRefactoring(true));
        }

        assertProblems(Arrays.asList(expectedProblems), problems);
    }
}
