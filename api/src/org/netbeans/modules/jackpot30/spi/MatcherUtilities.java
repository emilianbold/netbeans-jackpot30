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

package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.pm.CopyFinder;

/**
 *
 * @author lahvac
 */
public class MatcherUtilities {

    public static boolean matches(@NonNull HintContext ctx, @NonNull TreePath variable, @NonNull String pattern) {
        return matches(ctx, variable, pattern, null, null, null);
    }

    public static boolean matches(@NonNull HintContext ctx, @NonNull TreePath variable, @NonNull String pattern, Map<String, TreePath> outVariables, Map<String, Collection<? extends TreePath>> outMultiVariables, Map<String, String> outVariables2Names) {
        Scope s = Utilities.constructScope(ctx.getInfo(), Collections.<String, TypeMirror>emptyMap());
        Tree  patternTree = Utilities.parseAndAttribute(ctx.getInfo(), pattern, s);
        TreePath patternTreePath = new TreePath(new TreePath(ctx.getInfo().getCompilationUnit()), patternTree);
        Map<String, TreePath> variables = new HashMap<String, TreePath>(ctx.getVariables());
        Map<String, Collection<? extends TreePath>> multiVariables = new HashMap<String, Collection<? extends TreePath>>(ctx.getMultiVariables());
        Map<String, String> variables2Names = new HashMap<String, String>(ctx.getVariableNames());

        if (CopyFinder.isDuplicate(ctx.getInfo(), patternTreePath, variable, true, variables, multiVariables, variables2Names, new AtomicBoolean()/*XXX*/)) {
            outVariables(outVariables, variables, ctx.getVariables());
            outVariables(outMultiVariables, multiVariables, ctx.getMultiVariables());
            outVariables(outVariables2Names, variables2Names, ctx.getVariableNames());

            return true;
        }

        return false;
    }

    public static boolean matches(@NonNull HintContext ctx, @NonNull Collection<? extends TreePath> variable, @NonNull String pattern, Map<String, TreePath> outVariables, Map<String, Collection<? extends TreePath>> outMultiVariables, Map<String, String> outVariables2Names) {
        Scope s = Utilities.constructScope(ctx.getInfo(), Collections.<String, TypeMirror>emptyMap());
        Tree  patternTree = Utilities.parseAndAttribute(ctx.getInfo(), pattern, s);
        List<? extends Tree> patternTrees;

        if (Utilities.isFakeBlock(patternTree)) {
            List<? extends StatementTree> statements = ((BlockTree) patternTree).getStatements();

            patternTrees = statements.subList(1, statements.size() - 1);
        } else {
            patternTrees = Collections.singletonList(patternTree);
        }

        if (variable.size() != patternTrees.size()) return false;
        
        Map<String, TreePath> variables = new HashMap<String, TreePath>(ctx.getVariables());
        Map<String, Collection<? extends TreePath>> multiVariables = new HashMap<String, Collection<? extends TreePath>>(ctx.getMultiVariables());
        Map<String, String> variables2Names = new HashMap<String, String>(ctx.getVariableNames());
        Iterator<? extends TreePath> variableIt = variable.iterator();
        Iterator<? extends Tree> patternTreesIt = patternTrees.iterator();

        while (variableIt.hasNext() && patternTreesIt.hasNext()) {
            TreePath patternTreePath = new TreePath(new TreePath(ctx.getInfo().getCompilationUnit()), patternTreesIt.next());

            if (!CopyFinder.isDuplicate(ctx.getInfo(), patternTreePath, variableIt.next(), true, variables, multiVariables, variables2Names, new AtomicBoolean()/*XXX*/)) {
                return false;
            }
        }

        if (variableIt.hasNext() == patternTreesIt.hasNext()) {
            outVariables(outVariables, variables, ctx.getVariables());
            outVariables(outMultiVariables, multiVariables, ctx.getMultiVariables());
            outVariables(outVariables2Names, variables2Names, ctx.getVariableNames());

            return true;
        }

        return false;
    }

    private static <T> void outVariables(Map<String, T> outMap, Map<String, T> currentValues, Map<String, T> origValues) {
        if (outMap == null) return;

        for (String key : origValues.keySet()) {
            currentValues.remove(key);
        }

        outMap.putAll(currentValues);
    }

}
