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

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.util.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import javax.swing.text.Document;
import junit.framework.TestSuite;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.EncodingContext;
import org.netbeans.modules.java.source.TreeLoader;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import static org.junit.Assert.*;

/**
 *
 * @author lahvac
 */
public abstract class BulkSearchTestPerformer extends NbTestCase {

    public BulkSearchTestPerformer(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
        TreeLoader.DISABLE_CONFINEMENT_TEST = true;
    }

//    public static TestSuite suite() {
//        NbTestSuite s = new NbTestSuite();
//
//        s.addTestSuite(NFABasedBulkSearchTest.class);
//
//        return s;
//    }

    public void testSimple1() throws Exception {
        performTest("package test; public class Test { private void test() { System.err./**/println(\"\");}}",
                    Collections.singletonMap("System.err.println(\"\")", Arrays.asList("System.err./**/println(\"\")")),
                    Arrays.asList("System.err.println(\"\" + \"\")"));
    }

    public void testDontCare() throws Exception {
        performTest("package test; public class Test { private void test() { System.err./**/println(\"\" + \"\");}}",
                    Collections.singletonMap("System.err.println($1)", Arrays.asList("System.err./**/println(\"\" + \"\")")),
                    Collections.<String>emptyList());
    }

    public void testMemberSelectAndIdentifier() throws Exception {
        performTest("package test; public class Test { private static void test() { test();}}",
                    Collections.singletonMap("test.Test.test()", Arrays.asList("test()")),
                    Collections.<String>emptyList());
    }

    public void testUnpureMemberSelect() throws Exception {
        performTest("package test; public class Test { private static void test() { new StringBuilder().append(\"\");}}",
                    Collections.<String, List<String>>emptyMap(),
                    Arrays.asList("test.append(\"\")"));
    }

    public void testMemberSelectWithVariables1() throws Exception {
        performTest("package test; public class Test { private static void test() { new StringBuilder().append(\"\");}}",
                    Collections.singletonMap("$0.append(\"\")", Arrays.asList("new StringBuilder().append(\"\")")),
                    Collections.<String>emptyList());
    }

    public void testMemberSelectWithVariables2() throws Exception {
        performTest("package test; public class Test { private void append(char c) { append(\"\");}}",
                    Collections.singletonMap("$0.append(\"\")", Arrays.asList("append(\"\")")),
                    Collections.<String>emptyList());
    }

    public void testLocalVariables() throws Exception {
        performTest("package test; public class Test { private void test() { { int y; y = 1; } }}",
                    Collections.singletonMap("{ int $1; $1 = 1; }", Arrays.asList("{ int y; y = 1; }")),
                    Collections.<String>emptyList());
    }

    public void testAssert() throws Exception {
        performTest("package test; public class Test { private void test() { assert true : \"\"; }}",
                    Collections.singletonMap("assert $1 : $2;", Arrays.asList("assert true : \"\";")),
                    Collections.<String>emptyList());
    }

    public void testStatementAndSingleBlockStatementAreSame1() throws Exception {
        performTest("package test; public class Test { private void test() { { int y; { y = 1; } } }}",
                    Collections.singletonMap("{ int $1; $1 = 1; }", Arrays.asList("{ int y; { y = 1; } }")),
                    Collections.<String>emptyList());
    }

    public void testStatementAndSingleBlockStatementAreSame2() throws Exception {
        performTest("package test; public class Test { private void test() { { int y; y = 1; } }}",
                    Collections.singletonMap("{ int $1; { $1 = 1; } }", Arrays.asList("{ int y; y = 1; }")),
                    Collections.<String>emptyList());
    }

    public void testStatementVariables1() throws Exception {
        performTest("package test; public class Test { public int test1() { if (true) return 1; else return 2; } }",
                    Collections.singletonMap("if ($1) $2; else $3;", Arrays.asList("if (true) return 1; else return 2;")),
                    Collections.<String>emptyList());
    }

    public void testMultiStatementVariables1() throws Exception {
        performTest("package test; public class Test { public int test1(int i) { System.err.println(i); System.err.println(i); i = 3; System.err.println(i); System.err.println(i); return i; } }",
                    Collections.singletonMap("{ $s1$; i = 3; $s2$; return i; }", Arrays.asList("{ System.err.println(i); System.err.println(i); i = 3; System.err.println(i); System.err.println(i); return i; }")),
                    Collections.<String>emptyList());
    }

    public void testMultiStatementVariables2() throws Exception {
        performTest("package test; public class Test { public int test1(int i) { i = 3; return i; } }",
                    Collections.singletonMap("{ $s1$; i = 3; $s2$; return i; }", Arrays.asList("{ i = 3; return i; }")),
                    Collections.<String>emptyList());
    }

    public void testMultiStatementVariablesAndBlocks1() throws Exception {
        performTest("package test; public class Test { public void test1() { if (true) System.err.println(); } }",
                    Collections.singletonMap("if ($c) {$s1$; System.err.println(); $s2$; }", Arrays.asList("if (true) System.err.println();")),
                    Collections.<String>emptyList());
    }

