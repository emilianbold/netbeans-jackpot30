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

package org.netbeans.modules.jackpot30.file.conditionapi;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import javax.lang.model.element.Element;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.MatcherUtilities;

/**
 *
 * @author lahvac
 */
public final class Matcher {

    private final HintContext ctx;

    //XXX: should not be public:
    public Matcher(HintContext ctx) {
        this.ctx = ctx;
    }

    public boolean matches(@NonNull Variable var, @NonNull String pattern) {
        return MatcherUtilities.matches(ctx, ctx.getVariables().get(var.variableName), pattern);
    }

    public boolean referencedIn(@NonNull Variable variable, @NonNull Variable in) {
        final Element e = ctx.getInfo().getTrees().getElement(ctx.getVariables().get(variable.variableName));

        if (e == null) { //TODO: check also error
            return false;
        }

        for (TreePath tp : Context.getVariable(ctx, in)) {
            boolean occurs = new TreePathScanner<Boolean, Void>() {
                @Override
                public Boolean scan(Tree tree, Void p) {
                    if (tree == null) {
                        return false;
                    }

                    TreePath currentPath = new TreePath(getCurrentPath(), tree);
                    Element currentElement = ctx.getInfo().getTrees().getElement(currentPath);

                    if (e.equals(currentElement)) {
                        return true; //TODO: throwing an exception might be faster...
                    }

                    return super.scan(tree, p);
                }

                @Override
                public Boolean reduce(Boolean r1, Boolean r2) {
                    if (r1 == null) {
                        return r2;
                    }

                    if (r2 == null) {
                        return r1;
                    }

                    return r1 || r2;
                }

            }.scan(tp, null) == Boolean.TRUE;

            if (occurs) {
                return true;
            }
        }

        return false;
    }

}
