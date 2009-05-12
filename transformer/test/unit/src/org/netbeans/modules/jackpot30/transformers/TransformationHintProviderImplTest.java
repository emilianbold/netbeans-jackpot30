/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.transformers;

import com.sun.source.tree.Tree.Kind;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.java.source.TreeLoader;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.LifecycleManager;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.modules.InstalledFileLocator;

/**
 *
 * @author Jan Lahoda
 */
public class TransformationHintProviderImplTest extends NbTestCase {

    public TransformationHintProviderImplTest(String name) {
        super(name);
    }

    public void testSimpleAnalysis() throws Exception {
        performAnalysisTest("package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"$0.getCookie($1)\"),\n" +
                            "        constraint={\n" +
                            "            @Constraint(variable=\"$0\", type=Test.class),\n" +
                            "            @Constraint(variable=\"$1\", type=Class.class)\n" +
                            "        },\n" +
                            "        fix=@Fix(\"$0.lookup($1)\")\n" +
                            "    )\n" +
                            "    public void getCookie(Class c) {}\n" +
                            "    private void test() {\n" +
                            "         getCookie(null);\n" +
                            "    }\n" +
                            "}\n",
                            "17:9-17:18:verifier:O");
    }

    public void testCanGoUp() throws Exception {
        performAnalysisTest("package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public abstract class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"$0.getCookie().toString()\"),\n" +
                            "        constraint={\n" +
                            "            @Constraint(variable=\"$0\", type=Test.class)\n" +
                            "        },\n" +
                            "        fix=@Fix(\"$0.lookup($1)\")\n" +
                            "    )\n" +
                            "    public abstract String getCookie();\n" +
                            "    private void test() {\n" +
                            "         getCookie().toString();\n" +
                            "    }\n" +
                            "}\n",
                            "16:21-16:29:verifier:O");
    }

    public void testForConstructor() throws Exception {
        performAnalysisTest("package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"new test.Test()\"),\n" +
                            "        fix={}\n" +
                            "    )\n" +
                            "    public Test() {}\n" +
                            "    private void test() {\n" +
                            "         new Test();\n" +
                            "    }\n" +
                            "}\n",
                            "13:9-13:19:verifier:O");
    }

    public void testMoreWarnings() throws Exception {
        performAnalysisTest("package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"new test.Test()\"),\n" +
                            "        fix={}\n" +
                            "    )\n" +
                            "    public Test() {}\n" +
                            "    private void test() {\n" +
                            "         new Test();\n" +
                            "         new Test();\n" +
                            "    }\n" +
                            "}\n",
                            "13:9-13:19:verifier:O",
                            "14:9-14:19:verifier:O");
    }

    public void testCorrectImports() throws Exception {
        performFixTest("test/Test.java",
                            "package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public abstract class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"$this.test()\"),\n" +
                            "        fix=@Fix(\"test.Test.test1().toString()\")\n" +
                            "    )\n" +
                            "    private void test() {\n" +
                            "         test();\n" +
                            "    }" +
                            "    static String test1() {return null;}\n" +
                            "}\n",
                            "12:9-12:13:verifier:O",
                            "FixImpl",
                            "package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public abstract class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"$this.test()\"),\n" +
                            "        fix=@Fix(\"test.Test.test1().toString()\")\n" +
                            "    )\n" +
                            "    private void test() {\n" +
                            "         Test.test1().toString();\n" +
                            "    }" +
                            "    static String test1() {return null;}\n" +
                            "}\n");
    }

    public void testNoExceptions1() throws Exception {
        performNoExceptionsTest("package test;\n" +
                                "import net.java.lang.annotations.Constraint;\n" +
                                "import net.java.lang.annotations.Fix;\n" +
                                "import net.java.lang.annotations.Pattern;\n" +
                                "import net.java.lang.annotations.Transformation;\n" +
                                "public abstract class Test {\n" +
                                "    |public abstract String getCookie();\n" +
                                "    private void test() {\n" +
                                "         getCookie().toString();\n" +
                                "    }\n" +
                                "}\n",
                                "    @Transformation(\n" +
                                "        displayName=\"O\",\n" +
                                "        pattern=@Pattern(\"$0.getCookie().toString()\"),\n" +
                                "        constraint={\n" +
                                "            @Constraint(variable=\"$0\", type=Test.class)\n" +
                                "        },\n" +
                                "        fix={@Fix(\"$0.lookup($1)\")}\n" +
                                "    )\n".replaceAll("[ \n]+", " "));
    }

