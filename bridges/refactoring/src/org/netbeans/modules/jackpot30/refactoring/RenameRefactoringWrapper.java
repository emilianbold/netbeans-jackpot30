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

import com.sun.source.tree.Tree.Kind;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RenameRefactoring;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

/**
 *
 * @author lahvac
 */
public class RenameRefactoringWrapper implements RefactoringUI {

    private final RefactoringUI delegate;
    private final RenameRefactoring refactoring;

    public RenameRefactoringWrapper(RefactoringUI delegate, RenameRefactoring refactoring) {
        this.delegate = delegate;
        this.refactoring = refactoring;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public boolean isQuery() {
        return delegate.isQuery();
    }

    private GenerateScriptPanel gsp;
    private CustomRefactoringPanel crp;

    @Override
    public CustomRefactoringPanel getPanel(ChangeListener parent) {
        if (crp == null) {
            Lookup source = refactoring.getRefactoringSource();
            TreePathHandle toRename = source.lookup(TreePathHandle.class);

            gsp = new GenerateScriptPanel(toRename != null && toRename.getKind() == Kind.METHOD);
            crp = new CustomRefactoringPanelImpl(gsp, delegate.getPanel(parent));
        }

        return crp;
    }

    @Override
    public Problem setParameters() {
        gsp.saveDefaults();
        refactoring.getContext().add(gsp.getData());
        return delegate.setParameters();
    }

    @Override
    public Problem checkParameters() {
        return delegate.checkParameters();
    }

    @Override
    public boolean hasParameters() {
        return true;
    }

    @Override
    public AbstractRefactoring getRefactoring() {
        return refactoring;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return delegate.getHelpCtx();
    }

    private static final class CustomRefactoringPanelImpl implements CustomRefactoringPanel {
        private final CustomRefactoringPanel delegate;
        private final JComponent component;
        public CustomRefactoringPanelImpl(final GenerateScriptPanel panel, CustomRefactoringPanel delegate) {
            this.delegate = delegate;
            final Component delegateComponent = delegate.getComponent();
            this.component = new JPanel(new BorderLayout()) {
                {
                    add(delegateComponent, BorderLayout.CENTER);
                    panel.setBorder(new EmptyBorder(0, 5, 0, 0));
                    add(panel, BorderLayout.SOUTH);
                    delegateComponent.requestFocus();
                }
                @Override
                public void requestFocus() {
                    delegateComponent.requestFocus();
                }
            };
        }

        @Override
        public void initialize() {
            delegate.initialize();
        }

        @Override
        public Component getComponent() {
            return component;
        }
        
    }

}
