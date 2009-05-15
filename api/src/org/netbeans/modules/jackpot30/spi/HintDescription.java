package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.Tree.Kind;
import java.util.Collection;
import java.util.Map;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.Parameters;

/**
 *
 * @author Jan Lahoda
 */
public final class HintDescription {

    private final String displayName;
    private final Kind triggerKind;
    private final PatternDescription triggerPattern;
    private final Worker worker;

    private HintDescription(String displayName, Kind triggerKind, PatternDescription triggerPattern, Worker worker) {
        this.displayName = displayName;
        this.triggerKind = triggerKind;
        this.triggerPattern = triggerPattern;
        this.worker = worker;
    }

    //XXX: should not be public
    public Kind getTriggerKind() {
        return triggerKind;
    }

    //XXX: should not be public
    public PatternDescription getTriggerPattern() {
        return triggerPattern;
    }

    //XXX: should not be public
    public Worker getWorker() {
        return worker;
    }

    //XXX: should not be public
    public String getDisplayName() {
        return displayName;
    }

    static HintDescription create(String displayName, PatternDescription triggerPattern, Worker worker) {
        return new HintDescription(displayName, null, triggerPattern, worker);
    }

    static HintDescription create(String displayName, Kind triggerKind, Worker worker) {
        return new HintDescription(displayName, triggerKind, null, worker);
    }
    
    public static final class PatternDescription {
        
        private final String pattern;
        private final Map<String, String> constraints;

        private PatternDescription(String pattern, Map<String, String> constraints) {
            this.pattern = pattern;
            this.constraints = constraints;
        }

        public static PatternDescription create(String pattern, Map<String, String> constraints) {
            Parameters.notNull("pattern", pattern);
            Parameters.notNull("constraints", constraints);
            
            return new PatternDescription(pattern, constraints);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PatternDescription other = (PatternDescription) obj;
            if ((this.pattern == null) ? (other.pattern != null) : !this.pattern.equals(other.pattern)) {
                return false;
            }
            if (this.constraints != other.constraints && (this.constraints == null || !this.constraints.equals(other.constraints))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + (this.pattern != null ? this.pattern.hashCode() : 0);
            hash = 71 * hash + (this.constraints != null ? this.constraints.hashCode() : 0);
            return hash;
        }

        //XXX: should not be public:
        public String getPattern() {
            return pattern;
        }

        //XXX: should not be public:
        public Map<String, String> getConstraints() {
            return constraints;
        }
    }

    public static interface Worker {

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx);

    }

}
