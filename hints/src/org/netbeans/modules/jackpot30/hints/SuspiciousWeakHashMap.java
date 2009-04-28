/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javahints;
//import com.sun.source.tree.Tree.Kind;
//import com.sun.source.tree.VariableTree;
//import com.sun.source.util.TreePath;
//import java.util.Collections;
//import java.util.EnumSet;
//import java.util.List;
//import java.util.Set;
//import java.util.WeakHashMap;
//import javax.lang.model.element.Element;
//import javax.lang.model.element.ElementKind;
//import javax.lang.model.element.TypeElement;
//import javax.lang.model.element.VariableElement;
//import javax.lang.model.type.DeclaredType;
//import javax.lang.model.type.TypeKind;
//import javax.lang.model.type.TypeMirror;
//import javax.lang.model.util.ElementFilter;
//import org.netbeans.api.java.source.CompilationInfo;
//import org.netbeans.modules.java.hints.spi.AbstractHint;
//import org.netbeans.modules.java.hints.spi.nue.HintContext;
//import org.netbeans.modules.java.hints.spi.nue.JavaHint;
//import org.netbeans.spi.editor.hints.ErrorDescription;
//import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
//import org.netbeans.spi.editor.hints.Severity;
//
///**
// *
// * @author Jan Lahoda
// */
//public class SuspiciousWeakHashMap {//extends AbstractHint {
//
//    private SuspiciousWeakHashMap() {
////        super(false, true, HintSeverity.WARNING);
//    }
//
////    @Override
////    public String getDescription() {
////        return "Suspicious WeakHashMap";
////    }
////
////    public Set<Kind> getTreeKinds() {
////        return EnumSet.of(Kind.VARIABLE);
////    }
//
//    @JavaHint(id="SuspiciousWeakHashMap", kinds={Kind.VARIABLE})
//    public static List<ErrorDescription> run(HintContext context, TreePath tp) {
//        CompilationInfo info = context.getInfo();
//        Element e = info.getTrees().getElement(tp);
//
//        if (e == null || e.getKind() != ElementKind.FIELD) {
//            return null;
//        }
//
//        TypeMirror type = e.asType();
//
//        if (type.getKind() != TypeKind.DECLARED) {
//            return null;
//        }
//
//        DeclaredType dt = (DeclaredType) type;
//        TypeElement te = (TypeElement) dt.asElement();
//
//        if (!te.getQualifiedName().contentEquals("java.util.WeakHashMap")) {
//            return null;
//        }
//
//        List<? extends TypeMirror> typeArguments = dt.getTypeArguments();
//
//        if (typeArguments.size() != 2) {
//            return null;
//        }
//
//        TypeMirror key = typeArguments.get(0);
//        TypeMirror value = typeArguments.get(1);
//
//        if (!detectUseOfKeyInValue(info, key, value)) {
//            return null;
//        }
//
//        int[] span = info.getTreeUtilities().findNameSpan((VariableTree) tp.getLeaf());
//
//        if (span == null) {
//            return null;
//        }
//
//        ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(/*getSeverity().toEditorSeverity()*/Severity.WARNING, "Value contains field!", info.getFileObject(), span[0], span[1]);
//
//        return Collections.singletonList(ed);
//    }
//
//    private static boolean detectUseOfKeyInValue(CompilationInfo info, TypeMirror key, TypeMirror value) {
//        if (key.equals(value)) {
//            return true;
//        }
//
//        if (value.getKind() != TypeKind.DECLARED) {
//            return false;
//        }
//
//
//        DeclaredType dt = (DeclaredType) value;
//
//        for (VariableElement fields : ElementFilter.fieldsIn(info.getElements().getAllMembers((TypeElement) dt.asElement()))) {
//            if (detectUseOfKeyInValue(info, key, fields.asType())) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
////    public String getId() {
////        return SuspiciousWeakHashMap.class.getName();
////    }
////
////    public String getDisplayName() {
////        return "Suspicious WeakHashMap";
////    }
////
////    public void cancel() {}
//
//}