    public void testNoExceptions2() throws Exception {
        performNoExceptionsTest("package test;\n" +
                                "import net.java.lang.annotations.Constraint;\n" +
                                "import net.java.lang.annotations.Fix;\n" +
                                "import net.java.lang.annotations.Pattern;\n" +
                                "import net.java.lang.annotations.Transformation;\n" +
                                "public abstract class Test {\n" +
                                "    |public abstract String getCookie();\n" +
                                "    private void test() {\n" +
                                "         getCookie().toString();\n" +
                                "    }\n" +
                                "}\n",
                                "    @Transformation(\n" +
                                "        displayName=\"O\",\n" +
                                "        pattern=@Pattern(\"$0.getCookie().toString()\"),\n" +
                                "        constraint=\n" +
                                "            @Constraint(variable=\"$0\", type=Test.class)\n" +
                                "        ,\n" +
                                "        fix=@Fix(\"$0.lookup($1)\")\n" +
                                "    )\n".replaceAll("[ \n]+", " "));
    }

    public void testType1() throws Exception {
        performFixTest("test/Test.java",
                            "package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public abstract class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"test.Test.Bad\"),\n" +
                            "        fix=@Fix(\"test.Test.Good\")\n" +
                            "    )\n" +
                            "    public static class Bad {}\n" +
                            "    public static class Good {}\n" +
                            "    static void test1() { Bad b; }\n" +
                            "}\n",
                            "13:26-13:29:verifier:O",
                            "FixImpl",
                            "package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public abstract class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"test.Test.Bad\"),\n" +
                            "        fix=@Fix(\"test.Test.Good\")\n" +
                            "    )\n" +
                            "    public static class Bad {}\n" +
                            "    public static class Good {}\n" +
                            "    static void test1() { Good b; }\n" +
                            "}\n".replaceAll("[ \n]+", " "));
    }

    public void testCorrectReplace() throws Exception {
        performFixTest("test/Test.java",
                            "package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"$this.test()\"),\n" +
                            "        fix=@Fix(\"$this.test()\")\n" +
                            "    )\n" +
                            "    private void test() {\n" +
                            "         new Test().test();\n" +
                            "    }\n" +
                            "}\n",
                            "12:20-12:24:verifier:O",
                            "FixImpl",
                            "package test;\n" +
                            "import net.java.lang.annotations.Constraint;\n" +
                            "import net.java.lang.annotations.Fix;\n" +
                            "import net.java.lang.annotations.Pattern;\n" +
                            "import net.java.lang.annotations.Transformation;\n" +
                            "public class Test {\n" +
                            "    @Transformation(\n" +
                            "        displayName=\"O\",\n" +
                            "        pattern=@Pattern(\"$this.test()\"),\n" +
                            "        fix=@Fix(\"$this.test()\")\n" +
                            "    )\n" +
                            "    private void test() {\n" +
                            "         new Test().test();\n" +
                            "    }\n" +
                            "}\n");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        File jar = FileUtil.archiveOrDirForURL(TransformationHintProviderImpl.class.getProtectionDomain().getCodeSource().getLocation());

        assertNotNull(jar);

        String nbHome = jar.getParentFile().getParentFile().getAbsolutePath();

        System.setProperty("netbeans.home",nbHome);

        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
        TreeLoader.DISABLE_CONFINEMENT_TEST = true;
    }

    private List<ErrorDescription> computeWarnings() {
        Map<Kind, List<HintDescription>> hints = new HashMap<Kind, List<HintDescription>>();
        Map<PatternDescription, List<HintDescription>> patternHints = new HashMap<PatternDescription, List<HintDescription>>();

        RulesManager.computeElementBasedHintsXXX(info, new AtomicBoolean(), Collections.singletonList(new TransformationHintProviderImpl()), hints, patternHints);

        List<ErrorDescription> warnings = new HintsInvoker().computeHints(info, hints, patternHints);

        return warnings;
    }

    private void prepareTest(String fileName, String code) throws Exception {
        prepareTest(fileName, code, false);
    }

