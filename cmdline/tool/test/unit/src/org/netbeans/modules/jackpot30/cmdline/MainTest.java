/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.cmdline;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.junit.NbTestCase;

/**
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

        doRunCompiler(golden, "src/test/Test.java",
                              "package test;\n" +
                              "public class Test {\n" +
                              "    private void test(java.util.Collection c) {\n" +
                              "        boolean b = c.size() == 0;\n" +
                              "    }\n" +
                              "}\n",
                              null,
                              "--hint",
                              "Usage of .size() == 0");
    }

    private void doRunCompiler(String golden, String... fileContentAndExtraOptions) throws Exception {
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
        options.addAll(extraOptions);
        options.add(wd.getAbsolutePath());

        reallyRunCompiler(wd, options.toArray(new String[0]));

        assertEquals(golden, TestUtilities.copyFileToString(source));
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

}