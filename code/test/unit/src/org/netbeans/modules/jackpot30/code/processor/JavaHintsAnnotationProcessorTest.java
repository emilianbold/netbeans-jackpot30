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

package org.netbeans.modules.jackpot30.code.processor;

import java.util.Arrays;
import java.io.ByteArrayOutputStream;
import org.openide.util.test.AnnotationProcessorTestUtils;
import java.io.File;
import java.util.HashSet;
import org.openide.filesystems.FileObject;
import java.net.URL;
import org.netbeans.modules.jackpot30.code.spi.Hint;
import java.util.Set;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.jackpot30.code.spi.TriggerPattern;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.filesystems.URLMapper;
import static org.junit.Assert.*;

/**
 *
 * @author lahvac
 */
public class JavaHintsAnnotationProcessorTest extends NbTestCase {

    public JavaHintsAnnotationProcessorTest(String name) {
        super(name);
    }

    public void testBasic() throws Exception {
        clearWorkDir();
        File src = new File(getWorkDir(), "src");
        File dest = new File(getWorkDir(), "classes");

        AnnotationProcessorTestUtils.makeSource(src,
                                                "test.Test",
                                                "import org.netbeans.modules.jackpot30.code.spi.Hint;",
                                                "import org.netbeans.modules.jackpot30.code.spi.TriggerPattern;",
                                                "import org.netbeans.modules.jackpot30.spi.HintContext;",
                                                "import org.netbeans.spi.editor.hints.ErrorDescription;",
                                                "public class Test {",
                                                "    @Hint(\"test\")",
                                                "    @TriggerPattern(\"a\")",
                                                "    public ErrorDescription test(HintContext ctx) {",
                                                "        return null;",
                                                "    }",
                                                "}");

        assertTrue(AnnotationProcessorTestUtils.runJavac(src, "Test", dest, null, new ByteArrayOutputStream()));

        String hints = TestUtilities.copyFileToString(new File(dest, "META-INF/nb-hints/hints"));
        Set<String> hintClasses = new HashSet<String>(Arrays.asList(hints.split("\n")));

        assertEquals(new HashSet<String>(Arrays.asList("test.Test")), hintClasses);
    }

    public void testIncremental() throws Exception {
        clearWorkDir();
        File src = new File(getWorkDir(), "src");
        File dest = new File(getWorkDir(), "classes");

        AnnotationProcessorTestUtils.makeSource(src,
                                                "test.Test",
                                                "import org.netbeans.modules.jackpot30.code.spi.Hint;",
                                                "import org.netbeans.modules.jackpot30.code.spi.TriggerPattern;",
                                                "import org.netbeans.modules.jackpot30.spi.HintContext;",
                                                "import org.netbeans.spi.editor.hints.ErrorDescription;",
                                                "public class Test {",
                                                "    @Hint(\"test\")",
                                                "    @TriggerPattern(\"a\")",
                                                "    public ErrorDescription test(HintContext ctx) {",
                                                "        return null;",
                                                "    }",
                                                "}");

        assertTrue(AnnotationProcessorTestUtils.runJavac(src, "Test", dest, null, new ByteArrayOutputStream()));

        AnnotationProcessorTestUtils.makeSource(src,
                                                "test.Other",
                                                "import org.netbeans.modules.jackpot30.code.spi.Hint;",
                                                "import org.netbeans.modules.jackpot30.code.spi.TriggerPattern;",
                                                "import org.netbeans.modules.jackpot30.spi.HintContext;",
                                                "import org.netbeans.spi.editor.hints.ErrorDescription;",
                                                "public class Other {",
                                                "    @Hint(\"test2\")",
                                                "    @TriggerPattern(\"a\")",
                                                "    public ErrorDescription test(HintContext ctx) {",
                                                "        return null;",
                                                "    }",
                                                "}");

        assertTrue(AnnotationProcessorTestUtils.runJavac(src, "Other", dest, null, new ByteArrayOutputStream()));

        String hints = TestUtilities.copyFileToString(new File(dest, "META-INF/nb-hints/hints"));
        Set<String> hintClasses = new HashSet<String>(Arrays.asList(hints.split("\n")));

        assertEquals(new HashSet<String>(Arrays.asList("test.Test", "test.Other")), hintClasses);
    }
    
}