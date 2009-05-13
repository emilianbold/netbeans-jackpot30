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

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.modules.jackpot30.impl.TestBase;
import org.netbeans.modules.java.hints.infrastructure.Pair;
import org.netbeans.modules.java.source.TreeLoader;

/**
 *
 * @author Jan Lahoda
 */
public class PatternTest extends TestBase {

    public PatternTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
        TreeLoader.DISABLE_CONFINEMENT_TEST = true;
    }

    public void testSimple1() throws Exception {
        performVariablesTest("package test; public class Test {public void test() {int i = |1 + 2|;}}", "$1+$2",
                             new Pair<String, String>("$1", "1"),
                             new Pair<String, String>("$2", "2"));
    }

    public void testTyped1() throws Exception {
        performVariablesTest("package test; public class Test {public void test() {|String.valueOf(\"t\")|;}}", "String.valueOf($1{String})",
                             new Pair<String, String>("$1", "\"t\""));
    }

    public void testTyped2() throws Exception {
        performVariablesTest("package test; public class Test {public void test() {|String.valueOf(\"t\")|;}}", "$2{java.lang.String}.valueOf($1{String})",
                             new Pair<String, String>("$1", "\"t\""),
                             new Pair<String, String>("$2", "String"));
    }

    public void testTyped4() throws Exception {
        performVariablesTest("package test; public class Test {public void test() {|Integer.bitCount(1)|;}}", "$2{java.lang.String}.valueOf($1{String})",
                             (Pair[]) null);
    }

    public void testTyped5() throws Exception {
        performVariablesTest("package test; public class Test {public void test() {java.io.File f = null; |f.toURI().toURL()|;}}", "$1{java.io.File}.toURL()",
                             (Pair[]) null);
    }

    public void testTypedPrimitiveType() throws Exception {
        performVariablesTest("package test; public class Test {public void test(int i) {|test(1)|;}}", "$0{test.Test}.test($1{int})",
                             new Pair<String, String>("$1", "1"));
    }

    public void testMemberSelectVSIdentifier() throws Exception {
        performVariablesTest("package test; public class Test {void test1() {} void test2() {|test1()|;}}", "$1{test.Test}.test1()",
                             new Pair[0]);
    }

    public void testSubClass() throws Exception {
        performVariablesTest("package test; public class Test {void test() {String s = null; |s.toString()|;}}", "$1{java.lang.CharSequence}.toString()",
                             new Pair<String, String>("$1", "s"));
    }

    public void testEquality1() throws Exception {
        performVariablesTest("package test; public class Test {void test() {|test()|;}}", "$1{test.Test}.test()",
                             new Pair[0]);
    }

    public void testEquality2() throws Exception {
        performVariablesTest("package test; public class Test {void test() {String s = null; |String.valueOf(1).charAt(0)|;}}", "$1{java.lang.String}.charAt(0)",
                             new Pair<String, String>("$1", "String.valueOf(1)"));
    }

    public void testEquality3() throws Exception {
        performVariablesTest("package test; public class Test {void test() {String s = null; |s.charAt(0)|;}}", "java.lang.String.valueOf(1).charAt(0)",
                             (Pair[]) null);
    }
    
    public void testType1() throws Exception {
        performVariablesTest("package test; public class Test {void test() {|String| s;}}", "java.lang.String",
                             new Pair[0]);
    }

    public void testStatements1() throws Exception {
        performVariablesTest("package test; public class Test {void test() {|assert true : \"\";|}}", "assert $1{boolean} : $2{java.lang.Object};",
                             new Pair[0]);
    }

    protected void performVariablesTest(String code, String pattern, Pair<String, String>... duplicates) throws Exception {
        String[] split = code.split("\\|");

        assertEquals(Arrays.toString(split), 3, split.length);

        int      start = split[0].length();
        int      end   = start + split[1].length();

        code = split[0] + split[1] + split[2];

        prepareTest("test/Test.java", code);

        TreePath tp = info.getTreeUtilities().pathFor((start + end) / 2);

        while (tp != null) {
            Tree t = tp.getLeaf();
            SourcePositions sp = info.getTrees().getSourcePositions();

            if (   start == sp.getStartPosition(info.getCompilationUnit(), t)
                && end   == sp.getEndPosition(info.getCompilationUnit(), t)) {
                break;
            }

            tp = tp.getParentPath();
        }

        assertNotNull(tp);

        //XXX:
        Map<String, TreePath> vars = Pattern.compile(info, pattern).match(tp);

        if (duplicates == null) {
            assertNull(String.valueOf(vars), vars);
            return ;
        }

        assertNotNull(vars);
        
        Map<String, String> actual = new HashMap<String, String>();
        
        for (Entry<String, TreePath> e : vars.entrySet()) {
            int[] span = new int[] {
                (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), e.getValue().getLeaf()),
                (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), e.getValue().getLeaf())
            };

            actual.put(e.getKey(), info.getText().substring(span[0], span[1]));
        }

        for (Pair<String, String> dup : duplicates) {
            String span = actual.remove(dup.getA());

            if (span == null) {
                fail(dup.getA());
            }
            assertEquals(dup.getA() + ":" + span, span, dup.getB());
        }
    }
    
}
