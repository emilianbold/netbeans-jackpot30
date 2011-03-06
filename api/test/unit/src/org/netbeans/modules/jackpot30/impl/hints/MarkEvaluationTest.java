/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.hints;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Condition;
import org.netbeans.modules.jackpot30.spi.HintDescription.DeclarativeFixDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.ErrorDescriptionAcceptor;
import org.netbeans.modules.jackpot30.spi.HintDescription.FixAcceptor;
import org.netbeans.modules.jackpot30.spi.HintDescription.MarkCondition;
import org.netbeans.modules.jackpot30.spi.HintDescription.MarksWorker;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Selector;
import org.netbeans.modules.jackpot30.spi.HintDescriptionFactory;
import org.netbeans.modules.jackpot30.spi.HintMetadata;
import org.netbeans.modules.jackpot30.spi.HintMetadata.HintSeverity;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.jackpot30.spi.support.ErrorDescriptionFactory;
import org.netbeans.modules.java.hints.infrastructure.TreeRuleTestBase;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;

/**
 *
 * @author lahvac
 */
public class MarkEvaluationTest extends TreeRuleTestBase {

    public MarkEvaluationTest(String name) {
        super(name);
    }

//    public static TestSuite suite() {
//        NbTestSuite s = new NbTestSuite();
//
//        s.addTest(new MarkEvaluationTest("testSpeculativeAssignmentForFixes"));
//
//        return s;
//    }

    private HintMetadata prepareMetadata(String name) {
        return HintMetadata.create("no-id", name, "", "", true, HintMetadata.Kind.HINT_NON_GUI, HintSeverity.WARNING, Collections.<String>emptyList());
    }

    private void prepareSimpleEvaluationHint() throws Exception {
        List<Condition> globalConditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$_", "mark_var"), HintDescription.Operator.ASSIGN, new Selector("$var")));
        List<Condition> fix1Conditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$then", "mark_var"), HintDescription.Operator.EQUALS, new Selector("$var")));
        List<Condition> fix2Conditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$then", "mark_var"), HintDescription.Operator.NOT_EQUALS, new Selector("$var")));
        DeclarativeFixDescription f1 = new DeclarativeFixDescription(fix1Conditions, new FixAcceptorImpl("if ($var != $c) $then;"));
        DeclarativeFixDescription f2 = new DeclarativeFixDescription(fix2Conditions, new FixAcceptorImpl("if ($c == $var) $then;"));
        currentHint = HintDescriptionFactory.create().setMetadata(prepareMetadata("A"))
                                                     .setTriggerPattern(PatternDescription.create("if ($var == $c) $then;"))
                                                     .setWorker(new MarksWorker(globalConditions, new ErrorDescriptionAcceptorImpl("A"), Arrays.asList(f1, f2)))
                                                     .produce();

    }

    public void testSimpleMarkEvaluation1() throws Exception {
        prepareSimpleEvaluationHint();
        performFixTest("test/Test.java",
                       "|package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (i == 1);\n" +
                       "    }\n" +
                       "}\n",
                       "4:8-5:25:verifier:A",
                       "FixImpl",
                       ("package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i != 0)\n" +
                       "             if (i == 1);\n" +
                       "    }\n" +
                       "}\n").replaceAll("[ \t\n]+", " "));
    }
    
