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

package org.netbeans.modules.jackpot30.java.hints;

import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.java.hints.infrastructure.TreeRuleTestBase;
import org.netbeans.modules.java.hints.jackpot.code.spi.Hint;
import org.netbeans.modules.java.hints.jackpot.code.spi.TriggerPattern;
import org.netbeans.modules.java.hints.jackpot.spi.HintContext;
import org.netbeans.modules.java.hints.jackpot.spi.JavaFix;
import org.netbeans.modules.java.hints.jackpot.spi.support.ErrorDescriptionFactory;
import org.netbeans.modules.java.source.TreeLoader;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;

/**
 *
 * @author lahvac
 */
public class JavaHintsHintProviderTest extends TreeRuleTestBase {
    
    public JavaHintsHintProviderTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[] {"META-INF/generated-layer.xml"}, new Object[0]);
        TreeLoader.DISABLE_CONFINEMENT_TEST = true;
    }

    public void testCodeAPI() throws Exception {
        performFixTest("test/Test.java",
                       "|package test; public class Test {private void test() {Integer i = 0; if (i == null && null == i) System.err.println(i);\n}}\n",
                       "0:73-0:95:verifier:test",
                       "org.netbeans.modules.jackpot30.impl.batch.JavaFixImpl",
                       "package test; public class Test {private void test() {Integer i = 0; if (i == null) System.err.println(i); }} ");
    }

    @Override
    protected String toDebugString(CompilationInfo info, Fix f) {
        return f.getClass().getName();
    }

    @Hint(category="general")
    @TriggerPattern("$1 == null && null == $1")
    public static ErrorDescription codeHint(HintContext ctx) {
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "test", JavaFix.rewriteFix(ctx.getInfo(), "test", ctx.getPath(), "$1 == null", ctx.getVariables(), ctx.getMultiVariables(), ctx.getVariableNames(), Collections.<String, TypeMirror>emptyMap()));
    }

    @Override
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path) {
        return new HintsInvoker(info, new AtomicBoolean()).computeHints(info);
    }

    @Override
    public void testIssue105979() throws Exception {}
    @Override
    public void testIssue108246() throws Exception {}
    @Override
    public void testIssue113933() throws Exception {}
    @Override
    public void testNoHintsForSimpleInitialize() throws Exception {}

}
