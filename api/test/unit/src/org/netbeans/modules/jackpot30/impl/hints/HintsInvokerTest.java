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
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;
import org.netbeans.modules.jackpot30.spi.support.ErrorDescriptionFactory;
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

    private static final Map<String, HintDescription> test2Hint;

    static {
        test2Hint = new HashMap<String, HintDescription>();
        test2Hint.put("testPattern1", HintDescription.create(HintDescription.PatternDescription.create("$1.toURL()", Collections.singletonMap("$1", "java.io.File")), new WorkerImpl()));
        test2Hint.put("testPattern2", test2Hint.get("testPattern1"));
    }

    @Override
    protected List<ErrorDescription> computeErrors(CompilationInfo info, TreePath path) {
        HintDescription hd = test2Hint.get(getName());

        assertNotNull(hd);

        Map<Kind, List<HintDescription>> kind2Hints = new HashMap<Kind, List<HintDescription>>();
        Map<PatternDescription, List<HintDescription>> pattern2Hint = new HashMap<PatternDescription, List<HintDescription>>();

        pattern2Hint.put(hd.getTriggerPattern(), Collections.singletonList(hd));
        return new HintsInvoker().computeHints(info, new TreePath(info.getCompilationUnit()), kind2Hints, pattern2Hint);
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
        public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
            return Collections.singletonList(ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "HINT"));
        }
    }

}