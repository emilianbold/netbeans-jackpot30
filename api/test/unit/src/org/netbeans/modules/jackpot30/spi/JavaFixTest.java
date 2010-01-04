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

package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Collections;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.netbeans.modules.jackpot30.impl.TestBase;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.LifecycleManager;
import org.openide.modules.SpecificationVersion;
import org.openide.util.MapFormat;

/**
 *
 * @author Jan Lahoda
 */
public class JavaFixTest extends TestBase {

    public JavaFixTest(String name) {
        super(name);
    }

    public void testSimple() throws Exception {
        SpecificationVersion v = computeSpecVersion("/**\n" +
                                                    " * @since 1.5\n" +
                                                    " */\n");

        assertEquals(0, v.compareTo(new SpecificationVersion("1.5")));
    }
    
    public void testSimpleDate() throws Exception {
        SpecificationVersion v = computeSpecVersion("/**\n" +
                                                    " * @since 1.5 (16 May 2005)\n" +
                                                    " */\n");

        assertEquals(0, v.compareTo(new SpecificationVersion("1.5")));
    }

    public void testLongText() throws Exception {
        SpecificationVersion v = computeSpecVersion("/**\n" +
                                                    " * @since 1.123.2.1 - branch propsheet_issue_29447\n" +
                                                    " */\n");

        assertEquals(0, v.compareTo(new SpecificationVersion("1.123.2.1")));
    }

    public void testModuleName() throws Exception {
        SpecificationVersion v = computeSpecVersion("/**\n" +
                                                    " * @since org.openide.filesystems 7.15\n" +
                                                    " */\n");

        assertEquals(0, v.compareTo(new SpecificationVersion("7.15")));
    }

    public void testModuleNameMajor() throws Exception {
        SpecificationVersion v = computeSpecVersion("/**\n" +
                                                    " * @since org.openide/1 4.42\n" +
                                                    " */\n");

        assertEquals(0, v.compareTo(new SpecificationVersion("4.42")));
    }

    public void testEnd() throws Exception {
        SpecificationVersion v = computeSpecVersion("/**\n" +
                                                    " * @since 1.5 */\n");

        assertEquals(0, v.compareTo(new SpecificationVersion("1.5")));
    }

    public void testOpenAPI() throws Exception {
        SpecificationVersion v = computeSpecVersion("/**\n" +
                                                    " * @since OpenAPI version 2.12" +
                                                    " */\n");

        assertEquals(0, v.compareTo(new SpecificationVersion("2.12")));

    }

    private SpecificationVersion computeSpecVersion(String javadoc) throws Exception {
        prepareTest("test/Test.java",
                    "package test;\n" +
                    "public class Test {\n" +
                    javadoc +
                    "     public void test() {\n" +
                    "     }\n" +
                    "}\n");

        TypeElement te = info.getElements().getTypeElement("test.Test");
        ExecutableElement method = ElementFilter.methodsIn(te.getEnclosedElements()).iterator().next();

        return JavaFix.computeSpecVersion(info, method);
    }

    public void testArithmetic1() throws Exception {
        performArithmeticTest("1 + 2", "3");
        performArithmeticTest("1f + 2", "3.0F");
        performArithmeticTest("1 + 2f", "3.0F");
        performArithmeticTest("1.0 + 2f", "3.0");
        performArithmeticTest("1 + 2.0", "3.0");
        performArithmeticTest("1L + 2", "3L");
    }

    public void testArithmetic2() throws Exception {
        performArithmeticTest("1 * 2", "2");
        performArithmeticTest("1f * 2", "2.0F");
        performArithmeticTest("1 * 2f", "2.0F");
        performArithmeticTest("1.0 * 2f", "2.0");
        performArithmeticTest("1 * 2.0", "2.0");
        performArithmeticTest("1L * 2", "2L");
    }