    public void testMultiStatementVariablesAndBlocks2() throws Exception {
        performTest("package test; public class Test { public void test1() { if (true) System.err.println(); } }",
                    Collections.singletonMap("if ($c) {$s1$; System.err.println(); }", Arrays.asList("if (true) System.err.println();")),
                    Collections.<String>emptyList());
    }

    public void testMultiStatementVariablesAndBlocks3() throws Exception {
        performTest("package test; public class Test { public void test1() { if (true) System.err.println(); } }",
                    Collections.singletonMap("if ($c) {System.err.println(); $s2$; }", Arrays.asList("if (true) System.err.println();")),
                    Collections.<String>emptyList());
    }

    public void testMultiStatementVariablesAndBlocks4() throws Exception {
        performTest("package test; public class Test { public void test1() { if (true) System.err.println(); } }",
                    Collections.singletonMap("{ $s1$; System.err.println(); $s2$; }", Arrays.asList("System.err.println();")),
                    Collections.<String>emptyList());
    }

    public void XtestMultiVariablesInMethodInvocation1() throws Exception {
        performTest("package test; public class Test { public static void test() { test(); } }",
                    Collections.singletonMap("test.Test.test($params$)", Arrays.asList("test()")),
                    Collections.<String>emptyList());
    }

    public void testTwoPatterns() throws Exception {
        Map<String, List<String>> contained = new HashMap<String, List<String>>();

        contained.put("if ($a) $ret = $b; else $ret = $c;", Arrays.asList("if (b) q = 2; else q = 3;"));
        contained.put("{ $p$; $T $v; if($a) $v = $b; else $v = $c; $q$; }", Arrays.asList("{ int q; if (b) q = 2; else q = 3; }"));

        performTest("package test; public class Test { public void test1(boolean b) { int q; if (b) q = 2; else q = 3; } }",
                    contained,
                    Collections.<String>emptyList());
    }

    public void testEffectiveNewClass() throws Exception {
        performTest("package test; import javax.swing.ImageIcon; public class Test { public void test1(java.awt.Image i) { new ImageIcon(i); new String(i); } }",
                    Collections.singletonMap("new javax.swing.ImageIcon($1)", Arrays.asList("new ImageIcon(i)")),
                    Collections.<String>emptyList());
    }

    public void testSynchronizedAndMultiStatementVariables() throws Exception {
        performTest("package test; public class Test {public void test() { Object o = null; int i = 0; synchronized (o) {} } }",
                    Collections.singletonMap("synchronized($var) {$stmts$;}", Arrays.asList("synchronized (o) {}")),
                    Collections.<String>emptyList());
    }