    public void testSimpleMarkEvaluation2() throws Exception {
        prepareSimpleEvaluationHint();
        performFixTest("test/Test.java",
                       "|package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (i == 1) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n",
                       "5:13-7:14:verifier:A",
                       "FixImpl",
                       ("package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (1 == i) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n").replaceAll("[ \t\n]+", " "));
    }

//    public void testSpeculativeAssignmentForFixes() throws Exception {
//        List<MarkCondition> globalConditions = Arrays.asList();
//        List<MarkCondition> fix1Conditions = Arrays.asList(new MarkCondition(new Selector("$then", "impossible"), HintDescription.Operator.EQUALS, new Selector("$var")),
//                                                           new MarkCondition(new Selector("$then", "mark_var"), HintDescription.Operator.ASSIGN, new Selector("$var")));
//        List<MarkCondition> fix2Conditions = Arrays.asList(new MarkCondition(new Selector("$_", "mark_var"), HintDescription.Operator.NOT_EQUALS, new Selector("$var")));
//        DeclarativeFixDescription f1 = new DeclarativeFixDescription(fix1Conditions, "if ($var != $c) $then;");
//        DeclarativeFixDescription f2 = new DeclarativeFixDescription(fix2Conditions, "if ($c == $var) $then;");
//        currentHint = HintDescriptionFactory.create().setDisplayName("A")
//                                                     .setTriggerPattern(PatternDescription.create("if ($var == $c) $then;"))
//                                                     .setWorker(new MarksWorker(globalConditions, new AcceptorImpl(), Arrays.asList(f1, f2)))
//                                                     .produce();
//
//        performFixTest("test/Test.java",
//                       "|package test;\n" +
//                       "public class Test {\n" +
//                       "    {\n" +
//                       "        int i = 0;\n" +
//                       "        if (i == 0)\n" +
//                       "             if (i == 1) {\n" +
//                       "                 System.err.println(1);\n" +
//                       "             }\n" +
//                       "    }\n" +
//                       "}\n",
//                       "4:8-7:14:verifier:A",
//                       "FixImpl",
//                       ("package test;\n" +
//                       "public class Test {\n" +
//                       "    {\n" +
//                       "        int i = 0;\n" +
//                       "        if (0 == i)\n" +
//                       "             if (i == 1) {\n" +
//                       "                 System.err.println(1);\n" +
//                       "             }\n" +
//                       "    }\n" +
//                       "}\n").replaceAll("[ \t\n]+", " "));
//    }

    public void testClearUnrealizedAssignments1() throws Exception {
        List<Condition> globalConditions = Arrays.<Condition>asList();
        List<Condition> fix1Conditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$then", "impossible"), HintDescription.Operator.EQUALS, new Selector("$var")),
                                                                  new MarkCondition(new Selector("$then", "mark_var"), HintDescription.Operator.ASSIGN, new Selector("$var")));
        List<Condition> fix2Conditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$_", "mark_var"), HintDescription.Operator.NOT_EQUALS, new Selector("$var")));
        DeclarativeFixDescription f1 = new DeclarativeFixDescription(fix1Conditions, new FixAcceptorImpl("if ($var != $c) $then;"));
        DeclarativeFixDescription f2 = new DeclarativeFixDescription(fix2Conditions, new FixAcceptorImpl("if ($c == $var) $then;"));
        currentHint = HintDescriptionFactory.create().setMetadata(prepareMetadata("A"))
                                                     .setTriggerPattern(PatternDescription.create("if ($var == $c) $then;"))
                                                     .setWorker(new MarksWorker(globalConditions, new ErrorDescriptionAcceptorImpl("A"), Arrays.asList(f1, f2)))
                                                     .produce();
        
