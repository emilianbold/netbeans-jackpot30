/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.modules.jackpot30.java.hints;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;
import org.netbeans.modules.jackpot30.spi.HintDescriptionFactory;
import org.netbeans.modules.jackpot30.spi.HintMetadata;
import org.netbeans.modules.jackpot30.spi.HintMetadata.HintSeverity;
import org.netbeans.modules.jackpot30.spi.HintMetadata.Kind;
import org.netbeans.modules.jackpot30.spi.HintProvider;
import org.netbeans.modules.java.hints.jackpot.impl.RulesManager;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=HintProvider.class)
public class JavaHintsHintProvider implements HintProvider {

    public Map<HintMetadata, Collection<? extends HintDescription>> computeHints() {
        Map<HintMetadata, Collection<? extends HintDescription>> result = new HashMap<HintMetadata, Collection<? extends HintDescription>>();

        for (Entry<org.netbeans.modules.java.hints.jackpot.spi.HintMetadata, Collection<? extends org.netbeans.modules.java.hints.jackpot.spi.HintDescription>> e : RulesManager.getInstance().allHints.entrySet()) {
            HintMetadata hm = convert(e.getKey());
            List<HintDescription> hints = new LinkedList<HintDescription>();
            
            for (org.netbeans.modules.java.hints.jackpot.spi.HintDescription hd : e.getValue()) {
                Worker w = new WorkerImpl(hd.getWorker(), hd.getMetadata());
                HintDescriptionFactory fact = HintDescriptionFactory.create()
                                                                   .setMetadata(hm)
                                                                   .setWorker(w);
                if (hd.getTriggerPattern() != null) {
                    List<String> imports = new LinkedList<String>();
                    for (String imp : hd.getTriggerPattern().getImports()) {
                        imports.add(imp);
                    }
                    HintDescription.PatternDescription pd = HintDescription.PatternDescription.create(hd.getTriggerPattern().getPattern(), hd.getTriggerPattern().getConstraints(), imports.toArray(new String[0]));
                    fact = fact.setTriggerPattern(pd);
                } else {
                    fact = fact.setTriggerKind(hd.getTriggerKind());
                }

                hints.add(fact.produce());
            }

            result.put(hm, hints);
        }

        return result;
    }

    private static HintMetadata convert(org.netbeans.modules.java.hints.jackpot.spi.HintMetadata hm) {
        return HintMetadata.create(hm.id, hm.displayName, hm.description, hm.category, hm.enabled, convert(hm.kind), convert(hm.severity), /*XXX: customizer*/null, hm.suppressWarnings);
    }

    private static Kind convert(org.netbeans.modules.java.hints.jackpot.spi.HintMetadata.Kind kind) {
        return kind != null ? Kind.valueOf(kind.name()) : null;
    }

    private static HintSeverity convert(org.netbeans.modules.java.hints.spi.AbstractHint.HintSeverity sev) {
        return sev != null ? HintSeverity.valueOf(sev.name()) : null;
    }

    private static class WorkerImpl implements Worker {
        private final org.netbeans.modules.java.hints.jackpot.spi.HintDescription.Worker worker;
        private final org.netbeans.modules.java.hints.jackpot.spi.HintMetadata hm;

        public WorkerImpl(org.netbeans.modules.java.hints.jackpot.spi.HintDescription.Worker worker, org.netbeans.modules.java.hints.jackpot.spi.HintMetadata hm) {
            this.worker = worker;
            this.hm = hm;
        }

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
            org.netbeans.modules.java.hints.jackpot.spi.HintContext newCTX = new org.netbeans.modules.java.hints.jackpot.spi.HintContext(ctx.getInfo(), hm, ctx.getPath(), ctx.getVariables(), ctx.getMultiVariables(), ctx.getVariableNames());
            return worker.createErrors(newCTX);
        }
    }

}
