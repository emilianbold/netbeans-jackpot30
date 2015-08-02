/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.hintsimpl;

import org.junit.Test;
import org.netbeans.modules.java.hints.test.api.HintTest;

/**
 *
 * @author lahvac
 */
public class GlobalyUnusedNGTest {

    @Test
    public void testUsed1() throws Exception {
        HintTest.create()
                .input("test/Test1.java",
                       "package test;\n" +
                       "public class Test1 {\n" +
                       "    private final Test2 test2 = null;\n" +
                       "}\n")
                .input("test/Test2.java",
                       "package test;\n" +
                       "public class Test2 {\n" +
                       "    private final Test1 test1 = null;\n" +
                       "}\n")
                .runGlobal(GlobalyUnused.class)
                .assertWarnings();
    }
    
    @Test
    public void testUnused() throws Exception {
        HintTest.create()
                .input("test/Test1.java",
                       "package test;\n" +
                       "public class Test1 { }\n")
                .input("test/Test2.java",
                       "package test;\n" +
                       "public class Test2 {\n" +
                       "}\n")
                .runGlobal(GlobalyUnused.class)
                .assertWarnings("1:13-1:18:verifier:Unused", "1:13-1:18:verifier:Unused");
    }

}
