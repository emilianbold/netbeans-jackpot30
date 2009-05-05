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

package org.netbeans.modules.jackpot30.hints.pm;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.hints.epi.Constraint;
import org.netbeans.modules.jackpot30.hints.epi.ErrorDescriptionFactory;
import org.netbeans.modules.jackpot30.hints.epi.HintContext;
import org.netbeans.modules.jackpot30.hints.epi.TriggerPattern;
import org.netbeans.modules.jackpot30.hints.pm.RulesManager.PatternDescription;
import org.netbeans.modules.java.hints.infrastructure.TreeRuleTestBase;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.Exceptions;

/**
 *
 * @author user
 */
public class HintsInvokerTest extends TreeRuleTestBase {

    public HintsInvokerTest(String name) {
        super(name);
    }

    public void testPattern1() throws Exception {
        performAnalysisTest("test/Test.java",
                            "|package test;\n" +
                            "import java.io.File;\n" +
                            "public class Test {\n" +
                            "     private void test(File f) {\n" +
                            "         f.toURL();\n" +
                            "     }\n" +
                            "}\n",
                            "4:11-4:16:verifier:Use of java.io.File.toURL()");
    }

    public void testPattern2() throws Exception {
        performAnalysisTest("test/Test.java",
                            "|package test;\n" +
                            "\n" +
                            "public class Test {\n" +
                            "     private void test(java.io.File f) {\n" +
                            "         f.toURL();\n" +
                            "     }\n" +
                            "}\n",
                            "4:11-4:16:verifier:Use of java.io.File.toURL()");
    }

    @TriggerPattern(value="$1.toURL()", constraints=@Constraint(variable="$1", type="java.io.File"))
    public static ErrorDescription hintPattern1(HintContext ctx) {
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "Use of java.io.File.toURL()");
    }

    @TriggerPattern(value="$1.toURL()", constraints=@Constraint(variable="$1", type="java.io.File"))
    public static ErrorDescription hintPattern2(HintContext ctx) {
        return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "Use of java.io.File.toURL()");
    }
    
    @Override
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path) {
        try {
            String methodName = "hint" + getName().substring("test".length());
            Method m = getClass().getDeclaredMethod(methodName, HintContext.class);
            Map<Kind, List<Method>> kind2Hints = new HashMap<Kind, List<Method>>();
            Map<PatternDescription, List<Method>> pattern2Hint = new HashMap<PatternDescription, List<Method>>();

            RulesManager.processMethod(kind2Hints, pattern2Hint, m);
            return new HintsInvoker().computeHints(info, new TreePath(info.getCompilationUnit()), kind2Hints, pattern2Hint);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException(ex);
        } catch (SecurityException ex) {
            throw new IllegalStateException(ex);
        }
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