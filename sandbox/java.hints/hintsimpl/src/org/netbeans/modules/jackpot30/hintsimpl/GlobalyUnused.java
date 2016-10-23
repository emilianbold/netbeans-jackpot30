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
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.hints.Decision;
import org.netbeans.spi.java.hints.Decision.Factory;
import org.netbeans.spi.java.hints.ErrorDescriptionFactory;
import org.netbeans.spi.java.hints.Hint;
import org.netbeans.spi.java.hints.HintContext;
import org.netbeans.spi.java.hints.TriggerDecision;
import org.netbeans.spi.java.hints.TriggerTreeKind;

/**
 *
 * @author lahvac
 */
@Hint(displayName="Unused", description="Unused", category="general")
public class GlobalyUnused {
    
    @TriggerTreeKind({Kind.IDENTIFIER, Kind.MEMBER_SELECT})
    public static ErrorDescription usage(HintContext ctx) {
        DecisionImpl d = decisionOrNull(ctx);
        
        if (d != null) {
            System.err.println("recording usage");
            d.recordLocalFact(ctx.getInfo(), null);
        }
        
        return null;
    }
    
    @TriggerTreeKind({Kind.CLASS})
    public static ErrorDescription declaration(HintContext ctx) {
        decisionOrNull(ctx);
        return null;
    }
    
    @TriggerDecision(DecisionImpl.class)
    public static ErrorDescription produceWarning(HintContext ctx) {
        if (((DecisionImpl) ctx.decision).getCurrentResult() != Boolean.TRUE) {
            return ErrorDescriptionFactory.forName(ctx, ctx.getPath(), "Unused");
        }
        
        return null;
    }
    
    private static DecisionImpl decisionOrNull(HintContext ctx) {
        Element el = ctx.getInfo().getTrees().getElement(ctx.getPath());
        
        if (el == null || (el.asType() != null && el.asType().getKind() == TypeKind.ERROR) || el.getKind() != ElementKind.CLASS) return null;
        
        TreePathHandle h = TreePathHandle.create(el, ctx.getInfo());
        
        System.err.println("decision for: " + el);
        return ctx.findDecision(h, new Factory<Void, Boolean, DecisionImpl>(DecisionImpl.class) {
            @Override
            public DecisionImpl create(TreePathHandle handle) {
                return new DecisionImpl(handle);
            }
        });
    }
    
    private static final class DecisionImpl extends Decision<Void, Boolean> {

        public DecisionImpl(TreePathHandle root) {
            super(root);
        }

        @Override
        protected Boolean makeDecision(Iterable<? extends Void> input) {
            return input.iterator().hasNext();
        }
        
    }
}
