package org.netbeans.modules.jackpot30.impl.refactoring;

import javax.swing.event.ChangeListener;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.util.HelpCtx;

public class FindDuplicatesRefactoringUI implements RefactoringUI {

    private final PatternDescription pattern;
    private final Scope scope;

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

    public CustomRefactoringPanel getPanel(ChangeListener parent) {
        return null;
    }

    public Problem setParameters() {
        return null;
    }

    public Problem checkParameters() {
        return null;
    }

    public boolean hasParameters() {
        return false;
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