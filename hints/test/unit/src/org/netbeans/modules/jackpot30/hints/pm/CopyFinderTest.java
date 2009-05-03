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

package org.netbeans.modules.jackpot30.hints.pm;

import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.Document;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.hints.infrastructure.Pair;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/**
 *
 * @author Jan Lahoda
 */
public class CopyFinderTest extends NbTestCase {// extends org.netbeans.modules.java.hints.introduce.CopyFinderTest {

    public CopyFinderTest(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[0], new Object[0]);
        super.setUp();
    }

    public void testMemberSelectAndIdentifierAreSame() throws Exception {
        performTest("package test; import static java.lang.String.*; public class Test {public void test1() {|String.valueOf(2)|; |valueOf(2)|;} }");
    }

    public void testVariables1() throws Exception {
        performVariablesTest("package test; import static java.lang.String.*; public class Test {public void test1() {String.valueOf(2+4);} }",
                             "java.lang.String.valueOf($1)",
                             new Pair<String, int[]>("$1", new int[] {134 - 31, 137 - 31}));
    }

    public void testAssert1() throws Exception {
        performTest("package test; public class Test {public void test() {int i = 0; |assert i == 1;| |assert i == 1;|}}");
    }

    public void testReturn1() throws Exception {
        performTest("package test; public class Test {public int test1() {|return 1;|} public int test2() {|return 1;|}}");
    }

    public void testIf1() throws Exception {
        performTest("package test; public class Test {public void test() { int i = 0; int j; |if (i == 0) {j = 1;} else {j = 2;}| |if (i == 0) {j = 1;} else {j = 2;}| } }");
    }

    public void testExpressionStatement1() throws Exception {
        performTest("package test; public class Test {public void test() { int i = 0; |i = 1;| |i = 1;| } }");
    }

    public void testBlock1() throws Exception {
        performTest("package test; public class Test {public void test() { int i = 0; |{i = 1;}| |{i = 1;}| } }");
    }

    public void testSynchronized1() throws Exception {
        performTest("package test; public class Test {public void test() { Object o = null; int i = 0; |synchronized (o) {i = 1;}| |synchronized (o) {i = 1;}| } }");
    }

//    public void testEnhancedForLoop() throws Exception {
//        performTest("package test; public class Test {public void test(Iterable<String> i) { |for (String s : i) { System.err.println(); }| |for (String s : i) { System.err.println(); }| }");
//    }

//    public void testConstants() throws Exception {
//        performTest("package test; public class Test {public static final int A = 3; public void test() { int i = |3|; i = |test.Test.A|; } }");
//    }

    public void testOverridingImplementing1() throws Exception {
        doPerformTest("package test; public class Test {public void test() { T t = null; t.test(); test(); } public static class T extends Test { public void test() { } } }", 98 - 22, 104 - 22, -1, 88 - 22, 96 - 22);
    }

    public void testOverridingImplementing2() throws Exception {
        doPerformTest("package test; public class Test {public static class T extends Test { public void test() { Test t = null; t.test(); test(); } } public void test() { } }", 130 - 24, 138 - 24, -1, 140 - 24, 146 - 24);
    }

    protected void performVariablesTest(String code, String pattern, Pair<String, int[]>... duplicates) throws Exception {
        prepareTest(code, -1);

        Tree patternTree = info.getTreeUtilities().parseExpression(pattern, new SourcePositions[1]);
        Scope scope = info.getTrees().getScope(new TreePath(info.getCompilationUnit()));
        info.getTreeUtilities().attributeTree(patternTree, scope);
        Map<TreePath, Map<String, TreePath>> result = CopyFinder.computeDuplicates(info, new TreePath(new TreePath(info.getCompilationUnit()), patternTree), new TreePath(info.getCompilationUnit()), new AtomicBoolean());

        assertSame(1, result.size());

        Map<String, int[]> actual = new HashMap<String, int[]>();

        for (Entry<String, TreePath> e : result.values().iterator().next().entrySet()) {
            int[] span = new int[] {
                (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), e.getValue().getLeaf()),
                (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), e.getValue().getLeaf())
            };

            actual.put(e.getKey(), span);
        }

        for (Pair<String, int[]> dup : duplicates) {
            int[] span = actual.remove(dup.getA());

            if (span == null) {
                fail(dup.getA());
            }
            assertTrue(dup.getA() + ":" + Arrays.toString(span), Arrays.equals(span, dup.getB()));
        }
    }

