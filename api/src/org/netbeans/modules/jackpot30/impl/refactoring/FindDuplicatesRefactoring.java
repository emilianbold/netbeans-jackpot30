package org.netbeans.modules.jackpot30.impl.refactoring;

import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.util.Lookup;

public class FindDuplicatesRefactoring extends AbstractRefactoring {

    private Iterable<? extends HintDescription> patterns;
    private Scope scope;
    private boolean verify;

    public FindDuplicatesRefactoring() {
        super(Lookup.EMPTY);
    }

    public synchronized Iterable<? extends HintDescription> getPattern() {
        return patterns;
    }

    public synchronized void setPattern(Iterable<? extends HintDescription> patterns) {
        this.patterns = patterns;
    }

    public synchronized Scope getScope() {
        return scope;
    }

    public synchronized void setScope(Scope scope) {
        this.scope = scope;
    }

    public boolean isVerify() {
        return verify;
    }

    public void setVerify(boolean verify) {
        this.verify = verify;
    }

}
