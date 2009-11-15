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

package org.netbeans.modules.jackpot30.file.debugging;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.JavaSource.Priority;
import org.netbeans.api.java.source.JavaSourceTaskFactory;
import org.netbeans.api.java.source.support.SelectionAwareJavaSourceTaskFactory;
import org.netbeans.modules.jackpot30.file.Condition;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.FixTextDescription;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.MatcherUtilities;
import org.netbeans.modules.java.hints.spi.AbstractHint.HintSeverity;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
public class EvaluationSpanTask implements CancellableTask<CompilationInfo> {

    public void run(CompilationInfo info) throws Exception {
        DebugTopComponent c = DebugTopComponent.getExistingInstance();

        if (c == null) return ;

        Document doc = c.getDocument();

        assert doc != null;

        List<int[]> passed = new LinkedList<int[]>();
        List<int[]> failed = new LinkedList<int[]>();

        computeHighlights(info,
                          SelectionAwareJavaSourceTaskFactory.getLastSelection(info.getFileObject()),
                          c.getHints(),
                          passed,
                          failed);

        OffsetsBag bag = new OffsetsBag(doc);

        for (int[] span : passed) {
            bag.addHighlight(span[0], span[1], PASSED);
        }

        for (int[] span : failed) {
            bag.addHighlight(span[0], span[1], FAILED);
        }

        DebuggingHighlightsLayerFactory.getBag(doc).setHighlights(bag);
    }

    static void computeHighlights(CompilationInfo info,
                                  int[] selectionSpan,
                                  Collection<? extends HintWrapper> hints,
                                  List<int[]> passed,
                                  List<int[]> failed) {
        if (hints.isEmpty()) return ;

        if (selectionSpan == null || selectionSpan[0] == selectionSpan[1])
            return ;

        TreePath tp = validateSelection(info, selectionSpan[0], selectionSpan[1]);

        if (tp == null) {
            return ;
        }
        
        for (HintWrapper d : hints) {
            Map<String, TreePath> variables = new HashMap<String, TreePath>();
            Map<String, Collection<? extends TreePath>> multiVariables = new HashMap<String, Collection<? extends TreePath>>();
            Map<String, String> variableNames = new HashMap<String, String>();

            variables.put("$_", tp);

            HintContext ctx = new HintContext(info, HintSeverity.ERROR, tp, variables, multiVariables, variableNames);
            String pattern = d.spec.substring(d.desc.textStart, d.desc.textEnd);

            boolean matches = MatcherUtilities.matches(ctx, tp, pattern, true);

            List<int[]> target = matches ? passed : failed;

            target.add(trim(d.spec, new int[] {d.desc.textStart, d.desc.textEnd}));

            if (matches) {
                evaluateConditions(d.desc.conditions, d.desc.conditionSpans, ctx, passed, failed, d);

                for (FixTextDescription f : d.desc.fixes) {
                    evaluateConditions(f.conditions, f.conditionSpans, ctx, passed, failed, d);
                }
            }
        }
    }

    private static void evaluateConditions(Iterable<Condition> conditions, Iterable<int[]> conditionSpans, HintContext ctx, List<int[]> passed, List<int[]> failed, HintWrapper d) {
        Iterator<Condition> cond = conditions.iterator();
        Iterator<int[]> span = conditionSpans.iterator();

        while (cond.hasNext() && span.hasNext()) {
            boolean holds = cond.next().holds(ctx, true);
            List<int[]> condTarget = holds ? passed : failed;
            
            condTarget.add(trim(d.spec, span.next()));
        }
    }

    private static int[] trim(String spec, int[] span) {
        while (Character.isWhitespace(spec.charAt(span[0])))
            span[0]++;
        while (Character.isWhitespace(spec.charAt(span[1] - 1)))
            span[1]--;
        return span;
    }

    private static final AttributeSet PASSED = AttributesUtilities.createImmutable(StyleConstants.Background, Color.GREEN);
    private static final AttributeSet FAILED = AttributesUtilities.createImmutable(StyleConstants.Background, Color.RED);

    private static TreePath validateSelection(CompilationInfo ci, int start, int end) {
        TreePath tp = ci.getTreeUtilities().pathFor((start + end) / 2 + 1);

        for ( ; tp != null; tp = tp.getParentPath()) {
            Tree leaf = tp.getLeaf();

            long treeStart = ci.getTrees().getSourcePositions().getStartPosition(ci.getCompilationUnit(), leaf);
            long treeEnd   = ci.getTrees().getSourcePositions().getEndPosition(ci.getCompilationUnit(), leaf);

            if (treeStart != start || treeEnd != end) {
                continue;
            }

            return tp;
        }

        return null;
    }

    public void cancel() {
        //XXX
    }

    @ServiceProvider(service=JavaSourceTaskFactory.class)
    public static final class FactoryImpl extends SelectionAwareJavaSourceTaskFactory {

        public FactoryImpl() {
            super(Phase.RESOLVED, Priority.BELOW_NORMAL);
        }

        @Override
        protected CancellableTask<CompilationInfo> createTask(FileObject file) {
            return new EvaluationSpanTask();
        }

    }
}