//    @Override
    protected Collection<TreePath> computeDuplicates(TreePath path) {
        return CopyFinder.computeDuplicates(info, path, new TreePath(info.getCompilationUnit()), new AtomicBoolean()).keySet();
    }

    private void performTest(String code) throws Exception {
        List<int[]> result = new LinkedList<int[]>();

        code = findRegions(code, result);

        int testIndex = 0;
        
        for (int[] i : result) {
            int[] duplicates = new int[2 * (result.size() - 1)];
            int cntr = 0;
            List<int[]> l = new LinkedList<int[]>(result);

            l.remove(i);

            for (int[] span : l) {
                duplicates[cntr++] = span[0];
                duplicates[cntr++] = span[1];
            }

            doPerformTest(code, i[0], i[1], testIndex++, duplicates);
        }
    }

    private void doPerformTest(String code, int start, int end, int testIndex, int... duplicates) throws Exception {
        prepareTest(code, testIndex);

        TreePath path = info.getTreeUtilities().pathFor((start + end) / 2);

        while (path != null) {
            Tree t = path.getLeaf();
            SourcePositions sp = info.getTrees().getSourcePositions();

            if (   start == sp.getStartPosition(info.getCompilationUnit(), t)
                && end   == sp.getEndPosition(info.getCompilationUnit(), t)) {
                break;
            }

            path = path.getParentPath();
        }

        assertNotNull(path);

        Collection<TreePath> result = computeDuplicates(path);

        //        assertEquals(f.result.toString(), duplicates.length / 2, f.result.size());

        int[] dupes = new int[result.size() * 2];
        int   index = 0;

        for (TreePath tp : result) {
            dupes[index++] = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), tp.getLeaf());
            dupes[index++] = (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), tp.getLeaf());
        }

        assertTrue("Was: " + Arrays.toString(dupes) + " should have been: " + Arrays.toString(duplicates), Arrays.equals(duplicates, dupes));
    }

    private static String findRegions(String code, List<int[]> regions) {
        String[] split = code.split("\\|");
        StringBuilder filtered = new StringBuilder();

        filtered.append(split[0]);

        int offset = split[0].length();
        
        for (int cntr = 1; cntr < split.length; cntr += 2) {
            int[] i = new int[] {
                offset,
                offset + split[cntr].length()
            };

            regions.add(i);
            
            filtered.append(split[cntr]);
            filtered.append(split[cntr + 1]);

            offset += split[cntr].length();
            offset += split[cntr + 1].length();
        }

        return filtered.toString();
    }


    protected void prepareTest(String code, int testIndex) throws Exception {
        File workDirWithIndexFile = testIndex != (-1) ? new File(getWorkDir(), Integer.toString(testIndex)) : getWorkDir();
        FileObject workDirWithIndex = FileUtil.toFileObject(workDirWithIndexFile);

        if (workDirWithIndex != null) {
            workDirWithIndex.delete();
        }

        workDirWithIndex = FileUtil.createFolder(workDirWithIndexFile);

        assertNotNull(workDirWithIndexFile);

        FileObject sourceRoot = workDirWithIndex.createFolder("src");
        FileObject buildRoot  = workDirWithIndex.createFolder("build");
        FileObject cache = workDirWithIndex.createFolder("cache");

        FileObject data = FileUtil.createData(sourceRoot, "test/Test.java");

        TestUtilities.copyStringToFile(data, code);

        data.refresh();

        SourceUtilsTestUtil.prepareTest(sourceRoot, buildRoot, cache);

        DataObject od = DataObject.find(data);
        EditorCookie ec = od.getLookup().lookup(EditorCookie.class);

        assertNotNull(ec);

        doc = ec.openDocument();

        doc.putProperty(Language.class, JavaTokenId.language());

        JavaSource js = JavaSource.forFileObject(data);

        assertNotNull(js);

        info = SourceUtilsTestUtil.getCompilationInfo(js, Phase.RESOLVED);

        assertNotNull(info);
    }

    protected CompilationInfo info;
    private Document doc;

}