    public void testArithmetic3() throws Exception {
        performArithmeticTest("4 / 2", "2");
        performArithmeticTest("4f / 2", "2.0F");
        performArithmeticTest("4 / 2f", "2.0F");
        performArithmeticTest("4.0 / 2f", "2.0");
        performArithmeticTest("4 / 2.0", "2.0");
        performArithmeticTest("4L / 2", "2L");
    }

    public void testArithmetic4() throws Exception {
        performArithmeticTest("5 % 2", "1");
        performArithmeticTest("5f % 2", "1.0F");
        performArithmeticTest("5 % 2f", "1.0F");
        performArithmeticTest("5.0 % 2f", "1.0");
        performArithmeticTest("5 % 2.0", "1.0");
        performArithmeticTest("5L % 2", "1L");
    }

    public void testArithmetic5() throws Exception {
        performArithmeticTest("5 - 2", "3");
        performArithmeticTest("5f - 2", "3.0F");
        performArithmeticTest("5 - 2f", "3.0F");
        performArithmeticTest("5.0 - 2f", "3.0");
        performArithmeticTest("5 - 2.0", "3.0");
        performArithmeticTest("5L - 2", "3L");
    }

    public void testArithmetic6() throws Exception {
        performArithmeticTest("5 | 2", "7");
        performArithmeticTest("5L | 2", "7L");
        performArithmeticTest("5 | 2L", "7L");
    }

    public void testArithmetic7() throws Exception {
        performArithmeticTest("5 & 4", "4");
        performArithmeticTest("5L & 4", "4L");
        performArithmeticTest("5 & 4L", "4L");
    }

    public void testArithmetic8() throws Exception {
        performArithmeticTest("5 ^ 4", "1");
        performArithmeticTest("5L ^ 4", "1L");
        performArithmeticTest("5 ^ 4L", "1L");
    }

    public void testArithmetic9() throws Exception {
        performArithmeticTest("5 << 2", "20");
        performArithmeticTest("5L << 2", "20L");
        performArithmeticTest("5 << 2L", "20L");
    }

    public void testArithmeticA() throws Exception {
        performArithmeticTest("-20 >> 2", "-5");
        performArithmeticTest("-20L >> 2", "-5L");
        performArithmeticTest("-20 >> 2L", "-5L");
    }

    public void testArithmeticB() throws Exception {
        performArithmeticTest("-20 >>> 2", "1073741819");
    }

    public void testArithmeticC() throws Exception {
        performArithmeticTest("0 + -20", "-20");
        performArithmeticTest("0 + +20", "20");
    }

    public void testArithmeticComplex() throws Exception {
        performArithmeticTest("1 + 2 * 4 - 5", "4");
        performArithmeticTest("1f + 2 * 4.0 - 5", "4.0");
        performArithmeticTest("1L + 2 * 4 - 5", "4L");
    }

    private static final String ARITHMETIC = "public class Test { private Object o = __VAL__; }";
    private void performArithmeticTest(String orig, String nue) throws Exception {
        String code = replace("0");
        
        prepareTest("Test.java", code);
        ClassTree clazz = (ClassTree) info.getCompilationUnit().getTypeDecls().get(0);
        VariableTree variable = (VariableTree) clazz.getMembers().get(1);
        ExpressionTree init = variable.getInitializer();
        TreePath tp = new TreePath(new TreePath(new TreePath(new TreePath(info.getCompilationUnit()), clazz), variable), init);
        Fix fix = JavaFix.rewriteFix(info, "A", tp, orig, Collections.<String, TreePath>emptyMap(), Collections.<String, Collection<? extends TreePath>>emptyMap(), Collections.<String, String>emptyMap(), Collections.<String, TypeMirror>emptyMap());
        fix.implement();

        String golden = replace(nue);
        String out = doc.getText(0, doc.getLength());

        assertEquals(golden, out);

        LifecycleManager.getDefault().saveAll();
    }

    private static String replace(String val) {
        MapFormat f = new MapFormat(Collections.singletonMap("VAL", val));

        f.setLeftBrace("__");
        f.setRightBrace("__");

        return f.format(ARITHMETIC);
    }
}