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

package org.netbeans.modules.jackpot30.transformers;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.api.java.source.WorkingCopy;
import org.openide.util.NbCollections;

/**
 *
 * @author Jan Lahoda
 */
public class Utilities {

    private Utilities() {}

    public static <E> Iterable<E> checkedIterableByFilter(final Iterable raw, final Class<E> type, final boolean strict) {
        return new Iterable<E>() {
            public Iterator<E> iterator() {
                return NbCollections.checkedIteratorByFilter(raw.iterator(), type, strict);
            }
        };
    }
    
//    public static AnnotationTree constructConstraint(WorkingCopy wc, String name, TypeMirror tm) {
//        TreeMaker make = wc.getTreeMaker();
//        ExpressionTree variable = prepareAssignment(make, "variable", make.Literal(name));
//        ExpressionTree type     = prepareAssignment(make, "type", make.MemberSelect((ExpressionTree) make.Type(wc.getTypes().erasure(tm)), "class"));
//        TypeElement constraint  = wc.getElements().getTypeElement(Annotations.CONSTRAINT.toFQN());
//
//        return make.Annotation(make.QualIdent(constraint), Arrays.asList(variable, type));
//    }

    public static ExpressionTree prepareAssignment(TreeMaker make, String name, ExpressionTree value) {
        return make.Assignment(make.Identifier(name), value);
    }

    public static ExpressionTree findValue(AnnotationTree m, String name) {
        for (ExpressionTree et : m.getArguments()) {
            if (et.getKind() == Kind.ASSIGNMENT) {
                AssignmentTree at = (AssignmentTree) et;
                String varName = ((IdentifierTree) at.getVariable()).getName().toString();

                if (varName.equals(name)) {
                    return at.getExpression();
                }
            }

            if (et instanceof LiteralTree/*XXX*/ && "value".equals(name)) {
                return et;
            }
        }

        return null;
    }

    public static List<AnnotationTree> findArrayValue(AnnotationTree at, String name) {
        ExpressionTree fixesArray = findValue(at, name);
        List<AnnotationTree> fixes = new LinkedList<AnnotationTree>();

        if (fixesArray != null && fixesArray.getKind() == Kind.NEW_ARRAY) {
            NewArrayTree trees = (NewArrayTree) fixesArray;

            for (ExpressionTree fix : trees.getInitializers()) {
                if (fix.getKind() == Kind.ANNOTATION) {
                    fixes.add((AnnotationTree) fix);
                }
            }
        }

        if (fixesArray != null && fixesArray.getKind() == Kind.ANNOTATION) {
            fixes.add((AnnotationTree) fixesArray);
        }
        
        return fixes;
    }

    public static boolean isPureMemberSelect(Tree mst, boolean allowVariables) {
        switch (mst.getKind()) {
            case IDENTIFIER: return allowVariables || ((IdentifierTree) mst).getName().charAt(0) != '$';
            case MEMBER_SELECT: return isPureMemberSelect(((MemberSelectTree) mst).getExpression(), allowVariables);
            default: return false;
        }
    }

}
