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
package org.netbeans.modules.jackpot30.compiler;

import com.sun.tools.javac.Main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.hints.jackpot.code.spi.Hint;
import org.netbeans.modules.java.hints.jackpot.code.spi.TriggerCompileTime;
import org.netbeans.modules.java.hints.jackpot.code.spi.TriggerPattern;
import org.netbeans.modules.java.hints.jackpot.spi.HintContext;
import org.netbeans.modules.java.hints.jackpot.spi.JavaFix;
import org.netbeans.modules.java.hints.jackpot.spi.support.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;

/**
 *
 * @author lahvac
 */
public class HintsAnnotationProcessingTest extends NbTestCase {

    public HintsAnnotationProcessingTest(String name) {
        super(name);
    }

    public void testRunCompiler1() throws Exception {
        String golden =
                "--- {0}/src/test/Test.java\n" +
                "+++ {0}/src/test/Test.java\n" +
                "@@ -1 +1 @@\n" +
                "-package test; public class Test {private void test(java.io.File f) {f.isDirectory();}}\n" +
                "+package test; public class Test {private void test(java.io.File f) {!f.isFile();}}\n";

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test(java.io.File f) {f.isDirectory();}}\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\n$1.isDirectory() :: $1 instanceof java.io.File => !$1.isFile();;");
    }

    public void testRunCompiler2() throws Exception {
        String golden =
                "--- {0}/src/test/Test.java\n" +
                "+++ {0}/src/test/Test.java\n" +
                "@@ -1 +1 @@\n" +
                "-package test; public class Test {private void test() {Character.toLowerCase('a');}}\n" +
                "+package test; public class Test {private void test() {Character.toUpperCase('a');}}\n";

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test() {Character.toLowerCase('a');}}\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
    }

    public void testRunCompilerMulti() throws Exception {
        String golden =
                "--- {0}/src/test/Test.java\n" +
                "+++ {0}/src/test/Test.java\n" +
                "@@ -1 +1 @@\n" +
                "-package test; public class Test {private void test() {Character.toLowerCase('a'); Dep.test();}}\n" +
                "+package test; public class Test {private void test() {Character.toUpperCase('a'); Dep.test();}}\n" +
                "--- {0}/src/test/Dep.java\n" +
                "+++ {0}/src/test/Dep.java\n" +
                "@@ -1 +1 @@\n" +
                "-package test; public class Dep {static void test() {Character.toLowerCase('a');}}\n" +
                "+package test; public class Dep {static void test() {Character.toUpperCase('a');}}\n";

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test() {Character.toLowerCase('a'); Dep.test();}}\n",
                              "src/test/Dep.java",
                              "package test; public class Dep {static void test() {Character.toLowerCase('a');}}\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
    }

