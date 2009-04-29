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
 * License Header, with the fields enclosed by brackets [] replaced bysanno
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

package org.netbeans.modules.jackpot30.hints.pm;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
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
public class AnnotationBasedHintsRunner extends AbstractHint {

    private final AtomicBoolean cancel = new AtomicBoolean();
    
    public AnnotationBasedHintsRunner() {
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
        Element e = compilationInfo.getTrees().getElement(treePath);

        if (e == null) {
            return null;
        }

        List<ErrorDescription> result = new LinkedList<ErrorDescription>();

        for (AnnotationMirror am : e.getAnnotationMirrors()) {
            String fqn = ((TypeElement) am.getAnnotationType().asElement()).getQualifiedName().toString();

            if ("org.netbeans.api.java.rewrite.Transformation".equals(fqn)) {
                ErrorDescription ed = handleTransformation(compilationInfo, treePath, am);

                if (ed != null) {
                    result.add(ed);
                }
                
                continue;
            }

            if ("org.netbeans.api.java.rewrite.TransformationSet".equals(fqn)) {
                for (AnnotationMirror m : findValue(am, "value", AnnotationMirror[].class)) {
                    ErrorDescription ed = handleTransformation(compilationInfo, treePath, am);

                    if (ed != null) {
                        result.add(ed);
                    }
                }

                continue;
            }
        }

        return result;
    }

    public String getId() {
        return AnnotationBasedHintsRunner.class.getName();
    }

    public String getDisplayName() {
        return "Declarative Hints Runner";
    }

    public void cancel() {}

    private ErrorDescription handleTransformation(CompilationInfo compilationInfo, TreePath treePath, AnnotationMirror am) {
        AnnotationMirror pattern = findValue(am, "pattern", AnnotationMirror.class);
        String displayName = findValue(am, "displayName", String.class);
        String patternString = findValue(pattern, "pattern", String.class);
        @SuppressWarnings("unchecked")//TODO: NbCollections
        Collection<AnnotationMirror> constraints = (Collection<AnnotationMirror>) findValue(pattern, "constraints", Collection.class);
        Map<String, TypeMirror> variableTypes = new HashMap<String, TypeMirror>();
        
        for (AnnotationMirror m : constraints) {
            String var = findValue(m, "variable", String.class);
            TypeMirror tm = findValue(m, "type", TypeMirror.class);

            variableTypes.put(var, tm);
        }

        Map<String, TreePath> variables = Pattern.matchesPattern(compilationInfo, patternString, variableTypes, treePath, cancel);

        if (variables == null) {
            return null;
        }
        
        List<Fix> fixes = new LinkedList<Fix>();

        for (AnnotationMirror f : (Collection<AnnotationMirror>) findValue(am, "fix", Collection.class)) {
            String dn = findValue(f, "displayName", String.class);
            String to = findValue(f, "value", String.class);

            if (dn.length() == 0) {
                dn = "Rewrite to " + to;
            }
            
            fixes.add(JavaFix.rewriteFix(compilationInfo, dn, treePath, to, variables));
        }

        return ErrorDescriptionFactory.forName(HintContext.create(compilationInfo, this), treePath, displayName, fixes.toArray(new Fix[0]));
    }

    private <T> T findValue(AnnotationMirror m, String name, Class<T> clazz) {
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : m.getElementValues().entrySet()) {
            if (name.equals(e.getKey().getSimpleName().toString())) {
                return clazz.cast(e.getValue().getValue());
            }
        }

        TypeElement te = (TypeElement) m.getAnnotationType().asElement();

        for (ExecutableElement ee : ElementFilter.methodsIn(te.getEnclosedElements())) {
            if (name.equals(ee.getSimpleName().toString())) {
                return clazz.cast(ee.getDefaultValue().getValue());
            }
        }
        throw new IllegalStateException();
    }
}
