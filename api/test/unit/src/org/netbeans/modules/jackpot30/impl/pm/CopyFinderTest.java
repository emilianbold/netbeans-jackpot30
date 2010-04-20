/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.java.hints.introduce.CopyFinder.VariableAssignments;

/**
 *
 * @author Jan Lahoda
 */
public class CopyFinderTest extends org.netbeans.modules.java.hints.introduce.CopyFinderTest {

    public CopyFinderTest(String name) {
        super(name);
    }

    @Override
    protected VariableAssignments computeVariables(CompilationInfo info, TreePath searchingFor, TreePath scope, AtomicBoolean cancel, Map<String, TypeMirror> designedTypeHack) {
        return convert(CopyFinder.computeVariables(info, searchingFor, scope, cancel, designedTypeHack));
    }

    @Override
    protected Map<TreePath, VariableAssignments> computeDuplicates(CompilationInfo info, TreePath searchingFor, TreePath scope, AtomicBoolean cancel, Map<String, TypeMirror> designedTypeHack) {
        Map<TreePath, VariableAssignments> result = new HashMap<TreePath, VariableAssignments>();
        
        for (Entry<TreePath, CopyFinder.VariableAssignments> e : CopyFinder.computeDuplicates(info, searchingFor, scope, cancel, designedTypeHack).entrySet()) {
            result.put(e.getKey(), convert(e.getValue()));
        }

        return result;
    }

    @Override
    protected Collection<TreePath> computeDuplicates(TreePath path) {
        return CopyFinder.computeDuplicates(info, path, new TreePath(info.getCompilationUnit()), new AtomicBoolean(), null).keySet();
    }

    @Override
    protected Map<String, Collection<TreePath>> performBulkSearch(String pattern) {
        BulkPattern bulkPattern = BulkSearch.getDefault().create(info, pattern);
        Map<String, Collection<TreePath>> bulkSearchResult = BulkSearch.getDefault().match(info, new TreePath(info.getCompilationUnit()), bulkPattern);

        return bulkSearchResult;
    }

    private static VariableAssignments convert(CopyFinder.VariableAssignments va) {
        if (va == null) return null;
        return new VariableAssignments(va.variables, va.multiVariables, va.variables2Names);
    }

    public void testMatchInterfaceNoFQN() throws Exception {
        performTest("package test; import java.util.*; public class Test { public void test() { |List| l1; |java.util.List| l2;} }");
    }
}
