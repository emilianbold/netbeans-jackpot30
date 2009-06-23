package org.netbeans.modules.jackpot30.impl.refactoring;

import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.openide.util.Lookup;

public class FindDuplicatesRefactoring extends AbstractRefactoring {

    private HintDescription pattern;
    private Scope scope;
    private boolean verify;

    public FindDuplicatesRefactoring() {
        super(Lookup.EMPTY);
    }

    public synchronized HintDescription getPattern() {
        return pattern;
    }

    public synchronized void setPattern(HintDescription pattern) {
        this.pattern = pattern;
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
