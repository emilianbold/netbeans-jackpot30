package org.netbeans.modules.jackpot30.spi;

import java.util.Collection;
import java.util.Collections;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;
import org.netbeans.modules.jackpot30.spi.support.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.Lookup;

/**XXX: big hack?
 *
 * @author lahvac
 */
public abstract class PatternConvertor {

    protected abstract @CheckForNull Iterable<? extends HintDescription> parseString(@NonNull String code);

    public static @CheckForNull Iterable<? extends HintDescription> create(@NonNull String code) {
        //XXX:
        if (code.contains(";;")) {
            PatternConvertor c = Lookup.getDefault().lookup(PatternConvertor.class);

            if (c == null) {
                return null;
            }

            return c.parseString(code);
        }

        PatternDescription pd = PatternDescription.create(code, Collections.<String, String>emptyMap());

        HintDescription desc = HintDescriptionFactory.create()
                                                     .setDisplayName("Pattern Matches")
                                                     .setTriggerPattern(pd)
                                                     .setWorker(new WorkerImpl())
                                                     .produce();

        return Collections.singletonList(desc);
    }

    private static final class WorkerImpl implements Worker {

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
            ErrorDescription ed = ErrorDescriptionFactory.forTree(ctx, ctx.getPath(), "Found pattern occurrence");

            return Collections.singleton(ed);
        }
        
    }
}
