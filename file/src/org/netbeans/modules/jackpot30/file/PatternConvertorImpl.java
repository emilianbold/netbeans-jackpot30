package org.netbeans.modules.jackpot30.file;

import java.util.List;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.PatternConvertor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=PatternConvertor.class)
public class PatternConvertorImpl extends PatternConvertor {

    @Override
    protected HintDescription parseString(String code) {
        List<HintDescription> hints = DeclarativeHintRegistry.parseHints(code);

        if (hints.isEmpty()) {
            return null;
        }

        //XXX: what about hints.size() > 1?

        return hints.get(0);
    }

}
