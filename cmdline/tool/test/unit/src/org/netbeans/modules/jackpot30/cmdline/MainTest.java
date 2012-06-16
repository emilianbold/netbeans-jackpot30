/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010-2011 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010-2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.cmdline;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.runner.Result;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileUtil;

/**XXX: should also test error conditions
 *
 * @author lahvac
 */
public class MainTest extends NbTestCase {

    public MainTest(String name) {
        super(name);
    }

    public void testRunCompiler1() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b = c.isEmpty();\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      null,
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b = c.size() == 0;\n" +
                      "    }\n" +
                      "}\n",
                      null,
                      "--apply",
                      "--hint",
                      "Usage of .size() == 0");
    }

    public void testDoNotApply() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b1 = c.size() == 0;\n" +
            "\tboolean b2 = c.size() == 0;\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      "${workdir}/src/test/Test.java:4: warning: Usage of .size() == 0 can be replaced with .isEmpty()\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "                     ^\n" +
                      "${workdir}/src/test/Test.java:5: warning: Usage of .size() == 0 can be replaced with .isEmpty()\n" +
                      "\tboolean b2 = c.size() == 0;\n" +
                      "\t             ^\n",
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "\tboolean b2 = c.size() == 0;\n" +
                      "    }\n" +
                      "}\n",
                      null,
                      "--hint",
                      "Usage of .size() == 0",
                      "--no-apply");
    }

    public void testConfig() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private int test(String str) {\n" +
            "        if (\"a\" == str) {\n" +
            "            return 1;\n" +
            "        } else if (\"b\" == str) {\n" +
            "            return 2;\n" +
            "        } else {\n" +
            "            return 3;\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      null,
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private int test(String str) {\n" +
                      "        if (\"a\" == str) {\n" +
                      "            return 1;\n" +
                      "        } else if (\"b\" == str) {\n" +
                      "            return 2;\n" +
                      "        } else {\n" +
                      "            return 3;\n" +
                      "        }\n" +
                      "    }\n" +
                      "}\n",
                      null,
                      "--hint",
                      "Use switch over Strings where possible.",
                      "--config",
                      "also-equals=false");
    }

    public void testValidSourceLevel() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b = c.isEmpty();\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      null,
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b = c.size() == 0;\n" +
                      "    }\n" +
                      "}\n",
                      null,
                      "--apply",
                      "--hint",
                      "Usage of .size() == 0",
                      "--source",
                      "1.6");
    }

    public void testConfigurationFile() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b = c.isEmpty();\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      null,
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b = c.size() == 0;\n" +
                      "    }\n" +
                      "}\n",
                      "settings.xml",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<hints apply=\"true\">\n" +
                      "    <settings>\n" +
                      "        <org.netbeans.modules.java.hints.perf.SizeEqualsZero check.not.equals=\"false\" enabled=\"true\" hintSeverity=\"VERIFIER\"/>\n" +
                      "    </settings>\n" +
                      "</hints>\n",
                      null,
                      "--config-file",
                      "${workdir}/settings.xml",
                      "--source",
                      "1.6");
    }

    public void testConfigurationFileCmdLineOverride() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b = c.size() == 0;\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      "${workdir}/src/test/Test.java:4: warning: Usage of .size() == 0 can be replaced with .isEmpty()\n" +
                      "        boolean b = c.size() == 0;\n" +
                      "                    ^\n",
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b = c.size() == 0;\n" +
                      "    }\n" +
                      "}\n",
                      "settings.xml",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<hints apply=\"true\">\n" +
                      "    <settings>\n" +
                      "        <org.netbeans.modules.java.hints.perf.SizeEqualsZero check.not.equals=\"false\" enabled=\"true\" hintSeverity=\"VERIFIER\"/>\n" +
                      "    </settings>\n" +
                      "</hints>\n",
                      null,
                      "--config-file",
                      "${workdir}/settings.xml",
                      "--source",
                      "1.6",
                      "--no-apply");
    }

    private void doRunCompiler(String golden, String stdOut, String stdErr, String... fileContentAndExtraOptions) throws Exception {
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
            File target = new File(getWorkDir(), fileAndContent.get(cntr));

            target.getParentFile().mkdirs();
            
            TestUtilities.copyStringToFile(target, fileAndContent.get(cntr + 1));
        }

        File wd = getWorkDir();
        File source = new File(wd, "src/test/Test.java");

        List<String> options = new LinkedList<String>();

        options.add("--cache");
        options.add("/tmp/cachex");
        for (String extraOption : extraOptions) {
            options.add(extraOption.replace("${workdir}", wd.getAbsolutePath()));
        }
        options.add(wd.getAbsolutePath());

        String[] output = new String[2];

        reallyRunCompiler(wd, output, options.toArray(new String[0]));

        assertEquals(golden, TestUtilities.copyFileToString(source));

        if (stdOut != null) {
            assertEquals(stdOut, output[0].replaceAll(Pattern.quote(wd.getAbsolutePath()), Matcher.quoteReplacement("${workdir}")));
        }

        if (stdErr != null) {
            assertEquals(stdErr, output[1].replaceAll(Pattern.quote(wd.getAbsolutePath()), Matcher.quoteReplacement("${workdir}")));
        }
    }

    protected void reallyRunCompiler(File workDir, String[] output, String... params) throws Exception {
        String oldUserDir = System.getProperty("user.dir");

        System.setProperty("user.dir", workDir.getAbsolutePath());
        
        PrintStream oldOut = System.out;
        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outData, true, "UTF-8"));

        PrintStream oldErr = System.err;
        ByteArrayOutputStream errData = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errData, true, "UTF-8"));

        try {
            assertEquals(0, Main.compile(params));
        } finally {
            System.setProperty("user.dir", oldUserDir);
            System.out.close();
            output[0] = new String(outData.toByteArray(), "UTF-8");
            System.setOut(oldOut);
            System.err.close();
            output[1] = new String(errData.toByteArray(), "UTF-8");
            System.setErr(oldErr);
        }
    }

    //verify that the DeclarativeHintsTestBase works:
    public void testRunTest() throws Exception {
        clearWorkDir();

        File wd = getWorkDir();
        File classes = new File(wd, "classes");

        classes.mkdirs();
        TestUtilities.copyStringToFile(new File(classes, "h.hint"), "$1.equals(\"\") :: $1 instanceof java.lang.String => $1.isEmpty();;");

        String test = "%%TestCase pos\n" +
                      "package test;\n" +
                      "public class Test {{\n" +
                      " System.err.println(\"a\".equals(\"\"));\n" +
                      "}}\n" +
                      "%%=>\n" +
                      "package test;\n" +
                      "public class Test {{\n" +
                      " System.err.println(\"a\".isEmpty());\n" +
                      "}}\n" +
                      "%%TestCase neg\n" +
                      "package test;\n" +
                      "public class Test {{\n" +
                      " System.err.println(\"a\".equals(\"a\"));\n" +
                      "}}\n" +
                      "%%=>\n" +
                      "package test;\n" +
                      "public class Test {{\n" +
                      " System.err.println(\"a\".isEmpty());\n" +
                      "}}\n";
        TestUtilities.copyStringToFile(new File(classes, "h.test"), test);

        File runner = new File(classes, "org/netbeans/modules/jackpot30/cmdline/testtool/DoRunTests.class");

        assertTrue(runner.getParentFile().mkdirs());

        FileOutputStream os = new FileOutputStream(runner);
        InputStream is = MainTest.class.getResourceAsStream("DoRunTests.classx");

        assertNotNull(is);

        FileUtil.copy(is, os);
        os.close ();
        is.close();

        runAndTest(classes);
    }

    protected void runAndTest(File classes) throws Exception {
        ClassLoader cl = new URLClassLoader(new URL[] {classes.toURI().toURL()}, MainTest.class.getClassLoader());
        Class<?> doRunTests = Class.forName("org.netbeans.modules.jackpot30.cmdline.testtool.DoRunTests", true, cl);
        Result testResult = org.junit.runner.JUnitCore.runClasses(doRunTests);

        assertEquals(1, testResult.getFailureCount());
        assertTrue(testResult.getFailures().toString(), testResult.getFailures().get(0).getDescription().getMethodName().endsWith("/h.test/neg"));
    }
}