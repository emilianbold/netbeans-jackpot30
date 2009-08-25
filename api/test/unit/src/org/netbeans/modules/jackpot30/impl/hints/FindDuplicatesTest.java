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

import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;
import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.impl.TestBase;
import org.netbeans.modules.java.source.pretty.VeryPretty;

/**
 *
 * @author lahvac
 */
public class FindDuplicatesTest extends TestBase {

    public FindDuplicatesTest(String name) {
        super(name);
    }

    public void testLocalVariablesGeneralization() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        |int i = 0;\n" +
                    "        i++;\n" +
                    "        int j = i;|\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "{\n" +
                    "    $s0$;\n" +
                    "    int $0 = 0;\n" +
                    "    $0++;\n" +
                    "    int $1 = $0;\n" +
                    "    $s1$;\n" +
                    "}");
    }
    
    public void testSingleStatementPattern() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        int i = 0;\n" +
                    "        |i++;|\n" +
                    "        int j = i;\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "$0++;");
    }

    public void testExpressionPattern() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        int i = 0;\n" +
                    "        i++;\n" +
                    "        int j = |i|;\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "$0");
    }

    public void testVoidMethod() throws Exception {
        performGeneralizationTest("package test;\n" +
                    "public class Test {\n" +
                    "    private void test() {\n" +
                    "        |System.err.println()|;\n" +
                    "    }\n" +
                    "}\n" +
                    "",
                    "System.err.println()");
    }

    private void performGeneralizationTest(String code, String generalized) throws Exception {
        String[] split = code.split(Pattern.quote("|"));

        assertEquals(3, split.length);

        int start = split[0].length();
        int end   = split[1].length() + start;
        
        code = split[0] + split[1] + split[2];

        prepareTest("test/Test.java", code);

        Tree generalizedTree = FindDuplicates.resolveAndGeneralizePattern(info, start, end);
        VeryPretty vp = new VeryPretty(info);

        vp.print((JCTree) generalizedTree);
        
        String repr = vp.toString();
        
        assertEquals(generalized.replaceAll("[ \n\t]+", " "),
                     repr.replaceAll("[ \n\t]+", " "));
    }

}