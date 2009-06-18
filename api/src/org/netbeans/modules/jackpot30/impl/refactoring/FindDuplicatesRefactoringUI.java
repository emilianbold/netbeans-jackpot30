package org.netbeans.modules.jackpot30.impl.refactoring;

import java.awt.Component;
import java.util.Collections;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.util.HelpCtx;

public class FindDuplicatesRefactoringUI implements RefactoringUI {

    private volatile PatternDescription pattern;
    private volatile Scope scope;

    public FindDuplicatesRefactoringUI(PatternDescription pattern, Scope scope) {
        this.pattern = pattern;
        this.scope = scope;
    }

    public String getName() {
        return "Look for Duplicates";
    }

    public String getDescription() {
        return "Look for Duplicates";
    }

    public boolean isQuery() {
        return true;
    }

    private FindDuplicatesRefactoringPanel panel;

    public CustomRefactoringPanel getPanel(final ChangeListener parent) {
        return new CustomRefactoringPanel() {
            public void initialize() {}
            public Component getComponent() {
                if (panel == null) {
                    panel = new FindDuplicatesRefactoringPanel(parent);
                    panel.setPattern(pattern.getPattern());
                    panel.setScope(scope);
                }

                return panel;
            }
        };
    }

    public Problem setParameters() {
        pattern = PatternDescription.create(panel.getPattern(), Collections.<String, String>emptyMap());
        scope   = panel.getScope();
        return null;
    }

    public Problem checkParameters() {
        return null;
    }

    public boolean hasParameters() {
        return true;
    }

    public AbstractRefactoring getRefactoring() {
        FindDuplicatesRefactoring r = new FindDuplicatesRefactoring();

        r.setPattern(pattern);
        r.setScope(scope);

        return r;
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

}