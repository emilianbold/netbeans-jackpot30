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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.text.Document;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.lexer.Language;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
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
public class BulkSearchTest extends NbTestCase {

    public BulkSearchTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[0]);
        TreeLoader.DISABLE_CONFINEMENT_TEST = true;
    }

    public void testSimple1() throws Exception {
        performTest("package test; public class Test { private void test() { System.err.println(\"\");}}",
                    Arrays.asList("System.err.println(\"\")"),
                    Arrays.asList("System.err.println(\"\" + \"\")"));
    }

    public void testDontCare() throws Exception {
        performTest("package test; public class Test { private void test() { System.err.println(\"\" + \"\");}}",
                    Arrays.asList("System.err.println($1)"),
                    Collections.<String>emptyList());
    }

    public void testMemberSelectAndIdentifier() throws Exception {
        performTest("package test; public class Test { private static void test() { test();}}",
                    Arrays.asList("test.Test.test()"),
                    Collections.<String>emptyList());
    }

    public void testUnpureMemberSelect() throws Exception {
        performTest("package test; public class Test { private static void test() { new StringBuilder().append('');}}",
                    Collections.<String>emptyList(),
                    Arrays.asList("test.append('')"));
    }

    public void testMemberSelectWithVariables1() throws Exception {
        performTest("package test; public class Test { private static void test() { new StringBuilder().append('');}}",
                    Arrays.asList("$0.append('')"),
                    Collections.<String>emptyList());
    }

    public void testMemberSelectWithVariables2() throws Exception {
        performTest("package test; public class Test { private void append(char c) { append('');}}",
                    Arrays.asList("$0.append('')"),
                    Collections.<String>emptyList());
    }
    
    public void XtestMeasureTime() throws Exception {
        String code = TestUtilities.copyFileToString(new File("/usr/local/home/lahvac/src/nb//outgoing/java.editor/src/org/netbeans/modules/editor/java/JavaCompletionProvider.java"));
        List<String> patterns = new LinkedList<String>();

        for (int cntr = 0; cntr < 1000; cntr++) {
            patterns.add("System.err.println($1)");
        }
        
        performTest(code,
                    Collections.<String>emptyList(),
                    patterns);
    }

    private void performTest(String text, Collection<String> containedPatterns, Collection<String> notContainedPatterns) throws Exception {
        prepareTest("test/Test.java", text);

        List<String> patterns = new LinkedList<String>();

        patterns.addAll(containedPatterns);
        patterns.addAll(notContainedPatterns);

        long s1 = System.currentTimeMillis();
        BulkPattern p = BulkSearch.create(info, patterns);
        long e1 = System.currentTimeMillis();

//        System.err.println("create: " + (e1 - s1));

        long s2 = System.currentTimeMillis();
        Set<String> result = BulkSearch.match(info, info.getCompilationUnit(), p);
        long e2 = System.currentTimeMillis();

//        System.err.println("match: " + (e2 - s2));

        assertTrue(result.containsAll(containedPatterns));

        Set<String> none = new HashSet<String>(result);

        none.retainAll(notContainedPatterns);

        assertTrue(none.isEmpty());
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

}
