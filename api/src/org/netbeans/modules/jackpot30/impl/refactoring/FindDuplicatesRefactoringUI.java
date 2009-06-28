package org.netbeans.modules.jackpot30.impl.refactoring;

import java.awt.Component;
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
import org.openide.util.HelpCtx;
import org.openide.util.Union2;

public class FindDuplicatesRefactoringUI implements RefactoringUI {

    private volatile @NonNull Union2<String, Iterable<? extends HintDescription>> pattern;
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
        
        this.pattern = pattern != null ? Union2.<String, Iterable<? extends HintDescription>>createFirst(pattern) : Union2.<String, Iterable<? extends HintDescription>>createSecond(Collections.<HintDescription>emptyList());
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
                    panel.setPattern(FindDuplicatesRefactoringUI.this.pattern);
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
        Union2<String, Iterable<? extends HintDescription>> pattern = panel != null ? panel.getPattern() : this.pattern;

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

        if (query) {
            FindDuplicatesRefactoring r = new FindDuplicatesRefactoring();
            r.setPattern(hints);
            r.setScope(scope);
            r.setVerify(verify);

            return r;
        }

        ApplyPatternRefactoring r = new ApplyPatternRefactoring();

        r.setPattern(hints);
        r.setScope(scope);

        return r;
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx("jackpot30.pattern.format");
    }

}