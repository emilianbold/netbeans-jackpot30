package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.Tree.Kind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.hints.HintsInvoker;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.spi.editor.hints.ErrorDescription;

/**
 *
 * @author lahvac
 */
public class HintsRunner {

    public static List<ErrorDescription> computeErrors(CompilationInfo info, List<HintDescription> hints, AtomicBoolean cancel/*XXX*/) {
        Map<Kind, List<HintDescription>> kindHints = new HashMap<Kind, List<HintDescription>>();
        Map<PatternDescription, List<HintDescription>> patternHints = new HashMap<PatternDescription, List<HintDescription>>();
        
        RulesManager.sortOut(hints, kindHints, patternHints);

        return new HintsInvoker().computeHints(info, kindHints, patternHints);
    }

}
