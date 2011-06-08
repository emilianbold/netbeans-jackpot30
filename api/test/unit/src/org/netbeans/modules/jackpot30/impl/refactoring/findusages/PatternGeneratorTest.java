/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.refactoring.findusages;

import com.sun.source.util.TreePath;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.spi.PatternConvertor;
import org.netbeans.modules.java.hints.infrastructure.TreeRuleTestBase;
import org.netbeans.modules.java.hints.jackpot.impl.hints.HintsInvoker;
import org.netbeans.spi.editor.hints.ErrorDescription;

/**
 *
 * @author lahvac
 */
public class PatternGeneratorTest extends TreeRuleTestBase {

    public PatternGeneratorTest(String name) {
        super(name);
    }

    public void testStaticMethod() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public static void te|st(int i, String s) {\n" +
                    "        test(i, s);\n" +
                    "    }\n" +
                    "}\n",
                    "3:8-3:12:verifier:TODO: No display name");
    }

    public void testInstanceMethod() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public void te|st(int i, String s) {\n" +
                    "        new Test().test(i, s);\n" +
                    "        test(i, s);\n" +
                    "    }\n" +
                    "}\n",
                    "3:19-3:23:verifier:TODO: No display name",
                    "4:8-4:12:verifier:TODO: No display name");
    }

    public void testStaticField() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public static int I|I = 0;" +
                    "    public int test() {\n" +
                    "        return II;\n" +
                    "    }\n" +
                    "}\n",
                    "3:15-3:17:verifier:TODO: No display name");
    }

    public void testInstanceField() throws Exception {
        performTest("package test;\n" +
                    "public class Test {\n" +
                    "    public int I|I = 0;" +
                    "    public int test() {\n" +
                    "        if (true) return II; else return new Test().II;\n" +
                    "    }\n" +
                    "}\n",
                    "3:25-3:27:verifier:TODO: No display name",
                    "3:52-3:54:verifier:TODO: No display name");
    }

    private void performTest(String code, String... golden) throws Exception {
        performAnalysisTest("test/Test.java",
                            code,
                            golden);
    }

    @Override
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path) {
        String script = PatternGenerator.generateFindUsagesScript(info, info.getTrees().getElement(path));

        return new HintsInvoker(info, new AtomicBoolean()).computeHints(info, PatternConvertor.create(script));
    }

    @Override
    public void testIssue105979() throws Exception {}

    @Override
    public void testIssue108246() throws Exception {}

    @Override
    public void testIssue113933() throws Exception {}

    @Override
    public void testNoHintsForSimpleInitialize() throws Exception {}

}
