package org.netbeans.modules.jackpot30.impl.refactoring;

import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.util.Lookup;

public class FindDuplicatesRefactoring extends AbstractRefactoring {

    private PatternDescription pattern;
    private Scope scope;

    public FindDuplicatesRefactoring() {
        super(Lookup.EMPTY);
    }

    public synchronized PatternDescription getPattern() {
        return pattern;
    }

    public synchronized void setPattern(PatternDescription pattern) {
        this.pattern = pattern;
    }

    public synchronized Scope getScope() {
        return scope;
    }

    public synchronized void setScope(Scope scope) {
        this.scope = scope;
    }

}