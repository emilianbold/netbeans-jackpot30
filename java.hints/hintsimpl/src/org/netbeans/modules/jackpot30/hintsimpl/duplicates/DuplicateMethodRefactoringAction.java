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
package org.netbeans.modules.jackpot30.hintsimpl.duplicates;

import com.sun.source.tree.Tree.Kind;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.ui.ScanDialog;
import org.netbeans.modules.refactoring.java.ui.ContextAnalyzer;
import org.netbeans.modules.refactoring.java.ui.JavaRefactoringUIFactory;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

@ActionID(
        category = "Refactoring",
        id = "org.netbeans.modules.jackpot30.hintsimpl.duplicates.DuplicateMethodRefactoringAction")
@ActionRegistration(
        displayName = "#CTL_DuplicateMethodRefactoringAction")
@ActionReference(path = "Menu/Refactoring", position = 1120)
@Messages("CTL_DuplicateMethodRefactoringAction=Find and Replace Duplicates...")
public final class DuplicateMethodRefactoringAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        Runnable task = ContextAnalyzer.createTask(Utilities.actionsGlobalContext()/*!!!*/, new JavaRefactoringUIFactory() {
            @Override public RefactoringUI create(CompilationInfo info, TreePathHandle[] handles, FileObject[] files, NonRecursiveFolder[] packages) {
                //todo: more precise verification of refactoring feasibility:
                return handles.length == 1 && handles[0].getKind() == Kind.METHOD ? new DuplicateMethodRefactoringUI(handles[0]) : null;
            }
        });
        ScanDialog.runWhenScanFinished(task, "Find&Replace Duplicates");
    }

    public boolean isEnabled() {
        return ContextAnalyzer.canRefactorSingle(Utilities.actionsGlobalContext()/*!!!*/, true, true);
    }
}
