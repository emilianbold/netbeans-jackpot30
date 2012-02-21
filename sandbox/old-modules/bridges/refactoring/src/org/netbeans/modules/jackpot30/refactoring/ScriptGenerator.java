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

package org.netbeans.modules.jackpot30.refactoring;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.source.WorkingCopy;

/**
 *
 * @author lahvac
 */
public class ScriptGenerator {

    public static @NonNull String constructRenameRule(@NonNull WorkingCopy wc, @NonNull String newName, @NonNull Element original) {
        switch (original.getKind()) {
            case METHOD: return constructRenameRule(wc, newName, (ExecutableElement) original);
            default: throw new UnsupportedOperationException();
        }
    }

    private static String constructRenameRule(WorkingCopy wc, String newName, ExecutableElement method) {
        //XXX: type parameters?
        StringBuilder pattern = new StringBuilder();
        StringBuilder constraints = new StringBuilder();
        StringBuilder target = new StringBuilder();

        TypeElement clazz = (TypeElement) method.getEnclosingElement();

        if (method.getModifiers().contains(Modifier.STATIC)) {
            pattern.append(clazz.getQualifiedName().toString()).append(".");
            target.append(clazz.getQualifiedName().toString()).append(".");
        } else {
            pattern.append("$0.");
            constraints.append(" :: $0 instanceof " + clazz.getQualifiedName().toString());
            target.append("$0.");
        }

        pattern.append(method.getSimpleName().toString());
        target.append(newName);
        pattern.append("(");
        target.append("(");

        int count = 1;

        for (VariableElement p : method.getParameters()) {
            if (count++ > 1) {
                pattern.append(", ");
                target.append(", ");
            }

            String name = "$" + p.getSimpleName().toString();

            pattern.append(name);
            target.append(name);

            if (constraints.length() == 0) {
                constraints.append(" :: ");
            } else {
                constraints.append(" && ");
            }
            
            constraints.append(name).append(" instanceof ").append(p.asType());
        }

        pattern.append(")");
        target.append(")");

        return "   " + pattern + constraints + "\n=> " + target + "\n;;";
    }

}