    public void testJackpot30_2() throws Exception {
        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void m() {\n" +
                      "        a(c.i().getFileObject());\n" +
                      "        if (span != null && span[0] != (-1) && span[1] != (-1));\n" +
                      "    }\n" +
                      "}\n";

        performTest(code,
                    Collections.<String, List<String>>emptyMap(),
                    Arrays.asList("$0.getFileObject($1)"));
    }

    public void testIdentifierInPureMemberSelect() throws Exception {
        String code = "package test;\n" +
                       "public class Test {\n" +
                       "     public Test test;\n" +
                       "     public String name;\n" +
                       "     private void test() {\n" +
                       "         Test t = null;\n" +
                       "         String s = t.test.name;\n" +
                       "     }\n" +
                       "}\n";

        performTest(code,
                    Collections.singletonMap("$Test.test", Arrays.asList("test", "t.test")),
                    Collections.<String>emptyList());
    }

    public void testNoExponentialTimeComplexity() throws Exception {
        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void test() {\n" +
                      "        Object o;\n" +
                      "        if(o == null) {\n" +
                      "            f(\"\");\n" +
                      "        }|\n" +
                      "    }\n" +
                      "}";
        String pattern = "{ $p$; $T $v; if($a) $v = $b; else $v = $c; }";

        measure(code, "\na(\"\");", 5, pattern); //to load needed classes, etc.

        int rep = 1;
        long baseline;

        while (true) {
            baseline = measure(code, "\na(\"\");", rep, pattern);

            if (baseline >= 2000) {
                break;
            }

            rep *= 2;
        }

        long doubleSize = measure(code, "\na(\"\");", 2 * rep, pattern);

        assertTrue("baseline=" + baseline + ", actual=" + String.valueOf(doubleSize), doubleSize <= 4 * baseline);
    }

    private long measure(String baseCode, String toInsert, int repetitions, String pattern) throws Exception {
        int pos = baseCode.indexOf('|');

        assertTrue(pos != (-1));

        baseCode = baseCode.replaceAll(Pattern.quote("|"), "");
        
        StringBuilder code = new StringBuilder(baseCode.length() + repetitions * toInsert.length());

        code.append(baseCode);
        
        while (repetitions-- > 0) {
            code.insert(pos, toInsert);
        }

        long startTime = System.currentTimeMillis();

        performTest(code.toString(),
                    Collections.<String, List<String>>emptyMap(),
                    Arrays.asList(pattern));

        long endTime = System.currentTimeMillis();

        return endTime - startTime;
    }

    public void XtestMeasureTime() throws Exception {
        String code = TestUtilities.copyFileToString(new File("/usr/local/home/lahvac/src/nb//outgoing/java.editor/src/org/netbeans/modules/editor/java/JavaCompletionProvider.java"));
        List<String> patterns = new LinkedList<String>();

        for (int cntr = 0; cntr < 1000; cntr++) {
            patterns.add("System.err.println($1)");
        }

        performTest(code,
                    Collections.<String, List<String>>emptyMap(),
                    patterns);
    }

    public void testMatches1() throws Exception {
        performMatchesTest("package test; public class Test { private void test() { f.isDirectory(); } }", Arrays.asList("$1.isDirectory()", "new ImageIcon($1)"), true);
    }

    public void testSerialization() throws Exception {
        String text = "package test; public class Test { public void test1(boolean b) { int q; if (b) q = 2; else q = 3; } }";

        prepareTest("test/Test.java", text);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EncodingContext ec = new EncodingContext(out);

        createSearch().encode(info.getCompilationUnit(), ec);
        
        boolean matches = createSearch().matches(new ByteArrayInputStream(out.toByteArray()), createSearch().create(info, "{ $p$; $T $v; if($a) $v = $b; else $v = $c; $q$; }"));

        assertTrue(matches);
    }

    protected abstract BulkSearch createSearch();
    
    private void performMatchesTest(String text, List<String> patterns, boolean golden) throws Exception {
        prepareTest("test/Test.java", text);

        BulkPattern p = createSearch().create(info, patterns);

        boolean result = createSearch().matches(info, info.getCompilationUnit(), p);

        assertEquals(golden, result);
    }
    
    private void performTest(String text, Map<String, List<String>> containedPatterns, Collection<String> notContainedPatterns) throws Exception {
        prepareTest("test/Test.java", text);

        List<String> patterns = new LinkedList<String>();

        patterns.addAll(containedPatterns.keySet());
        patterns.addAll(notContainedPatterns);

        long s1 = System.currentTimeMillis();
        BulkPattern p = createSearch().create(info, patterns);
        long e1 = System.currentTimeMillis();

//        System.err.println("create: " + (e1 - s1));

        long s2 = System.currentTimeMillis();
        Map<String, Collection<TreePath>> result = createSearch().match(info, info.getCompilationUnit(), p);
        long e2 = System.currentTimeMillis();

//        System.err.println("match: " + (e2 - s2));

        assertTrue(result.toString(), result.keySet().containsAll(containedPatterns.keySet()));

        for (Entry<String, Collection<TreePath>> e : result.entrySet()) {
            List<String> actual = new LinkedList<String>();

            for (TreePath tp : e.getValue()) {
                assertNotNull(TreePathHandle.create(tp, info).resolve(info));
                
                int start = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), tp.getLeaf());
                int end   = (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), tp.getLeaf());

                actual.add(info.getText().substring(start, end));
            }

            assertEquals(e.getKey(), containedPatterns.get(e.getKey()), actual);
        }


        Set<String> none = new HashSet<String>(result.keySet());

        none.retainAll(notContainedPatterns);

        assertTrue(none.isEmpty());

        if (!verifyIndexingData())
            return ;
        
        //ensure the returned identifiers/treeKinds are correct:
        EncodingContext ec = new EncodingContext(new ByteArrayOutputStream());
        
        createSearch().encode(info.getCompilationUnit(), ec);

        for (int i = 0; i < containedPatterns.size(); i++) {
            assertTrue("expected: " + p.getIdentifiers().get(i) + ", but exist only: " + ec.getIdentifiers(), ec.getIdentifiers().containsAll(p.getIdentifiers().get(i)));
            assertTrue("expected: " + p.getKinds().get(i) + ", but exist only: " + ec.getKinds(), ec.getKinds().containsAll(p.getKinds().get(i)));
        }
    }
    
    private void prepareTest(String fileName, String code) throws Exception {
        clearWorkDir();

        FileUtil.refreshFor(File.listRoots());

        FileObject workFO = FileUtil.toFileObject(getWorkDir());

        assertNotNull(workFO);

        workFO.refresh();

        sourceRoot = workFO.createFolder("src");
        FileObject buildRoot  = workFO.createFolder("build");
        FileObject cache = workFO.createFolder("cache");

        FileObject data = FileUtil.createData(sourceRoot, fileName);
        File dataFile = FileUtil.toFile(data);

        assertNotNull(dataFile);

        TestUtilities.copyStringToFile(dataFile, code);

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

    private FileObject sourceRoot;
    private CompilationInfo info;
    private Document doc;

    protected boolean verifyIndexingData() {
        return true;
    }
}
