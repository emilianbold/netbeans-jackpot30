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
                      "${workdir}/src/test/Test.java:4: warning: [Usage_of_size_equals_0] Usage of .size() == 0 can be replaced with .isEmpty()\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "                     ^\n" +
                      "${workdir}/src/test/Test.java:5: warning: [Usage_of_size_equals_0] Usage of .size() == 0 can be replaced with .isEmpty()\n" +
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
                      "${workdir}/src/test/Test.java:4: warning: [Usage_of_size_equals_0] Usage of .size() == 0 can be replaced with .isEmpty()\n" +
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
    
    public void testHintFile() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b = c.size() == 0;\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      "",
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b = c.isEmpty();\n" +
                      "    }\n" +
                      "}\n",
                      "test-rule.hint",
                      "$var.isEmpty() => $var.size() == 0;;",
                      null,
                      "--hint-file",
                      "${workdir}/test-rule.hint",
                      "--source",
                      "1.6",
                      "--apply");
    }

    public void testConfigurationFileDeclarative1() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b1 = c.isEmpty();\n" +
            "        boolean b2 = c.size() <= 0;\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      null,
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "        boolean b2 = c.size() <= 0;\n" +
                      "    }\n" +
                      "}\n",
                      "META-INF/upgrade/test1.hint",
                      "$c.size() == 0 :: $c instanceof java.util.Collection => $c.isEmpty();;\n",
                      "META-INF/upgrade/test2.hint",
                      "$c.size() <= 0 :: $c instanceof java.util.Collection => $c.isEmpty();;\n",
                      "settings.xml",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<hints apply=\"true\" runDeclarative=\"false\">\n" +
                      "    <settings>\n" +
                      "        <test1.hint enabled=\"true\"/>\n" +
                      "    </settings>\n" +
                      "</hints>\n",
                      null,
                      "--config-file",
                      "${workdir}/settings.xml",
                      "--source",
                      "1.6");
    }

    public void testConfigurationFileDeclarative2() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b1 = c.isEmpty();\n" +
            "        boolean b2 = c.isEmpty();\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      null,
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "        boolean b2 = c.size() <= 0;\n" +
                      "    }\n" +
                      "}\n",
                      "META-INF/upgrade/test1.hint",
                      "$c.size() == 0 :: $c instanceof java.util.Collection => $c.isEmpty();;\n",
                      "META-INF/upgrade/test2.hint",
                      "$c.size() <= 0 :: $c instanceof java.util.Collection => $c.isEmpty();;\n",
                      "settings.xml",
                      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                      "<hints apply=\"true\" runDeclarative=\"true\">\n" +
                      "    <settings>\n" +
                      "        <test1.hint enabled=\"true\"/>\n" +
                      "    </settings>\n" +
                      "</hints>\n",
                      null,
                      "--config-file",
                      "${workdir}/settings.xml",
                      "--source",
                      "1.6");
    }

    public void testSourcePath() throws Exception {
        String golden =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test() {\n" +
            "        String s = test2.Test2.C;\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(golden,
                      null,
                      null,
                      "src/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test() {\n" +
                      "        String s = test2.Test2.C.intern();\n" +
                      "    }\n" +
                      "}\n",
                      "src/test2/Test2.java",
                      "package test2;\n" +
                      "public class Test2 {\n" +
                      "    public static final String C = \"a\";\n" +
                      "}\n",
                      null,
                      DONT_APPEND_PATH,
                      "--apply",
                      "--hint",
                      "String.intern() called on constant",
                      "--sourcepath",
                      "${workdir}/src",
                      "${workdir}/src/test");
    }

    public void testWarningsAreErrors() throws Exception {
        String code =
            "package test;\n" +
            "public class Test {\n" +
            "    private void test(java.util.Collection c) {\n" +
            "        boolean b1 = c.size() == 0;\n" +
            "\tboolean b2 = c.size() == 0;\n" +
            "    }\n" +
            "}\n";

        doRunCompiler(equivalentValidator(code),
                      equivalentValidator(
                          "${workdir}/src/test/Test.java:4: warning: [Usage_of_size_equals_0] Usage of .size() == 0 can be replaced with .isEmpty()\n" +
                          "        boolean b1 = c.size() == 0;\n" +
                          "                     ^\n" +
                          "${workdir}/src/test/Test.java:5: warning: [Usage_of_size_equals_0] Usage of .size() == 0 can be replaced with .isEmpty()\n" +
                          "\tboolean b2 = c.size() == 0;\n" +
                          "\t             ^\n"
                      ),
                      equivalentValidator(null),
                      1,
                      "src/test/Test.java",
                      code,
                      null,
                      "--hint",
                      "Usage of .size() == 0",
                      "--no-apply",
                      "--fail-on-warnings");
    }

    public void testGroups() throws Exception {
        doRunCompiler(null,
                      "${workdir}/src1/test/Test.java:4: warning: [test] test\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "                     ^\n" +
                      "${workdir}/src2/test/Test.java:5: warning: [test] test\n" +
                      "        boolean b2 = c.size() != 0;\n" +
                      "                     ^\n",
                      null,
                      "cp1/META-INF/upgrade/test.hint",
                      "$coll.size() == 0 :: $coll instanceof java.util.Collection;;",
                      "src1/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "        boolean b2 = c.size() != 0;\n" +
                      "    }\n" +
                      "}\n",
                      "cp2/META-INF/upgrade/test.hint",
                      "$coll.size() != 0 :: $coll instanceof java.util.Collection;;",
                      "src2/test/Test.java",
                      "package test;\n" +
                      "public class Test {\n" +
                      "    private void test(java.util.Collection c) {\n" +
                      "        boolean b1 = c.size() == 0;\n" +
                      "        boolean b2 = c.size() != 0;\n" +
                      "    }\n" +
                      "}\n",
                      null,
                      DONT_APPEND_PATH,
                      "--group",
                      "--classpath ${workdir}/cp1 ${workdir}/src1",
                      "--group",
                      "--classpath ${workdir}/cp2 ${workdir}/src2");
    }

    public void testGroupsList() throws Exception {
        doRunCompiler(null,
                      new Validator() {
                          @Override public void validate(String content) {
                              assertTrue("Missing expected content, actual content: " + content, content.contains("test\n"));
                          }
                      },
                      null,
                      "cp1/META-INF/upgrade/test.hint",
                      "$coll.size() == 0 :: $coll instanceof java.util.Collection;;",
                      "src1/test/Test.java",
                      "\n",
                      "cp2/META-INF/upgrade/test.hint",
                      "$coll.size() != 0 :: $coll instanceof java.util.Collection;;",
                      "src2/test/Test.java",
                      "\n",
                      null,
                      DONT_APPEND_PATH,
                      "--group",
                      "--classpath ${workdir}/cp1 ${workdir}/src1",
                      "--group",
                      "--classpath ${workdir}/cp2 ${workdir}/src2",
                      "--list");
    }

    public void testGroupsParamEscape() throws Exception {
        assertEquals(Arrays.asList("a b", "a\\b"),
                     Arrays.asList(Main.splitGroupArg("a\\ b a\\\\b")));
    }

    private static final String DONT_APPEND_PATH = new String("DONT_APPEND_PATH");

    private void doRunCompiler(String golden, String stdOut, String stdErr, String... fileContentAndExtraOptions) throws Exception {
        doRunCompiler(equivalentValidator(golden), equivalentValidator(stdOut), equivalentValidator(stdErr), fileContentAndExtraOptions);
    }

    private void doRunCompiler(Validator fileContentValidator, Validator stdOutValidator, Validator stdErrValidator, String... fileContentAndExtraOptions) throws Exception {
        doRunCompiler(fileContentValidator, stdOutValidator, stdErrValidator, 0, fileContentAndExtraOptions);
    }

    private void doRunCompiler(Validator fileContentValidator, Validator stdOutValidator, Validator stdErrValidator, int exitcode, String... fileContentAndExtraOptions) throws Exception {
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
        boolean appendPath = true;

        options.add("--cache");
        options.add("/tmp/cachex");
        for (String extraOption : extraOptions) {
            if (extraOption == DONT_APPEND_PATH) {
                appendPath = false;
                continue;
            }
            options.add(extraOption.replace("${workdir}", wd.getAbsolutePath()));
        }

        if (appendPath)
            options.add(wd.getAbsolutePath());

        String[] output = new String[2];

        reallyRunCompiler(wd, exitcode, output, options.toArray(new String[0]));

        if (fileContentValidator != null) {
            fileContentValidator.validate(TestUtilities.copyFileToString(source));
        }
        if (stdOutValidator != null) {
            stdOutValidator.validate(output[0].replaceAll(Pattern.quote(wd.getAbsolutePath()), Matcher.quoteReplacement("${workdir}")));
        }
        if (stdErrValidator != null) {
            stdErrValidator.validate(output[1].replaceAll(Pattern.quote(wd.getAbsolutePath()), Matcher.quoteReplacement("${workdir}")));
        }
    }

    protected void reallyRunCompiler(File workDir, int exitcode, String[] output, String... params) throws Exception {
        String oldUserDir = System.getProperty("user.dir");

        System.setProperty("user.dir", workDir.getAbsolutePath());
        
        PrintStream oldOut = System.out;
        ByteArrayOutputStream outData = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outData, true, "UTF-8"));

        PrintStream oldErr = System.err;
        ByteArrayOutputStream errData = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errData, true, "UTF-8"));

        try {
            assertEquals(exitcode, Main.compile(params));
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

    private static Validator equivalentValidator(final String expected) {
        if (expected == null) return null;

        return new Validator() {
            @Override public void validate(String content) {
                assertEquals(expected, content);
            }
        };
    }

    private static interface Validator {
        public void validate(String content);
    }
}