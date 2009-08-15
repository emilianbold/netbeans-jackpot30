package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.Tree.Kind;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;

/**
 *
 * @author lahvac
 */
public class HintDescriptionFactory {

    private       String displayName;
    private       Kind triggerKind;
    private       PatternDescription triggerPattern;
    private       Worker worker;
    private final List<String> suppressWarningsKeys = new LinkedList<String>();
    private       boolean finished;

    private HintDescriptionFactory() {
    }

    public static HintDescriptionFactory create() {
        return new HintDescriptionFactory();
    }

    public HintDescriptionFactory setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public HintDescriptionFactory setTriggerKind(Kind triggerKind) {
        if (this.triggerPattern != null) {
            throw new IllegalStateException(this.triggerPattern.getPattern());
        }

        this.triggerKind = triggerKind;
        return this;
    }

    public HintDescriptionFactory setTriggerPattern(PatternDescription triggerPattern) {
        if (this.triggerKind != null) {
            throw new IllegalStateException(this.triggerKind.name());
        }
        
        this.triggerPattern = triggerPattern;
        return this;
    }

    public HintDescriptionFactory setWorker(Worker worker) {
        this.worker = worker;
        return this;
    }

    public HintDescriptionFactory addSuppressWarningsKeys(String... keys) {
        this.suppressWarningsKeys.addAll(Arrays.asList(keys));
        return this;
    }

    public HintDescription produce() {
        if (this.triggerKind == null) {
            return HintDescription.create(displayName, triggerPattern, worker, Collections.unmodifiableList(suppressWarningsKeys));
        } else {
            return HintDescription.create(displayName, triggerKind, worker, Collections.unmodifiableList(suppressWarningsKeys));
        }
    }
    
}
