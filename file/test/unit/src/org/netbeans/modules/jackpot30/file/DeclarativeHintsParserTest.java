package org.netbeans.modules.jackpot30.file;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;
import org.netbeans.api.lexer.TokenHierarchy;
import static org.junit.Assert.*;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.Result;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintsParserTest {

    @Test
    public void testSimpleParse() {
        performTest(" 1 + 1 :: $1 instanceof something && $test instanceof somethingelse => 1 + 1;; ",
                    StringHintDescription.create(" 1 + 1 ")
                                         .addConstraint("$1", " something ")
                                         .addConstraint("$test", " somethingelse ")
                                         .addTos(" 1 + 1"));
    }

    @Test
    public void testParseDisplayName() {
        performTest("'test': 1 + 1 :: $1 instanceof something && $test instanceof somethingelse => 1 + 1;; ",
                    StringHintDescription.create(" 1 + 1 ")
                                         .addConstraint("$1", " something ")
                                         .addConstraint("$test", " somethingelse ")
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test"));
    }

    @Test
    public void testMultiple() {
        performTest("'test': 1 + 1 => 1 + 1;; 'test2': 1 + 1 => 1 + 1;;",
                    StringHintDescription.create(" 1 + 1 ")
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test"),
                    StringHintDescription.create(" 1 + 1 ")
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test2"));
    }

    private void performTest(String code, StringHintDescription... golden) {
        TokenHierarchy<?> h = TokenHierarchy.create(code, DeclarativeHintTokenId.language());
        Result parsed = new DeclarativeHintsParser().parse(h.tokenSequence(DeclarativeHintTokenId.language()));
        List<StringHintDescription> real = new LinkedList<StringHintDescription>();

        for (HintTextDescription hint : parsed.hints) {
            real.add(StringHintDescription.create(code, hint));
        }

        assertEquals(Arrays.asList(golden), real);
    }

    private static final class StringHintDescription {
        private String displayName;
        private final String text;
        private final Map<String, String> constraints;
        private final List<String> to;

        private StringHintDescription(String text) {
            this.text = text;
            this.constraints = new HashMap<String, String>();
            this.to = new LinkedList<String>();
        }

        public static StringHintDescription create(String text) {
            return new StringHintDescription(text);
        }

        public static StringHintDescription create(String code, HintTextDescription desc) {
            StringHintDescription r = StringHintDescription.create(code.substring(desc.textStart, desc.textEnd));

            for (Entry<String, int[]> e : desc.variables2Constraints.entrySet()) {
                r = r.addConstraint(e.getKey(), code.substring(e.getValue()[0], e.getValue()[1]));
            }

            for (int[] range : desc.fixes) {
                r = r.addTos(code.substring(range[0], range[1]));
            }

            return r.setDisplayName(desc.displayName);
        }

        public StringHintDescription addConstraint(String var, String constraint) {
            constraints.put(var, constraint);
            return this;
        }

        public StringHintDescription addTos(String... to) {
            this.to.addAll(Arrays.asList(to));
            return this;
        }

        public StringHintDescription setDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final StringHintDescription other = (StringHintDescription) obj;
            if ((this.displayName == null) ? (other.displayName != null) : !this.displayName.equals(other.displayName)) {
                return false;
            }
            if ((this.text == null) ? (other.text != null) : !this.text.equals(other.text)) {
                return false;
            }
            if (this.constraints != other.constraints && (this.constraints == null || !this.constraints.equals(other.constraints))) {
                return false;
            }
            if (this.to != other.to && (this.to == null || !this.to.equals(other.to))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + (this.displayName != null ? this.displayName.hashCode() : 0);
            hash = 43 * hash + (this.text != null ? this.text.hashCode() : 0);
            hash = 43 * hash + (this.constraints != null ? this.constraints.hashCode() : 0);
            hash = 43 * hash + (this.to != null ? this.to.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return "<" + String.valueOf(displayName) + ":" + text + ":" + constraints + ":" + to + ">";
        }

    }

}