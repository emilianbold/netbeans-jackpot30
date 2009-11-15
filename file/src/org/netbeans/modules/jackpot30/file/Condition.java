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

package org.netbeans.modules.jackpot30.file;

import com.sun.source.util.TreePath;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public abstract class Condition {

    public final boolean not;

    private Condition(boolean not) {
        this.not = not;
    }

    public abstract boolean holds(HintContext ctx, boolean global);

    @Override
    public abstract String toString();
    
    public static final class Instanceof extends Condition {

        public final String variable;
        public final String constraint;
        public final int[]  constraintSpan;

        public Instanceof(boolean not, String variable, String constraint, int[]  constraintSpan) {
            super(not);
            this.variable = variable;
            this.constraint = constraint;
            this.constraintSpan = constraintSpan;
        }

        @Override
        public boolean holds(HintContext ctx, boolean global) {
            if (global && !not) {
                //if this is a global condition, not == false, then the computation should always lead to true
                //note that ctx.getVariables().get(variable) might even by null (implicit this)
                return true;
            }

            TreePath boundTo = ctx.getVariables().get(variable);
            TypeMirror realType = ctx.getInfo().getTrees().getTypeMirror(boundTo);
            TypeElement jlObject = ctx.getInfo().getElements().getTypeElement("java.lang.Object");
            TypeMirror designedType = ctx.getInfo().getTreeUtilities().parseType(constraint, jlObject);

            return not ^ ctx.getInfo().getTypes().isSubtype(realType, designedType);
        }

        @Override
        public String toString() {
            return "(INSTANCEOF " + (not ? "!" : "") + variable + "/" + constraint + ")";
        }

    }

    public static final class MethodInvocation extends Condition {

        private final String methodName;
        private final Map<? extends String, ? extends ParameterKind> params;
        private final MethodInvocationContext mic;
        private final AtomicReference<Method> toCall = new AtomicReference<Method>();

        public MethodInvocation(boolean not, String methodName, Map<? extends String, ? extends ParameterKind> params, MethodInvocationContext mic) {
            super(not);
            this.methodName = methodName;
            this.params = params;
            this.mic = mic;
        }

        @Override
        public boolean holds(HintContext ctx, boolean global) {
            if (toCall.get() == null) {
                //not linked yet?
                if (!link()) {
                    throw new IllegalStateException();
                }
            }

            return mic.invokeMethod(ctx, toCall.get(), params) ^ not;
        }

        boolean link() {
            Method m = mic.linkMethod(methodName, params);

            toCall.set(m);

            return m != null;
        }

        @Override
        public String toString() {
            return "(METHOD_INVOCATION " + (not ? "!" : "") + ":" + methodName + "(" + params.toString() + "))";
        }

        public enum ParameterKind {
            VARIABLE,
            STRING_LITERAL,
            ENUM_CONSTANT;
        }
    }

    public static final class False extends Condition {

        public False() {
            super(false);
        }

        @Override
        public boolean holds(HintContext ctx, boolean global) {
            return false;
        }

        @Override
        public String toString() {
            return "(FALSE)";
        }
    }
}
