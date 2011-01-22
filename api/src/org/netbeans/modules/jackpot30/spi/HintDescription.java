/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.Tree.Kind;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.Parameters;

/**
 *
 * @author Jan Lahoda
 */
public final class HintDescription {

    private final HintMetadata metadata;
    private final Kind triggerKind;
    private final PatternDescription triggerPattern;
    private final Worker worker;
    private final AdditionalQueryConstraints additionalConstraints;

    private HintDescription(HintMetadata metadata, Kind triggerKind, PatternDescription triggerPattern, Worker worker, AdditionalQueryConstraints additionalConstraints) {
        this.metadata = metadata;
        this.triggerKind = triggerKind;
        this.triggerPattern = triggerPattern;
        this.worker = worker;
        this.additionalConstraints = additionalConstraints;
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

    public HintMetadata getMetadata() {
        return metadata;
    }

    public Iterable<? extends String> getSuppressWarnings() {
        return metadata.suppressWarnings;
    }

    public AdditionalQueryConstraints getAdditionalConstraints() {
        return additionalConstraints;
    }

    static HintDescription create(HintMetadata metadata, PatternDescription triggerPattern, Worker worker, AdditionalQueryConstraints additionalConstraints) {
        return new HintDescription(metadata, null, triggerPattern, worker, additionalConstraints);
    }

    static HintDescription create(HintMetadata metadata, Kind triggerKind, Worker worker, AdditionalQueryConstraints additionalConstraints) {
        return new HintDescription(metadata, triggerKind, null, worker, additionalConstraints);
    }

    @Override
    public String toString() {
        return "[HintDescription:" + getTriggerPattern() + "/" + getTriggerKind() + "]";
    }

    public static final class PatternDescription {
        
        private final String pattern;
        private final Map<String, String> constraints;
        private final Iterable<? extends String> imports;

        private PatternDescription(String pattern, Map<String, String> constraints, String... imports) {
            this.pattern = pattern;
            this.constraints = constraints;
            this.imports = Arrays.asList(imports);
        }

        //for tests:
        public static PatternDescription create(String pattern) {
            return create(pattern, Collections.<String, String>emptyMap(), new String[0]);
        }

        public static PatternDescription create(String pattern, Map<String, String> constraints, String... imports) {
            Parameters.notNull("pattern", pattern);
            Parameters.notNull("constraints", constraints);
            Parameters.notNull("imports", imports);
            
            return new PatternDescription(pattern, constraints, imports);
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

        //XXX: should not be public:
        public Iterable<? extends String> getImports() {
            return imports;
        }

        @Override
        public String toString() {
            return pattern;
        }

    }

    public static final class MarkCondition {
        public final Value left;
        public final Operator op;
        public final Value right;

        public MarkCondition(Value left, Operator op, Value right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }

        @Override
        public String toString() {
            return left.toString() + op.name() + right.toString();
        }
    }

    public static class Value {}

    public static final class Selector extends Value {
        public final List<String> selected;

        public Selector(String... selected) {
            this(Arrays.asList(selected));
        }

        public Selector(List<String> selected) {
            this.selected = Collections.unmodifiableList(new LinkedList<String>(selected));
        }

        @Override
        public String toString() {
            return selected.toString();
        }
    }

    public static final class Literal extends Value {
        public Boolean value;

        public Literal(boolean value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    public static enum Operator {
        ASSIGN,
        EQUALS,
        NOT_EQUALS;
    }

    public static interface Worker {

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx);

    }

    public static interface Acceptor {
        public boolean accept(HintContext ctx);
    }

    //XXX: should be a method on the factory:
    //XXX: currently does not support ordering of custom conditions:
    public static final class MarksWorker implements Worker {

        public final List<MarkCondition> marks;
        public final Acceptor acceptor;
        public final List<DeclarativeFixDescription> fixes;

        public MarksWorker(List<MarkCondition> marks, Acceptor acceptor, List<DeclarativeFixDescription> fixes) {
            this.marks = marks;
            this.acceptor = acceptor;
            this.fixes = fixes;
        }

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    //XXX: should be a method on the factory:
    public static final class DeclarativeFixDescription {
        public final List<MarkCondition> marks;
        public final Acceptor acceptor;
        public final String fix;

        public DeclarativeFixDescription(List<MarkCondition> marks, Acceptor acceptor, String fix) {
            this.marks = marks;
            this.acceptor = acceptor;
            this.fix = fix;
        }

    }

    public static final class AdditionalQueryConstraints {
        public final Set<String> requiredErasedTypes;

        public AdditionalQueryConstraints(Set<String> requiredErasedTypes) {
            this.requiredErasedTypes = Collections.unmodifiableSet(new HashSet<String>(requiredErasedTypes));
        }

        private static final AdditionalQueryConstraints EMPTY = new AdditionalQueryConstraints(Collections.<String>emptySet());
        public static AdditionalQueryConstraints empty() {
            return EMPTY;
        }
    }
}
