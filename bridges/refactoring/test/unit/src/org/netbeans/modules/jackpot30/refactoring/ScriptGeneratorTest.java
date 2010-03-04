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

package org.netbeans.modules.jackpot30.refactoring;

import com.sun.source.util.TreePath;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.modules.jackpot30.impl.TestBase;

/**
 *
 * @author lahvac
 */
public class ScriptGeneratorTest extends TestBase {
    
    public ScriptGeneratorTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[0], new Object[0]);
        super.setUp();
    }

    public void testInstanceMethod() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public void te|st() {}" +
                    "}\n",
                    "foo",
                    "   $0.test() :: $0 instanceof test.Test\n" +
                    "=> $0.foo()\n" +
                    ";;");
    }

    public void testInstanceMethodParam() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public void te|st(int i) {}" +
                    "}\n",
                    "foo",
                    "   $0.test($i) :: $0 instanceof test.Test && $i instanceof int\n" +
                    "=> $0.foo($i)\n" +
                    ";;");
    }

    public void testStaticMethod() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public static void te|st() {}" +
                    "}\n",
                    "foo",
                    "   test.Test.test()\n" +
                    "=> test.Test.foo()\n" +
                    ";;");
    }

    public void testStaticMethodParam() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public static void te|st(int i) {}" +
                    "}\n",
                    "foo",
                    "   test.Test.test($i) :: $i instanceof int\n" +
                    "=> test.Test.foo($i)\n" +
                    ";;");
    }

    private void performTest(String code, String newName, String script) throws Exception {
        assertEquals(2, code.split(Pattern.quote("|")).length);

        int pos = code.indexOf('|');

        code = code.substring(0, pos) + code.substring(pos + 1);

        prepareTest("test/Test.java", code);

        TreePath path = info.getTreeUtilities().pathFor(pos);
        Element el = info.getTrees().getElement(path);

        assertNotNull(el);

        assertEquals(script, ScriptGenerator.constructRenameRule(null, newName, el));
    }
}
