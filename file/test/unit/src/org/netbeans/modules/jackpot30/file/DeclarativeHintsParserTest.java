package org.netbeans.modules.jackpot30.file;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.jackpot30.file.Condition.Instanceof;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation.ParameterKind;
import static org.junit.Assert.*;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.Result;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintsParserTest extends NbTestCase {

    public DeclarativeHintsParserTest(String name) {
        super(name);
    }

    public void testSimpleParse() {
        performTest(" 1 + 1 :: $1 instanceof something && $test instanceof somethingelse => 1 + 1;; ",
                    StringHintDescription.create(" 1 + 1 ")
                                         .addCondition(new Instanceof(false, "$1", " something ", new int[2]))
                                         .addCondition(new Instanceof(false, "$test", " somethingelse ", new int[2]))
                                         .addTos(" 1 + 1"));
    }

    public void testParseDisplayName() {
        performTest("'test': 1 + 1 :: $1 instanceof something && $test instanceof somethingelse => 1 + 1;; ",
                    StringHintDescription.create(" 1 + 1 ")
                                         .addCondition(new Instanceof(false, "$1", " something ", new int[2]))
                                         .addCondition(new Instanceof(false, "$test", " somethingelse ", new int[2]))
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test"));
    }

    public void testMultiple() {
        performTest("'test': 1 + 1 => 1 + 1;; 'test2': 1 + 1 => 1 + 1;;",
                    StringHintDescription.create(" 1 + 1 ")
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test"),
                    StringHintDescription.create(" 1 + 1 ")
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test2"));
    }

    public void testMethodInvocationCondition1() {
        Map<String, ParameterKind> m = new LinkedHashMap<String, ParameterKind>();

        m.put("a", ParameterKind.STRING_LITERAL);
        m.put("$2", ParameterKind.VARIABLE);
        m.put("$1", ParameterKind.VARIABLE);

        performTest("'test': $1 + $2 :: test(\"a\", $2, $1) => 1 + 1;;",
                    StringHintDescription.create("$1 + $2 ")
                                         .addCondition(new MethodInvocation(false, "test", m, null))
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test"));
    }

    public void testMethodInvocationCondition2() {
        Map<String, ParameterKind> m = new LinkedHashMap<String, ParameterKind>();

        m.put("$1", ParameterKind.VARIABLE);
        m.put("javax.lang.model.element.Modifier.VOLATILE", ParameterKind.ENUM_CONSTANT);
        m.put("javax.lang.model.SourceVersion.RELEASE_6", ParameterKind.ENUM_CONSTANT);

        performTest("'test': $1 + $2 :: test($1, Modifier.VOLATILE, SourceVersion.RELEASE_6) => 1 + 1;;",
                    StringHintDescription.create("$1 + $2 ")
                                         .addCondition(new MethodInvocation(false, "test", m, null))
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test"));
    }

    public void testNegation() {
        Map<String, ParameterKind> m = new LinkedHashMap<String, ParameterKind>();

        m.put("$1", ParameterKind.VARIABLE);
        m.put("javax.lang.model.element.Modifier.VOLATILE", ParameterKind.ENUM_CONSTANT);
        m.put("javax.lang.model.SourceVersion.RELEASE_6", ParameterKind.ENUM_CONSTANT);

        performTest("'test': $1 + $2 :: !test($1, Modifier.VOLATILE, SourceVersion.RELEASE_6) => 1 + 1;;",
                    StringHintDescription.create("$1 + $2 ")
                                         .addCondition(new MethodInvocation(true, "test", m, null))
                                         .addTos(" 1 + 1")
                                         .setDisplayName("test"));
    }

    public void testComments1() {
        performTest("/**/'test': /**/1 /**/+ 1//\n =>/**/ 1 + 1/**/;; //\n'test2': /**/1 + 1 =>//\n 1/**/ + 1;;",
                    StringHintDescription.create("1 /**/+ 1//\n ")
                                         .addTos(" 1 + 1/**/")
                                         .setDisplayName("test"),
                    StringHintDescription.create("1 + 1 ")
                                         .addTos(" 1/**/ + 1")
                                         .setDisplayName("test2"));
    }

    public void testParserSanity1() {
        String code = "'Use of assert'://\n" +
                      "   assert /**/ $1 : $2; :: //\n $1 instanceof boolean && $2 instanceof java.lang.Object\n" +
                      "=> if (!$1) throw new /**/ IllegalStateException($2);\n" +
                      ";;//\n";
        performParserSanityTest(code);
    }

    public void testJavaBlocks() {
        performTest("<?import java.util.List;?> /**/'test': List $l; :: test($_)\n => List $l; ;; <?private boolean test(Variable v) {return false;}?>",
                    "import java.util.List;",
                    Arrays.asList(StringHintDescription.create(" List $l; ")
                                                       .addTos(" List $l; ")
                                                       .setDisplayName("test")
                                                       .addCondition(new MethodInvocation(false, "test", Collections.singletonMap("$_", ParameterKind.VARIABLE), null))),
                    Arrays.asList("private boolean test(Variable v) {return false;}"));
    }

    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[0], new Object[0]);
        
        FileObject dir = SourceUtilsTestUtil.makeScratchDir(this);
        
        System.setProperty("netbeans.user", FileUtil.toFile(dir).getAbsolutePath());
    }
    
    private void performTest(String code, StringHintDescription... golden) {
        performTest(code, null, Arrays.asList(golden), Collections.<String>emptyList());
    }

    private void performTest(String code, String goldenImportsBlock, Collection<StringHintDescription> goldenHints, Collection<String> goldenBlocks) {
        TokenHierarchy<?> h = TokenHierarchy.create(code, DeclarativeHintTokenId.language());
        Result parsed = new DeclarativeHintsParser().parse(code, h.tokenSequence(DeclarativeHintTokenId.language()));
        List<StringHintDescription> real = new LinkedList<StringHintDescription>();

        for (HintTextDescription hint : parsed.hints) {
            real.add(StringHintDescription.create(code, hint));
        }

        if (goldenImportsBlock != null) {
            assertNotNull(parsed.importsBlock);
            assertEquals(goldenImportsBlock, code.substring(parsed.importsBlock[0], parsed.importsBlock[1]));
        } else {
            assertNull(parsed.importsBlock);
        }
        
        assertEquals(goldenHints, real);
        if (goldenBlocks != null) {
            assertNotNull(parsed.blocks);

            List<String> realBlocks = new LinkedList<String>();
            
            for (int[] span : parsed.blocks) {
                realBlocks.add(code.substring(span[0], span[1]));
            }
        } else {
            assertNull(parsed.blocks);
        }
    }

    private void performParserSanityTest(String code) {
        for (int cntr = 0; cntr < code.length(); cntr++) {
            String currentpath = code.substring(0, cntr);

            System.err.println("currentpath=" + currentpath);
            
            TokenHierarchy<?> h = TokenHierarchy.create(currentpath, DeclarativeHintTokenId.language());

            new DeclarativeHintsParser().parse(currentpath, h.tokenSequence(DeclarativeHintTokenId.language()));
        }
    }

    private static final class StringHintDescription {
        private String displayName;
        private final String text;
        private final List<String> conditions;
        private final List<String> to;

        private StringHintDescription(String text) {
            this.text = text;
            this.conditions = new LinkedList<String>();
            this.to = new LinkedList<String>();
        }

        public static StringHintDescription create(String text) {
            return new StringHintDescription(text);
        }

        public static StringHintDescription create(String code, HintTextDescription desc) {
            StringHintDescription r = StringHintDescription.create(code.substring(desc.textStart, desc.textEnd));

            for (Condition c : desc.conditions) {
                r = r.addCondition(c);
            }

            for (int[] range : desc.fixes) {
                r = r.addTos(code.substring(range[0], range[1]));
            }

            return r.setDisplayName(desc.displayName);
        }

        public StringHintDescription addCondition(Condition c) {
            conditions.add(c.toString());
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
            if (this.conditions != other.conditions && (this.conditions == null || !this.conditions.equals(other.conditions))) {
                return false;
            }
            if (this.to != other.to && (this.to == null || !this.to.equals(other.to))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + (this.displayName != null ? this.displayName.hashCode() : 0);
            hash = 89 * hash + (this.text != null ? this.text.hashCode() : 0);
            hash = 89 * hash + (this.conditions != null ? this.conditions.hashCode() : 0);
            hash = 89 * hash + (this.to != null ? this.to.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return "<" + String.valueOf(displayName) + ":" + text + ":" + conditions + ":" + to + ">";
        }

    }

}