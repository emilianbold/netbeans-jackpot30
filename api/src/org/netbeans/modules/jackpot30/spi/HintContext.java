/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.spi;

import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.spi.AbstractHint.HintSeverity;

/**
 *
 * @author Jan Lahoda
 */
public class HintContext {

    private final CompilationInfo info;
    private final HintSeverity severity;
    private final TreePath path;
    private final Map<String, TreePath> variables;
    private final Map<String, Collection<? extends TreePath>> multiVariables;
    private final Map<String, String> variableNames;

    public HintContext(CompilationInfo info, HintSeverity severity, TreePath path, Map<String, TreePath> variables, Map<String, Collection<? extends TreePath>> multiVariables, Map<String, String> variableNames) {
        this.info = info;
        this.severity = severity;
        this.path = path;

        variables = new HashMap<String, TreePath>(variables);
        variables.put("$_", path);
        
        this.variables = variables;
        this.multiVariables = multiVariables;
        this.variableNames = variableNames;
    }

    public CompilationInfo getInfo() {
        return info;
    }

    public HintSeverity getSeverity() {
        return severity;
    }

    public TreePath getPath() {
        return path;
    }

    public Map<String, TreePath> getVariables() {
        return variables;
    }

    public Map<String, Collection<? extends TreePath>> getMultiVariables() {
        return multiVariables;
    }

    public Map<String, String> getVariableNames() {
        return variableNames;
    }

    //XXX: probably should not be visible to clients:
    public static HintContext create(CompilationInfo info, HintSeverity severity, TreePath path, Map<String, TreePath> variables, Map<String, Collection<? extends TreePath>> multiVariables, Map<String, String> variableNames) {
        return new HintContext(info, severity, path, variables, multiVariables, variableNames);
    }
}
