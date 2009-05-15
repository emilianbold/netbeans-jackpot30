package org.netbeans.modules.jackpot30.spi;

import java.util.Collection;
import org.netbeans.api.java.classpath.ClassPath;

/**
 * XXX: this is another ugly hack!
 * @author lahvac
 */
public interface ClassPathBasedHintProvider {

    public Collection<? extends HintDescription> computeHints(ClassPath cp);

}
