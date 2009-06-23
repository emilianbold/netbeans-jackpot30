package org.netbeans.modules.jackpot30.impl.refactoring;

import java.awt.Component;
import javax.swing.event.ChangeListener;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.PatternConvertor;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.ui.CustomRefactoringPanel;
import org.netbeans.modules.refactoring.spi.ui.RefactoringUI;
import org.openide.util.HelpCtx;

public class FindDuplicatesRefactoringUI implements RefactoringUI {

    private volatile @NullAllowed String pattern;
    private volatile @NonNull Scope scope;
    private volatile boolean verify;

    private final boolean query;

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
        
        this.pattern = pattern;
        this.scope = scope;
        this.verify = verify;
        this.query = query;
    }

    public String getName() {
        return query ? "Look for Duplicates" : "Apply Pattern";
    }

    public String getDescription() {
        return query ? "Look for Duplicates" : "Apply Pattern";
    }

    public boolean isQuery() {
        return query;
    }

    private FindDuplicatesRefactoringPanel panel;

    public CustomRefactoringPanel getPanel(final ChangeListener parent) {
        return new CustomRefactoringPanel() {
            public void initialize() {}
            public Component getComponent() {
                if (panel == null) {
                    panel = new FindDuplicatesRefactoringPanel(parent, query);
                    String pattern = FindDuplicatesRefactoringUI.this.pattern;
                    panel.setPattern(pattern != null ? pattern : "");
                    panel.setScope(scope);
                    panel.setVerify(verify);
                }

                return panel;
            }
        };
    }

    public Problem setParameters() {
        pattern = panel.getPattern();
        scope   = panel.getScope();
        verify  = panel.getVerify();
        return null;
    }

    public Problem checkParameters() {
        String pattern = panel != null ? panel.getPattern() : this.pattern;
        if (pattern == null) {
            return new Problem(true, "No pattern specified");
        }
        if (PatternConvertor.create(pattern) == null) {
            return new Problem(true, "The pattern cannot be parsed");
        }
        //TODO
        return null;
    }

    public boolean hasParameters() {
        return true;
    }

    public AbstractRefactoring getRefactoring() {
        if (query) {
            FindDuplicatesRefactoring r = new FindDuplicatesRefactoring();

            r.setPattern(pattern != null ? PatternConvertor.create(pattern) : null/*???*/);
            r.setScope(scope);
            r.setVerify(verify);

            return r;
        }

        ApplyPatternRefactoring r = new ApplyPatternRefactoring();

        r.setPattern(pattern != null ? PatternConvertor.create(pattern) : null/*???*/);
        r.setScope(scope);

        return r;
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx("jackpot30.pattern.format");
    }

}