//    public void testNPEFromAttribute() throws Exception {//TODO: does not reproduce the problem - likely caused by null Env<AttrContext> for annonymous innerclasses
//        String golden = null;
//
//        doRunCompiler(golden, "src/test/Test.java",
//                              "package test; public class Test {private void test() {new Runnable() {public void run() {int i = 0; System.err.println(i);}};}}\n",
//                              "src/META-INF/upgrade/joFile.hint",
//                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
//    }

    public void testTreesCleaning1() throws Exception {
        String golden = null;

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test() {java.util.Collections.<String>emptyList();}}\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
    }

    public void testTreesCleaning2() throws Exception {
        String golden = null;

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {enum A { B; A() {}} }\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
    }

    public void testTreesCleaningEnumTooMuch() throws Exception {
        String golden = null;

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {enum A { B; private final int i; A() {this(1);} A(int i) {this.i = i;}} }\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
    }

    public void testTreesCleaningEnum3() throws Exception {
        String golden = null;

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {enum A { B(\"a\"){public String toString() {return null;} }; A(String str) {}} }\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
    }

    public void testCRTable() throws Exception {
        String golden = null;

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test() {Integer i = 0; i++;}}\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;");
    }

    public void testCodeAPI() throws Exception {
        String golden = "--- {0}/src/test/Test.java\n"+
                        "+++ {0}/src/test/Test.java\n"+
                        "@@ -1,2 +1,2 @@\n"+
                        "-package test; public class Test {private void test() {Integer i = 0; if (i == null && null == i) System.err.println(i);\n"+
                        "+package test; public class Test {private void test() {Integer i = 0; if (i == null) System.err.println(i);\n"+
                        " }}\n";
        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test() {Integer i = 0; if (i == null && null == i) System.err.println(i);\n}}\n",
                              null,
                              "-A" + HintsAnnotationProcessing.HARDCODED_HINTS_ENABLE + "=defaults");
    }

    public void testExtraHints() throws Exception {
        String golden =
                "--- {0}/src/test/Test.java\n" +
                "+++ {0}/src/test/Test.java\n" +
                "@@ -1 +1 @@\n" +
                "-package test; public class Test {private void test() {Character.toLowerCase('a');}}\n" +
                "+package test; public class Test {private void test() {Character.toUpperCase('a');}}\n";

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test() {Character.toLowerCase('a');}}\n",
                              "extra.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;",
                              null,
                              "-A" + HintsAnnotationProcessing.EXTRA_HINTS + "=extra.hint");

    }
    public void testHintsOnClassPath() throws Exception {
        String golden =
                "--- {0}/src/test/Test.java\n" +
                "+++ {0}/src/test/Test.java\n" +
                "@@ -1 +1 @@\n" +
                "-package test; public class Test {private void test() {Character.toLowerCase('a');}}\n" +
                "+package test; public class Test {private void test() {Character.toUpperCase('a');}}\n";

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test() {Character.toLowerCase('a');}}\n",
                              "comp/META-INF/upgrade/joFile.hint",
                              "'test':\njava.lang.Character.toLowerCase($1) :: $1 instanceof char => java.lang.Character.toUpperCase($1) ;;",
                              null,
                              "-classpath",
                              "comp");
    }

    public void testNoDebugInfo() throws Exception {
        String golden =
                "--- {0}/src/test/Test.java\n" +
                "+++ {0}/src/test/Test.java\n" +
                "@@ -1 +1 @@\n" +
                "-package test; public class Test {private void test(java.io.File f) {f.isDirectory();}}\n" +
                "+package test; public class Test {private void test(java.io.File f) {!f.isFile();}}\n";

        doRunCompiler(golden, "src/test/Test.java",
                              "package test; public class Test {private void test(java.io.File f) {f.isDirectory();}}\n",
                              "src/META-INF/upgrade/joFile.hint",
                              "'test':\n$1.isDirectory() :: $1 instanceof java.io.File => !$1.isFile();;",
                              null,
                              "-g:none");
    }

    private void doRunCompiler(String goldenDiff, String... fileContentAndExtraOptions) throws Exception {
        List<String> fileAndContent = new LinkedList<String>();
        List<String> extraOptions = new LinkedList<String>();
        List<String> fileContentAndExtraOptionsList = Arrays.asList(fileContentAndExtraOptions);
        int nullPos = fileContentAndExtraOptionsList.indexOf(null);

        if (nullPos == (-1)) {
            fileAndContent = fileContentAndExtraOptionsList;
            extraOptions = Collections.emptyList();
        } else {
            fileAndContent = fileContentAndExtraOptionsList.subList(0, nullPos);
            extraOptions = fileContentAndExtraOptionsList.subList(nullPos + 1, fileContentAndExtraOptionsList.size());
        }

        assertTrue(fileAndContent.size() % 2 == 0);

        clearWorkDir();

        for (int cntr = 0; cntr < fileAndContent.size(); cntr += 2) {
            createAndFill(fileAndContent.get(cntr), fileAndContent.get(cntr + 1));
        }

        File wd = getWorkDir();
        File source = new File(wd, "src/test/Test.java");

        File sourceOutput = new File(wd, "src-out");

        sourceOutput.mkdirs();

        List<String> options = new LinkedList<String>();
        
        options.add(source.getAbsolutePath());
        options.add("-sourcepath");
        options.add(source.getParentFile().getParentFile().getAbsolutePath());
        options.add("-s");
        options.add(sourceOutput.getAbsolutePath());
        options.add("-source");
        options.add("1.5");
        options.add("-Xjcov");
        options.addAll(extraOptions);

        reallyRunCompiler(wd, options.toArray(new String[0]));
        
        File diff = new File(sourceOutput, "META-INF/upgrade/upgrade.diff");
        String diffText = readFully(diff);

        goldenDiff = goldenDiff != null ? goldenDiff.replace("{0}", wd.getAbsolutePath()) : null;
        assertEquals(goldenDiff, diffText);
    }

    protected void reallyRunCompiler(File workDir, String... params) throws Exception {
        String oldUserDir = System.getProperty("user.dir");

        System.setProperty("user.dir", workDir.getAbsolutePath());
        
        try {
            assertEquals(0, Main.compile(params));
        } finally {
            System.setProperty("user.dir", oldUserDir);
        }
    }

    private void createAndFill(String path, String content) throws IOException {
        File wd = getWorkDir();
        File source = new File(wd, path);

        source.getParentFile().mkdirs();
        
        Writer out = new OutputStreamWriter(new FileOutputStream(source));

        out.write(content);

        out.close();
    }

    private String readFully(File file) throws IOException {
        if (!file.canRead()) return null;
        StringBuilder res = new StringBuilder();
        Reader in = new InputStreamReader(new FileInputStream(file));
        int read;
        
        while ((read = in.read()) != (-1)) {
            res.append((char) read);
        }

        return res.toString();
    }

    @Hint(category="general")
    @TriggerPattern("$1 == null && null == $1")
    @TriggerCompileTime //XXX: currently not really used
    public static ErrorDescription codeHint(HintContext ctx) {
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "test", JavaFix.rewriteFix(ctx.getInfo(), "test", ctx.getPath(), "$1 == null", ctx.getVariables(), ctx.getMultiVariables(), ctx.getVariableNames(), Collections.<String, TypeMirror>emptyMap()));
    }

}