        performFixTest("test/Test.java",
                       "|package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (i == 1) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n",
                       "5:13-7:14:verifier:A",
                       "FixImpl",
                       ("package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (1 == i) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n").replaceAll("[ \t\n]+", " "));
    }

    public void testClearUnrealizedAssignments2() throws Exception {
        List<Condition> hint1GlobalConditions = Arrays.<Condition>asList();
        List<Condition> hint2GlobalConditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$then", "impossible"), HintDescription.Operator.EQUALS, new Selector("$var")),
                                                                         new MarkCondition(new Selector("$then", "mark_var"), HintDescription.Operator.ASSIGN, new Selector("$var")));
        List<Condition> hint1Fix1Conditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$_", "mark_var"), HintDescription.Operator.NOT_EQUALS, new Selector("$var")));
        DeclarativeFixDescription h1f1 = new DeclarativeFixDescription(hint1Fix1Conditions, new FixAcceptorImpl("if ($c == $var) $then;"));
        HintDescription hint1 = HintDescriptionFactory.create().setMetadata(prepareMetadata("A"))
                                                               .setTriggerPattern(PatternDescription.create("if ($var == $c) $then;"))
                                                               .setWorker(new MarksWorker(hint1GlobalConditions, new ErrorDescriptionAcceptorImpl("A"), Arrays.asList(h1f1)))
                                                               .produce();
        HintDescription hint2 = HintDescriptionFactory.create().setMetadata(prepareMetadata("B"))
                                                               .setTriggerPattern(PatternDescription.create("if ($var == $c) $then;"))
                                                               .setWorker(new MarksWorker(hint2GlobalConditions, new ErrorDescriptionAcceptorImpl("B"), Collections.<DeclarativeFixDescription>emptyList()))
                                                               .produce();

        currentHints = Arrays.asList(hint1, hint2);

        performFixTest("test/Test.java",
                       "|package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (i == 1) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n",
                       "5:13-7:14:verifier:A",
                       "FixImpl",
                       ("package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (1 == i) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n").replaceAll("[ \t\n]+", " "));
    }

    public void testClearUnrealizedAssignments3() throws Exception {
        List<Condition> hint1GlobalConditions = Arrays.<Condition>asList();
        List<Condition> hint2GlobalConditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$then", "impossible"), HintDescription.Operator.EQUALS, new Selector("$var")));
        List<Condition> hint1Fix1Conditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$_", "mark_var"), HintDescription.Operator.NOT_EQUALS, new Selector("$var")));
        List<Condition> hint2Fix1Conditions = Arrays.<Condition>asList(new MarkCondition(new Selector("$then", "mark_var"), HintDescription.Operator.ASSIGN, new Selector("$var")));
        DeclarativeFixDescription h1f1 = new DeclarativeFixDescription(hint1Fix1Conditions, new FixAcceptorImpl("if ($c == $var) $then;"));
        DeclarativeFixDescription h2f1 = new DeclarativeFixDescription(hint2Fix1Conditions, new FixAcceptorImpl("if ($var != $c) $then;"));
        HintDescription hint1 = HintDescriptionFactory.create().setMetadata(prepareMetadata("A"))
                                                               .setTriggerPattern(PatternDescription.create("if ($var == $c) $then;"))
                                                               .setWorker(new MarksWorker(hint1GlobalConditions, new ErrorDescriptionAcceptorImpl("A"), Arrays.asList(h1f1)))
                                                               .produce();
        HintDescription hint2 = HintDescriptionFactory.create().setMetadata(prepareMetadata("B"))
                                                               .setTriggerPattern(PatternDescription.create("if ($var == $c) $then;"))
                                                               .setWorker(new MarksWorker(hint2GlobalConditions, new ErrorDescriptionAcceptorImpl("B"), Arrays.asList(h2f1)))
                                                               .produce();

        currentHints = Arrays.asList(hint1, hint2);

        performFixTest("test/Test.java",
                       "|package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (i == 1) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n",
                       "5:13-7:14:verifier:A",
                       "FixImpl",
                       ("package test;\n" +
                       "public class Test {\n" +
                       "    {\n" +
                       "        int i = 0;\n" +
                       "        if (i == 0)\n" +
                       "             if (1 == i) {\n" +
                       "                 System.err.println(1);\n" +
                       "             }\n" +
                       "    }\n" +
                       "}\n").replaceAll("[ \t\n]+", " "));
    }

    private HintDescription currentHint;
    private Collection<HintDescription> currentHints;
    
    @Override
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path) {
        Collection<HintDescription> hints;

        if (currentHint != null) {
            assertNull(currentHints);
            hints = Collections.singletonList(currentHint);
        } else {
            assertNotNull(currentHints);
            assertFalse(currentHints.isEmpty());
            hints = currentHints;
        }

        Map<Kind, List<HintDescription>> kind2Hints = new HashMap<Kind, List<HintDescription>>();
        Map<PatternDescription, List<HintDescription>> pattern2Hint = new HashMap<PatternDescription, List<HintDescription>>();
        RulesManager.sortOut(hints, kind2Hints, pattern2Hint);

        return new HintsInvoker(info, new AtomicBoolean()).computeHints(info, kind2Hints, pattern2Hint, new LinkedList<MessageImpl>());
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

    private static final class ErrorDescriptionAcceptorImpl implements ErrorDescriptionAcceptor {
        private final String displayName;
        public ErrorDescriptionAcceptorImpl(String displayName) {
            this.displayName = displayName;
        }
        public ErrorDescription accept(HintContext ctx) {
            return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), displayName);
        }
    };
    
    private static final class FixAcceptorImpl implements FixAcceptor {
        private final String to;
        public FixAcceptorImpl(String to) {
            this.to = to;
        }
        public Fix accept(HintContext ctx) {
            return JavaFix.rewriteFix(ctx, "FixImpl", ctx.getPath(), to);
        }
    }
}