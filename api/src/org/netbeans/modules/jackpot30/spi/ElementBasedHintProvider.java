package org.netbeans.modules.jackpot30.spi;

import java.util.Collection;
import javax.lang.model.element.Element;
import org.netbeans.api.java.source.CompilationInfo;

/**
 * XXX: this is an ugly hack!
 * @author lahvac
 */
public interface ElementBasedHintProvider {

    public Collection<? extends HintDescription> computeHints(CompilationInfo info, Element el);

}
