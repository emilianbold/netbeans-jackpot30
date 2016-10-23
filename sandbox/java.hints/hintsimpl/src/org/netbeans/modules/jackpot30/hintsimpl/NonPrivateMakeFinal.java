/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2013 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.hintsimpl;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.EnumSet;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.java.hints.introduce.Flow;
import org.netbeans.modules.java.hints.introduce.Flow.FlowResult;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.hints.Decision;
import org.netbeans.spi.java.hints.Decision.Factory;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerDecision;
import org.netbeans.spi.java.hints.TriggerPattern;
import org.netbeans.spi.java.hints.TriggerTreeKind;
import org.netbeans.spi.java.hints.support.FixFactory;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author lahvac
 */
@Hint(displayName="Non Private Make Final", description="Non Private Make Final", category="general")
public class NonPrivateMakeFinal {

    @TriggerPattern("$variable = $value")
    public static ErrorDescription write(HintContext ctx) {
        Element field = ctx.getInfo().getTrees().getElement(ctx.getVariables().get("$variable"));

        if (field == null || field.getKind() != ElementKind.FIELD) {
            //unknown, or not a field, ignore.
            return null;
        }

        if (ctx.getInfo().getTopLevelElements().contains(ctx.getInfo().getElementUtilities().outermostTypeElement(field))) {
            //field in current CU, ignore (will be handled by local flow)
            return null;
        }

        decision(ctx, (VariableElement) field).recordLocalFact(ctx.getInfo(), false);
        
        return null;
    }
    
    @TriggerTreeKind({Kind.VARIABLE})
    public static ErrorDescription declaration(HintContext ctx) {
        Element ve = ctx.getInfo().getTrees().getElement(ctx.getPath());

        if (ve == null || ve.getKind() != ElementKind.FIELD || ve.getModifiers().contains(Modifier.FINAL) || /*TODO: the point of volatile?*/ve.getModifiers().contains(Modifier.VOLATILE)) return null;

        //handle all fields, even the private ones, which are handled by the standard hints.
        
        FlowResult flow = Flow.assignmentsForUse(ctx);

        if (flow == null || ctx.isCanceled()) return null;

        DecisionImpl decision = decision(ctx, (VariableElement) ve);

        decision.recordLocalFact(ctx.getInfo(), flow.getFinalCandidates().contains((VariableElement) ve));

        return null;
    }
    
    @TriggerDecision(DecisionImpl.class)
    @Messages("FIX_CanBeFinal={0} can be final")
    public static ErrorDescription produceWarning(HintContext ctx) {
        if (((DecisionImpl) ctx.decision).getCurrentResult() == Boolean.TRUE) {
            VariableTree vt = (VariableTree) ctx.getPath().getLeaf();
            Fix fix = FixFactory.addModifiersFix(ctx.getInfo(), new TreePath(ctx.getPath(), vt.getModifiers()), EnumSet.of(Modifier.FINAL), Bundle.FIX_CanBeFinal(vt.getName().toString()));
            return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "Can be final", fix);
        }
        
        return null;
    }
    
    private static @NonNull DecisionImpl decision(HintContext ctx, @NonNull VariableElement field) {
        TreePathHandle h = TreePathHandle.create(field, ctx.getInfo());
        
        return ctx.findDecision(h, new Factory<Boolean, Boolean, DecisionImpl>(DecisionImpl.class) {
            @Override
            public DecisionImpl create(TreePathHandle handle) {
                return new DecisionImpl(handle);
            }
        });
    }
    
    private static final class DecisionImpl extends Decision<Boolean, Boolean> {

        public DecisionImpl(TreePathHandle root) {
            super(root);
        }

        @Override
        protected Boolean makeDecision(Iterable<? extends Boolean> input) {
            for (Boolean b : input) {
                if (b == null || !b) return false;
            }
            return true;
        }
        
    }
}
