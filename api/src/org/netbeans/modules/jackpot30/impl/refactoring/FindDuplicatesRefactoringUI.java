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

package org.netbeans.modules.jackpot30.impl.refactoring;

import java.awt.Component;
import java.io.File;
import java.util.Collections;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.PatternConvertor;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.Union2;

public class FindDuplicatesRefactoringUI implements RefactoringUI {

    private volatile @NonNull Union2<String, Iterable<? extends HintDescription>> pattern;
    private volatile @NonNull Scope scope;
    private volatile @NullAllowed String folder;
    private volatile boolean verify;

    private final boolean query;
    private final FindDuplicatesRefactoring refactoring;

    public FindDuplicatesRefactoringUI(@NullAllowed String pattern, Scope scope) {
        this(pattern, scope, false);
    }
    
    public FindDuplicatesRefactoringUI(@NullAllowed String pattern, Scope scope, boolean verify) {
        this(pattern, scope, verify, true);
    }

    public FindDuplicatesRefactoringUI(@NullAllowed String pattern, Scope scope, boolean verify, boolean query) {
        if (!query && !verify) {
            throw new UnsupportedOperationException();
        }
        
        this.pattern = pattern != null ? Union2.<String, Iterable<? extends HintDescription>>createFirst(pattern) : Union2.<String, Iterable<? extends HintDescription>>createSecond(Collections.<HintDescription>emptyList());
        this.scope = scope;
        this.verify = verify;
        this.query = query;
        this.refactoring = new FindDuplicatesRefactoring(query);
    }

    public String getName() {
        return query ? "Find Pattern Occurrences" : "Apply Pattern";
    }

    public String getDescription() {
        return query ? "Find Pattern Occurrences" : "Apply Pattern";
    }

    public boolean isQuery() {
        return query;
    }

    private FindDuplicatesRefactoringPanel panel;

    public CustomRefactoringPanel getPanel(final ChangeListener parent) {
        return new CustomRefactoringPanel() {
            public void initialize() {
                panel.initializeFoldersCombo();
            }
            public Component getComponent() {
                if (panel == null) {
                    panel = new FindDuplicatesRefactoringPanel(parent, query);
                    panel.setPattern(FindDuplicatesRefactoringUI.this.pattern);
                    panel.setScope(scope);
                    panel.setVerify(verify);
                    panel.setSelectedFolder(folder);
                }

                return panel;
            }
        };
    }

    public Problem setParameters() {
        pattern = panel.getPattern();
        scope   = panel.getScope();
        verify  = panel.getVerify();
        folder  = panel.getSelectedFolder();
        panel.saveFoldersCombo();
        return null;
    }

    public Problem checkParameters() {
        Union2<String, Iterable<? extends HintDescription>> pattern = panel != null ? panel.getPattern() : this.pattern;
        Scope scope = panel != null ? panel.getScope() : this.scope;
        String selectedFolder = panel != null ? panel.getSelectedFolder() : this.folder;

        if (pattern.hasFirst()) {
            if (pattern.first() == null) {
                return new Problem(true, "No pattern specified");
            }
            if (PatternConvertor.create(pattern.first()) == null) {
                return new Problem(true, "The pattern cannot be parsed");
            }
        } else {
            if (!pattern.second().iterator().hasNext()) {
                return new Problem(true, "No pattern specified");
            }
        }

        if (scope == Scope.GIVEN_FOLDER && findFolderFileObject(selectedFolder) == null) {
            return new Problem(true, "Specified folder not found");
        }
        return null;
    }

    public boolean hasParameters() {
        return true;
    }

    public AbstractRefactoring getRefactoring() {
        Iterable<? extends HintDescription> hints;
        
        if (pattern.hasFirst()) {
            if (pattern.first() != null) {
                hints = PatternConvertor.create(pattern.first());
            } else {
                hints = Collections.<HintDescription>emptyList();
            }
        } else {
            hints = pattern.second();
        }

        FindDuplicatesRefactoring r = refactoring;
        r.setPattern(hints);
        r.setScope(scope);
        r.setVerify(verify);
        r.setFolder(findFolderFileObject(folder));

        return r;
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx("jackpot30.pattern.format");
    }

    private FileObject findFolderFileObject(String folder) {
        if (folder == null) return null;

        File file = new File(folder);

        if (!file.exists()) return null;

        return FileUtil.toFileObject(FileUtil.normalizeFile(file));
    }

}