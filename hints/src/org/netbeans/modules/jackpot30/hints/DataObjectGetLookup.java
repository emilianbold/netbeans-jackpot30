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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;

/**
 *
 * @author Jan Lahoda
 */
public class DataObjectGetLookup extends AbstractHint {

    public DataObjectGetLookup() {
        super(true, true, HintSeverity.ERROR);
    }

    @Override
    public String getDescription() {
        return "DataObject.getLookup has to be overriden";
    }

    public Set<Kind> getTreeKinds() {
        return EnumSet.of(Kind.CLASS);
    }

    public List<ErrorDescription> run(CompilationInfo info, TreePath treePath) {
        TypeElement od = info.getElements().getTypeElement("org.openide.loaders.DataObject");

        if (od == null) {
            return null;
        }
        
        Element e = info.getTrees().getElement(treePath);

        if (e == null || e.getKind() != ElementKind.CLASS) {
            return null;
        }

        Types t = info.getTypes();

        if (!t.isSubtype(t.erasure(od.asType()), t.erasure(((TypeElement) e).asType()))) {
            return null;
        }

        for (Element m : info.getElements().getAllMembers(((TypeElement) e))) {
            if (m.getKind() == ElementKind.METHOD && "getLookup".contentEquals(m.getSimpleName())) {
                ExecutableElement ee = (ExecutableElement) m;

                if (ee.getParameters().size() == 0 && od.equals(info.getElementUtilities().enclosingTypeElement(ee))) {
                    int[] span = info.getTreeUtilities().findNameSpan((ClassTree) treePath.getLeaf());

                    if (span == null) {
                        return null;
                    }
                    
                    ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(getSeverity().toEditorSeverity(),
                                                                                         "DataObjects subclasses have to override getLookup",
                                                                                         info.getFileObject(),
                                                                                         span[0],
                                                                                         span[1]);
                    return Collections.singletonList(ed);
                }
            }
        }

        return null;
    }

    public String getId() {
        return DataObjectGetLookup.class.getName();
    }

    public String getDisplayName() {
        return "DataObject.getLookup has to be overriden";
    }

    public void cancel() {}

}
