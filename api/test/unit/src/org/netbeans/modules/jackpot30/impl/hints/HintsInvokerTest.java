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

package org.netbeans.modules.jackpot30.impl.hints;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.jackpot30.spi.support.ErrorDescriptionFactory;
import org.netbeans.modules.java.hints.infrastructure.TreeRuleTestBase;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
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
                            "4:11-4:16:verifier:HINT");
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
                            "4:11-4:16:verifier:HINT");
    }

    public void testKind1() throws Exception {
        performAnalysisTest("test/Test.java",
                            "|package test;\n" +
                            "\n" +
                            "public class Test {\n" +
                            "     private void test(java.io.File f) {\n" +
                            "         f.toURL();\n" +
                            "     }\n" +
                            "}\n",
                            "4:11-4:16:verifier:HINT");
    }

    public void testPatternVariable1() throws Exception {
        performFixTest("test/Test.java",
                       "|package test;\n" +
                       "\n" +
                       "public class Test {\n" +
                       "     private void test() {\n" +
                       "         {\n" +
                       "             int y;\n" +
                       "             y = 1;\n" +
                       "         }\n" +
                       "         int z;\n" +
                       "         {\n" +
                       "             int y;\n" +
                       "             z = 1;\n" +
                       "         }\n" +
                       "     }\n" +
                       "}\n",
                       "4:9-7:10:verifier:HINT",
                       "FixImpl",
                       "package test; public class Test { private void test() { { int y = 1; } int z; { int y; z = 1; } } } ");
    }

    public void testPatternAssert1() throws Exception {
        performAnalysisTest("test/Test.java",
                            "|package test;\n" +
                            "\n" +
                            "public class Test {\n" +
                            "     private void test() {\n" +
                            "         assert true : \"\";\n" +
                            "     }\n" +
                            "}\n",
                            "4:9-4:26:verifier:HINT");
    }

    public void testPatternStatementAndSingleStatementBlockAreSame() throws Exception {
        performAnalysisTest("test/Test.java",
                            "|package test;\n" +
                            "\n" +
                            "public class Test {\n" +
                            "     private int test() {\n" +
                            "         if (true) {\n" +
                            "             return 0;\n" +
                            "         }\n" +
                            "     }\n" +
                            "}\n",
                            "4:9-6:10:verifier:HINT");
    }
    
    public void testPatternFalseOccurrence() throws Exception {
        performAnalysisTest("test/Test.java",
                            "|package test;\n" +
                            "\n" +
                            "public class Test {\n" +
                            "     private int test(java.io.File f) {\n" +
                            "         f.toURI().toURL();\n" +
                            "     }\n" +
                            "}\n");
    }

    private static final Map<String, HintDescription> test2Hint;

    static {
        test2Hint = new HashMap<String, HintDescription>();
        test2Hint.put("testPattern1", HintDescription.create(HintDescription.PatternDescription.create("$1.toURL()", Collections.singletonMap("$1", "java.io.File")), new WorkerImpl()));
        test2Hint.put("testPattern2", test2Hint.get("testPattern1"));
        test2Hint.put("testKind1", HintDescription.create(Kind.METHOD_INVOCATION, new WorkerImpl()));
        test2Hint.put("testPatternVariable1", HintDescription.create(HintDescription.PatternDescription.create("{ $1 $2; $2 = $3; }", Collections.<String, String>emptyMap()), new WorkerImpl("{ $1 $2 = $3; }")));
        Map<String, String> constraints = new HashMap<String, String>();

        constraints.put("$1", "boolean");
        constraints.put("$2", "java.lang.Object");

        test2Hint.put("testPatternAssert1", HintDescription.create(HintDescription.PatternDescription.create("assert $1 : $2;", constraints), new WorkerImpl()));
        test2Hint.put("testPatternStatementAndSingleStatementBlockAreSame", HintDescription.create(HintDescription.PatternDescription.create("if ($1) return $2;", Collections.<String, String>emptyMap()), new WorkerImpl()));
        test2Hint.put("testPatternFalseOccurrence", HintDescription.create(HintDescription.PatternDescription.create("$1.toURL()", Collections.singletonMap("$1", "java.io.File")), new WorkerImpl()));
    }

    @Override
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path) {
        HintDescription hd = test2Hint.get(getName());

        assertNotNull(hd);

        Map<Kind, List<HintDescription>> kind2Hints = new HashMap<Kind, List<HintDescription>>();
        Map<PatternDescription, List<HintDescription>> pattern2Hint = new HashMap<PatternDescription, List<HintDescription>>();
        RulesManager.sortOut(Collections.singletonList(hd), kind2Hints, pattern2Hint);

        return new HintsInvoker().computeHints(info, new TreePath(info.getCompilationUnit()), kind2Hints, pattern2Hint);
    }

    @Override
    protected String toDebugString(CompilationInfo info, Fix f) {
        return "FixImpl";
    }

    @Override
    public void testIssue105979() throws Exception {}

    @Override
    public void testIssue108246() throws Exception {}

    @Override
    public void testIssue113933() throws Exception {}

    @Override
    public void testNoHintsForSimpleInitialize() throws Exception {}

    private static final class WorkerImpl implements Worker {

        private final String fix;

        public WorkerImpl() {
            this(null);
        }

        public WorkerImpl(String fix) {
            this.fix = fix;
        }

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
            if (ctx.getInfo().getTreeUtilities().isSynthetic(ctx.getPath())) {
                return null;
            }

            List<Fix> fixes = new LinkedList<Fix>();

            if (fix != null) {
                fixes.add(JavaFix.rewriteFix(ctx.getInfo(), "Rewrite", ctx.getPath(), fix, ctx.getVariables(), ctx.getVariableNames(), /*XXX*/Collections.<String, TypeMirror>emptyMap()));
            }
            
            return Collections.singletonList(ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "HINT", fixes.toArray(new Fix[0])));
        }
    }

}