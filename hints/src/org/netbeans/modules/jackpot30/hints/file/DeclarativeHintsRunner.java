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

package org.netbeans.modules.jackpot30.hints.file;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.modules.jackpot30.hints.epi.ErrorDescriptionFactory;
import org.netbeans.modules.jackpot30.hints.epi.HintContext;
import org.netbeans.modules.jackpot30.hints.epi.JavaFix;
import org.netbeans.modules.jackpot30.hints.epi.Pattern;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;

/**
 *
 * @author Jan Lahoda
 */
public class DeclarativeHintsRunner extends AbstractHint {

    private final AtomicBoolean cancel = new AtomicBoolean();
    
    public DeclarativeHintsRunner() {
        super(true, false, HintSeverity.WARNING);
    }

    @Override
    public String getDescription() {
        return "Declarative Hints Runner";
    }

    public Set<Kind> getTreeKinds() {
//        return EnumSet.allOf(Kind.class);
        return EnumSet.of(Kind.METHOD_INVOCATION);
    }

    public List<ErrorDescription> run(CompilationInfo compilationInfo, TreePath treePath) {
        //XXX: very, very bad performance:
        List<ErrorDescription> result = new LinkedList<ErrorDescription>();
        
        for (DeclarativeHint h : DeclarativeHintRegistry.getAllHints()) {
            Map<String, TreePath> vars = Pattern.compile(compilationInfo, h.getPattern()).match(treePath); //XXX: cancellability

            if (vars == null)
                continue;

            Fix[] fixes = new Fix[h.getFixes().size()];

            for (int cntr = 0; cntr < h.getFixes().size(); cntr++) {
                fixes[cntr] = JavaFix.rewriteFix(compilationInfo, h.getFixes().get(cntr).getDisplayName(), treePath, h.getFixes().get(cntr).getPattern(), vars, Collections.<String, TypeMirror>emptyMap()/*XXX*/);
            }

            ErrorDescription ed = ErrorDescriptionFactory.forName(HintContext.create(compilationInfo, getSeverity(), treePath), treePath, h.getDisplayName(), fixes);

            if (ed == null) {
                continue;
            }

            result.add(ed);
        }

        return result;
    }

    public String getId() {
        return DeclarativeHintsRunner.class.getName();
    }

    public String getDisplayName() {
        return "Declarative Hints Runner";
    }

    public void cancel() {}

}