    private void prepareTest(String fileName, String code, boolean ignoreErrors) throws Exception {
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

        File ann = InstalledFileLocator.getDefault().locate("libs/annotations.jar", null, false);

        assertNotNull(ann);

        FileObject annFO = FileUtil.toFileObject(ann);

        assertNotNull(annFO);

        SourceUtilsTestUtil.prepareTest(sourceRoot, buildRoot, cache, new FileObject[] {FileUtil.getArchiveRoot(annFO)});

        DataObject od = DataObject.find(data);
        EditorCookie ec = od.getLookup().lookup(EditorCookie.class);

        assertNotNull(ec);

        doc = ec.openDocument();
        doc.putProperty(Language.class, JavaTokenId.language());

        JavaSource js = JavaSource.forFileObject(data);

        assertNotNull(js);

        info = SourceUtilsTestUtil.getCompilationInfo(js, Phase.RESOLVED);

        assertNotNull(info);

        if (!ignoreErrors) {
            assertEquals(info.getDiagnostics().toString(), 0, info.getDiagnostics().size());
        }
    }

    private FileObject sourceRoot;
    private CompilationInfo info;
    private Document doc;
    private String wordDirPath;
    
    protected void performNoExceptionsTest(String code, String type) throws Exception {
        String wDP = getWorkDirPath();
        
        int offset = code.indexOf('|');

        code = code.replace("|", "");

        File prev = null;

        for (int i = 0; i < type.length(); i++) {
            StringBuilder nueCode = new StringBuilder();

            nueCode.append(code);
            nueCode.insert(offset, type.substring(0, i + 1));

            this.wordDirPath = wDP + File.separatorChar + i;
            
            performAnalysisTest(nueCode.toString(), null, true);

            if (prev != null) {
                deleteFile(prev);
                FileUtil.refreshFor(File.listRoots());
            }

            prev = getWorkDir();
        }
    }

    protected void performAnalysisTest(String code, String... golden) throws Exception {
        performAnalysisTest(code, golden, false);
    }

    protected void performAnalysisTest(String code, String[] golden, boolean ignoreErrors) throws Exception {
        prepareTest("test/Test.java", code, ignoreErrors);

        List<ErrorDescription> warnings = computeWarnings();
        List<String> dns = new LinkedList<String>();

        for (ErrorDescription ed : warnings) {
            dns.add(ed.toString());
        }

        if (golden != null) {
            assertEquals(Arrays.asList(golden), dns);
        }
    }

    protected String performFixTest(String fileName, String code, String errorDescriptionToString, String fixDebugString, String golden) throws Exception {
        prepareTest(fileName, code);

        List<ErrorDescription> errors = computeWarnings();

        ErrorDescription toFix = null;

        for (ErrorDescription d : errors) {
            if (errorDescriptionToString.equals(d.toString())) {
                toFix = d;
                break;
            }
        }

        assertNotNull("Error: \"" + errorDescriptionToString + "\" not found. All ErrorDescriptions: " + errors.toString(), toFix);

        assertTrue("Must be computed", toFix.getFixes().isComputed());

        List<Fix> fixes = toFix.getFixes().getFixes();
        List<String> fixNames = new LinkedList<String>();
        Fix toApply = null;

        for (Fix f : fixes) {
            if (fixDebugString.equals(toDebugString(info, f))) {
                toApply = f;
            }

            fixNames.add(toDebugString(info, f));
        }

        assertNotNull("Cannot find fix to invoke: " + fixNames.toString(), toApply);

        toApply.implement();

        FileObject toCheck = sourceRoot.getFileObject(fileName);

        assertNotNull(toCheck);

        DataObject toCheckDO = DataObject.find(toCheck);
        EditorCookie ec = toCheckDO.getCookie(EditorCookie.class);
        Document toCheckDocument = ec.openDocument();

        String realCode = toCheckDocument.getText(0, toCheckDocument.getLength());

        //ignore whitespaces:
        realCode = realCode.replaceAll("[ \t\n]+", " ");

        if (golden != null) {
            golden = golden.replaceAll("[ \t\n]+", " ");
            assertEquals("The output code does not match the expected code.", golden, realCode);
        }

        LifecycleManager.getDefault().saveAll();

        return realCode;
    }

    private String toDebugString(CompilationInfo info, Fix f) {
        return "FixImpl";
    }

    @Override
    public String getWorkDirPath() {
        if (this.wordDirPath != null) {
            return this.wordDirPath;
        }
        
        return super.getWorkDirPath();
    }

    private static void deleteFile(File file) throws IOException {
        if (file.isDirectory()) {
            // file is a directory - delete sub files first
            File files[] = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                deleteFile(files[i]);
            }

        }
        // file is a File :-)
        boolean result = file.delete();
        if (result == false ) {
            // a problem has appeared
            throw new IOException("Cannot delete file, file = "+file.getPath());
        }
    }
}
