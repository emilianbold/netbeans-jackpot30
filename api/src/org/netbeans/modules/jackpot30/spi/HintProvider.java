package org.netbeans.modules.jackpot30.spi;

import java.util.Collection;

public interface HintProvider {

    public Collection<? extends HintDescription> computeHints();

}
