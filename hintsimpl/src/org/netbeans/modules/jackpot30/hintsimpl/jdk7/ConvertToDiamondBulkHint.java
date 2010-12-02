/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.hintsimpl.jdk7;

import com.sun.source.util.TreePath;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.tools.Diagnostic;
import org.netbeans.modules.java.hints.errors.ConvertToDiamond;
import org.netbeans.modules.java.hints.jackpot.code.spi.Hint;
import org.netbeans.modules.java.hints.jackpot.code.spi.TriggerPattern;
import org.netbeans.modules.java.hints.jackpot.code.spi.TriggerPatterns;
import org.netbeans.modules.java.hints.jackpot.spi.HintContext;
import org.netbeans.modules.java.hints.jackpot.spi.HintMetadata.Kind;
import org.netbeans.modules.java.hints.spi.ErrorRule;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;

/**
 *
 * @author lahvac
 */
@Hint(category="rules15",enabled=false, hintKind=Kind.HINT_NON_GUI)
public class ConvertToDiamondBulkHint {

    @TriggerPatterns({
        @TriggerPattern("new $clazz<$tparams$>($params$)")
    })
    public static List<ErrorDescription> compute(HintContext ctx) {
        List<ErrorDescription> result = new LinkedList<ErrorDescription>();
        ErrorRule<Void> convert = new ConvertToDiamond();
        Set<String> codes = convert.getCodes();
        TreePath clazz = ctx.getVariables().get("$clazz");
        long start = ctx.getInfo().getTrees().getSourcePositions().getStartPosition(clazz.getCompilationUnit(), clazz.getLeaf());

        for (Diagnostic<?> d : ctx.getInfo().getDiagnostics()) {
            if (start != d.getStartPosition()) continue;
            if (!codes.contains(d.getCode())) continue;

            List<Fix> fixes = convert.run(ctx.getInfo(), d.getCode(), (int) d.getPosition(), ctx.getInfo().getTreeUtilities().pathFor((int) d.getPosition() + 1), null);
            result.add(ErrorDescriptionFactory.createErrorDescription(Severity.ERROR, "", fixes, ctx.getInfo().getFileObject(), (int) d.getStartPosition(), (int) d.getEndPosition()));
        }

        return result;
    }

}
