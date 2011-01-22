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

package org.netbeans.modules.jackpot30.spi;

import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.MessageImpl;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.spi.HintMetadata.HintSeverity;

/**
 *
 * @author Jan Lahoda
 */
public class HintContext {

    private final CompilationInfo info;
    private final Preferences preferences;
    private final HintSeverity severity;
    private final Collection<? extends String> suppressWarningsKeys;
    private final TreePath path;
    private final List<Map<String, TreePath>> variables = new LinkedList<Map<String, TreePath>>();
    private final List<Map<String, Collection<? extends TreePath>>> multiVariables = new LinkedList<Map<String, Collection<? extends TreePath>>>();
    private final List<Map<String, String>> variableNames = new LinkedList<Map<String, String>>();
    private final Collection<? super MessageImpl> messages;
    private final Map<String, TypeMirror> constraints;

    public HintContext(CompilationInfo info, HintMetadata metadata, TreePath path, Map<String, TreePath> variables, Map<String, Collection<? extends TreePath>> multiVariables, Map<String, String> variableNames) {
        this(info, metadata, path, variables, multiVariables, variableNames, new LinkedList<MessageImpl>());
    }

    public HintContext(CompilationInfo info, HintMetadata metadata, TreePath path, Map<String, TreePath> variables, Map<String, Collection<? extends TreePath>> multiVariables, Map<String, String> variableNames, Collection<? super MessageImpl> problems) {
        this(info, metadata, path, variables, multiVariables, variableNames, Collections.<String, TypeMirror>emptyMap(), problems);
    }

    public HintContext(CompilationInfo info, HintMetadata metadata, TreePath path, Map<String, TreePath> variables, Map<String, Collection<? extends TreePath>> multiVariables, Map<String, String> variableNames, Map<String, TypeMirror> constraints, Collection<? super MessageImpl> problems) {
        this.info = info;
        this.preferences = metadata != null ? RulesManager.getInstance().getHintPreferences(metadata) : null;
        this.severity = preferences != null ? RulesManager.getInstance().getHintSeverity(metadata) : HintSeverity.WARNING;
        this.suppressWarningsKeys = metadata != null ? metadata.suppressWarnings : Collections.<String>emptyList();
        this.path = path;

        variables = new HashMap<String, TreePath>(variables);
        variables.put("$_", path);
        
        this.variables.add(Collections.unmodifiableMap(variables));
        this.multiVariables.add(Collections.unmodifiableMap(multiVariables));
        this.variableNames.add(Collections.unmodifiableMap(variableNames));
        this.messages = problems;
        this.constraints = constraints;
    }

    public HintContext(HintContext orig) {
        this.info = orig.info;
        this.preferences = orig.preferences;
        this.severity = orig.severity;
        this.suppressWarningsKeys = orig.suppressWarningsKeys;
        this.path = orig.path;
        this.variables.addAll(orig.variables);
        this.multiVariables.addAll(orig.multiVariables);
        this.variableNames.addAll(orig.variableNames);
        this.messages = orig.messages;
        this.constraints = orig.constraints;
    }

    public CompilationInfo getInfo() {
        return info;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public HintSeverity getSeverity() {
        return severity;
    }

    public TreePath getPath() {
        return path;
    }

    public void putVariable(String name, TreePath path) {
        variables.get(0).put(name, path);
    }
    
    public Map<String, TreePath> getVariables() {
        Map<String, TreePath> result = new HashMap<String, TreePath>();

        for (Map<String, TreePath> m : reverse(variables)) {
            result.putAll(m);
        }

        return result;
    }

    public void putMultiVariable(String name, Collection<? extends TreePath> path) {
        multiVariables.get(0).put(name, path);
    }
    
    public Map<String, Collection<? extends TreePath>> getMultiVariables() {
        Map<String, Collection<? extends TreePath>> result = new HashMap<String, Collection<? extends TreePath>>();

        for (Map<String, Collection<? extends TreePath>> m : reverse(multiVariables)) {
            result.putAll(m);
        }

        return result;
    }

    public void putVariableName(String variable, String name) {
        variableNames.get(0).put(variable, name);
    }
    
    public Map<String, String> getVariableNames() {
        Map<String, String> result = new HashMap<String, String>();

        for (Map<String, String> m : reverse(variableNames)) {
            result.putAll(m);
        }

        return result;
    }

    private <T> List<T> reverse(List<T> original) {
        List<T> result = new LinkedList<T>();

        for (T t : original) {
            result.add(0, t);
        }

        return result;
    }

    public Collection<? extends String> getSuppressWarningsKeys() {
        return suppressWarningsKeys;
    }

    //TODO: not sure it should be here:
    public Map<String, TypeMirror> getConstraints() {
        return constraints;
    }

    /**
     * Will be used only for refactoring(s), will be ignored for hints.
     * 
     * @param kind
     * @param text
     */
    public void reportMessage(MessageKind kind, String text) {
        messages.add(new MessageImpl(kind, text));
    }

    public void enterScope() {
        variables.add(0, new HashMap<String, TreePath>());
        multiVariables.add(0, new HashMap<String, Collection<? extends TreePath>>());
        variableNames.add(0, new HashMap<String, String>());
    }

    public void leaveScope() {
        variables.remove(0);
        multiVariables.remove(0);
        variableNames.remove(0);
    }
    
    //XXX: probably should not be visible to clients:
    public static HintContext create(CompilationInfo info, HintMetadata metadata, TreePath path, Map<String, TreePath> variables, Map<String, Collection<? extends TreePath>> multiVariables, Map<String, String> variableNames) {
        return new HintContext(info, metadata, path, variables, multiVariables, variableNames);
    }

    public enum MessageKind {
        WARNING, ERROR;
    }
    
}
