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

package org.netbeans.modules.jackpot30.file;

import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeMirror;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.jackpot30.spi.support.ErrorDescriptionFactory;
//import org.netbeans.modules.java.hints.spi.support.FixFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;

/**
 *
 * @author Jan Lahoda
 */
class DeclarativeHintsWorker implements Worker {

    private final String displayName;
    private final List<Condition> conditions;
    private final String imports;
    private final List<DeclarativeFix> fixes;
    private final Map<String, String> options;
    private final String primarySuppressWarningsKey;

    public DeclarativeHintsWorker(String displayName, List<Condition> conditions, String imports, List<DeclarativeFix> fixes, Map<String, String> options, String primarySuppressWarningsKey) {
        this.displayName = displayName;
        this.conditions = conditions;
        this.imports = imports;
        this.fixes = fixes;
        this.options = options;
        this.primarySuppressWarningsKey = primarySuppressWarningsKey;
    }

    //for tests:
    String getDisplayName() {
        return displayName;
    }

    //for tests:
    List<DeclarativeFix> getFixes() {
        return fixes;
    }

    public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
        for (Condition c : conditions) {
            if (!c.holds(ctx, true)) {
                return null;
            }
        }
        
        List<Fix> editorFixes = new LinkedList<Fix>();

        OUTER: for (DeclarativeFix fix : fixes) {
            for (Condition c : fix.getConditions()) {
                if (!c.holds(ctx, false)) {
                    continue OUTER;
                }
            }

            reportErrorWarning(ctx, fix.getOptions());

            //XXX: empty/noop fixes should not be realized:
            editorFixes.add(JavaFix.rewriteFix(ctx.getInfo(), fix.getDisplayName(), ctx.getPath(), fix.getPattern(), ctx.getVariables(), ctx.getMultiVariables(), ctx.getVariableNames(), ctx.getConstraints(), fix.getOptions(), imports));
        }

//        if (primarySuppressWarningsKey != null && primarySuppressWarningsKey.length() > 0) {
//            editorFixes.addAll(FixFactory.createSuppressWarnings(ctx.getInfo(), ctx.getPath(), primarySuppressWarningsKey));
//        }

        ErrorDescription ed = ErrorDescriptionFactory.forName(ctx, ctx.getPath(), displayName, editorFixes.toArray(new Fix[0]));

        if (ed == null) {
            return null;
        }

        return Collections.singletonList(ed);
    }

    private static void reportErrorWarning(HintContext ctx, Map<String, String> options) {
        String errorText = options.get("error");

        if (errorText != null)  {
            ctx.reportMessage(MessageKind.ERROR, errorText);
        }

        String warningText = options.get("warning");

        if (warningText != null)  {
            ctx.reportMessage(MessageKind.WARNING, warningText);
        }
    }

}
