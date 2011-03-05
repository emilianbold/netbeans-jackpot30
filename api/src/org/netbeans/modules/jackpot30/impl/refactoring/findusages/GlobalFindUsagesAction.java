/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.impl.refactoring.findusages;

import com.sun.source.util.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.refactoring.spi.ui.UI;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Refactoring",
id = "org.netbeans.modules.jackpot30.impl.refactoring.findusages.GlobalFindUsagesAction")
@ActionRegistration(displayName = "#CTL_GlobalFindUsagesAction")
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 2225)
})
@Messages("CTL_GlobalFindUsagesAction=Global Find Usages")
public final class GlobalFindUsagesAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        JTextComponent focusedComponent = EditorRegistry.lastFocusedComponent();

        if (focusedComponent == null) {
            return;
        }

        final int pos = focusedComponent.getCaretPosition();
        JavaSource js = JavaSource.forDocument(focusedComponent.getDocument());

        if (js == null) {
            return ;
        }

        final String[] script = new String[1];
        
        try {
            js.runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController parameter) throws Exception {
                    TreePath sel = parameter.getTreeUtilities().pathFor(pos);
                    Element el = parameter.getTrees().getElement(sel);

                    if (el != null && el.getKind() == ElementKind.METHOD) {
                        script[0] = PatternGenerator.generateFindUsagesScript(parameter, el);
                    }
                }
            }, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        UI.openRefactoringUI(new GlobalFindUsagesRefactoringUI(new GlobalFindUsagesRefactoring(script[0])));
    }
}
