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

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import javax.lang.model.element.Name;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class NFABasedBulkSearch extends BulkSearch {

    public NFABasedBulkSearch() {
        super(false);
    }

    @Override
    public Map<String, Collection<TreePath>> match(CompilationInfo info, TreePath tree, BulkPattern patternIn, Map<String, Long> timeLog) {
        BulkPatternImpl pattern = (BulkPatternImpl) patternIn;
        
        final Map<Res, Collection<TreePath>> occurringPatterns = new HashMap<Res, Collection<TreePath>>();
        final NFA<Input, Res> nfa = pattern.toNFA();
        final Set<String> identifiers = new HashSet<String>();

        new CollectIdentifiers<Void, TreePath>(identifiers) {
            private NFA.State active = nfa.getStartingState();
            @Override
            public Void scan(Tree node, TreePath p) {
                if (node == null) {
                    return null;
                }

                TreePath currentPath = new TreePath(p, node);
                boolean[] goDeeper = new boolean[1];
                final NFA.State newActiveAfterVariable = nfa.transition(active, new Input(Kind.IDENTIFIER, "$", false));
                Input normalizedInput = normalizeInput(node, goDeeper, null);
                boolean ignoreKind = normalizedInput.kind == Kind.IDENTIFIER || normalizedInput.kind == Kind.MEMBER_SELECT;

                NFA.State newActiveBefore = nfa.transition(active, normalizedInput);

                if (normalizedInput.name != null && !ignoreKind) {
                    newActiveBefore = nfa.join(newActiveBefore, nfa.transition(active, new Input(normalizedInput.kind, "$", false)));
                }

                active = newActiveBefore;

                if (goDeeper[0]) {
                    super.scan(node, currentPath);
                } else {
                    new CollectIdentifiers<Void, Void>(identifiers).scan(node, null);
                }

                NFA.State newActiveAfter = nfa.transition(active, UP);

                active = nfa.join(newActiveAfter, nfa.transition(newActiveAfterVariable, UP));

                for (Res r : nfa.getResults(active)) {
                    addOccurrence(r, currentPath);
                }

                return null;
            }

            private void addOccurrence(Res r, TreePath currentPath) {
                Collection<TreePath> occurrences = occurringPatterns.get(r);
                if (occurrences == null) {
                    occurringPatterns.put(r, occurrences = new LinkedList<TreePath>());
                }
                occurrences.add(currentPath);
            }
        }.scan(tree, null);

        Map<String, Collection<TreePath>> result = new HashMap<String, Collection<TreePath>>();

        for (Entry<Res, Collection<TreePath>> e : occurringPatterns.entrySet()) {
            if (!identifiers.containsAll(pattern.getIdentifiers().get(e.getKey().patternIndex))) {
                continue;
            }

            result.put(e.getKey().pattern, e.getValue());
        }

        return result;
    }

    @Override
    public BulkPattern create(Collection<? extends String> code, Collection<? extends Tree> patterns) {
        int startState = 0;
        final int[] nextState = new int[] {1};
        final Map<NFA.Key<Input>, NFA.State> transitionTable = new LinkedHashMap<NFA.Key<Input>, NFA.State>();
        Map<Integer, Res> finalStates = new HashMap<Integer, Res>();
        List<Set<? extends String>> identifiers = new LinkedList<Set<? extends String>>();
        List<List<List<String>>> requiredContent = new ArrayList<List<List<String>>>();
        Iterator<? extends String> codeIt = code.iterator();
        int patternIndex = 0;

        for (final Tree pattern : patterns) {
            final int[] currentState = new int[] {startState};
            final Set<String> patternIdentifiers = new HashSet<String>();
            final List<List<String>> content = new ArrayList<List<String>>();

            identifiers.add(patternIdentifiers);
            requiredContent.add(content);

            class Scanner extends CollectIdentifiers<Void, Void> {
                public Scanner() {
                    super(patternIdentifiers);
                }
                private boolean auxPath;
                private List<String> currentContent;
                {
                    content.add(currentContent = new ArrayList<String>());
                }
                @Override
                public Void scan(Tree t, Void v) {
                    if (t == null) {
                        return null;
                    }

                    if (Utilities.isMultistatementWildcardTree(t)) {
                        int target = nextState[0]++;

                        setBit(transitionTable, NFA.Key.create(currentState[0], new Input(Kind.IDENTIFIER, "$", false)), target);
                        setBit(transitionTable, NFA.Key.create(target, UP), currentState[0]);

                        content.add(currentContent = new ArrayList<String>());
                        
                        return null;
                    }

                    if (t.getKind() == Kind.BLOCK) {
                        StatementTree singletonStatement = null;
                        BlockTree bt = (BlockTree) t;

                        if (!bt.isStatic()) {
                            switch (bt.getStatements().size()) {
                                case 1:
                                    singletonStatement = bt.getStatements().get(0);
                                    break;
                                case 2:
                                    if (Utilities.isMultistatementWildcardTree(bt.getStatements().get(0))) {
                                        singletonStatement = bt.getStatements().get(1);
                                    } else {
                                        if (Utilities.isMultistatementWildcardTree(bt.getStatements().get(1))) {
                                            singletonStatement = bt.getStatements().get(0);
                                        }
                                    }
                                    break;
                                case 3:
                                    if (Utilities.isMultistatementWildcardTree(bt.getStatements().get(0)) && Utilities.isMultistatementWildcardTree(bt.getStatements().get(2))) {
                                        singletonStatement = bt.getStatements().get(1);
                                    }
                                    break;
                            }
                        }

                        if (singletonStatement != null) {
                            int backup = currentState[0];

                            boolean oldAuxPath = auxPath;

                            auxPath = true;

                            scan(singletonStatement, null);

                            auxPath = oldAuxPath;

                            int target = currentState[0];

                            setBit(transitionTable, NFA.Key.create(backup, new Input(Kind.BLOCK, null, false)), currentState[0] = nextState[0]++);

                            for (StatementTree st : bt.getStatements()) {
                                scan(st, null);
                            }

                            setBit(transitionTable, NFA.Key.create(currentState[0], UP), target);
                            currentState[0] = target;

                            return null;
                        }
                    }
                    
                    boolean[] goDeeper = new boolean[1];
                    Input[] bypass = new Input[1];
                    Input i = normalizeInput(t, goDeeper, bypass);

                    if (!TO_IGNORE.contains(i.kind) && !auxPath) {
                        currentContent.add(kind2EncodedString.get(i.kind));
                    }

                    if (i.name != null && !auxPath) {
                        if (!"$".equals(i.name) && !Utilities.isPureMemberSelect(t, false)) {
                            currentContent.add(i.name);
                        } else {
                            content.add(currentContent = new ArrayList<String>());
                        }
                    }

                    int backup = currentState[0];

                    handleTree(i, goDeeper, t, bypass);

                    boolean oldAuxPath = auxPath;

                    auxPath = true;

                    if (StatementTree.class.isAssignableFrom(t.getKind().asInterface()) && t != pattern) {
                        int target = currentState[0];

                        setBit(transitionTable, NFA.Key.create(backup, new Input(Kind.BLOCK, null, false)), currentState[0] = nextState[0]++);
                        handleTree(i, goDeeper, t, bypass);
                        setBit(transitionTable, NFA.Key.create(currentState[0], UP), target);
                        currentState[0] = target;
                    }

                    auxPath = oldAuxPath;

                    return null;
                }

                private void handleTree(Input i, boolean[] goDeeper, Tree t, Input[] bypass) {
                    int backup = currentState[0];
                    int target = nextState[0]++;

                    setBit(transitionTable, NFA.Key.create(backup, i), currentState[0] = nextState[0]++);

                    if (goDeeper[0]) {
                        super.scan(t, null);
                    } else {
                        new CollectIdentifiers<Void, Void>(patternIdentifiers).scan(t, null);
                        int aux = nextState[0]++;
                        setBit(transitionTable, NFA.Key.create(backup, new Input(Kind.MEMBER_SELECT, i.name, false)), aux);
                        setBit(transitionTable, NFA.Key.create(aux, new Input(Kind.IDENTIFIER, "$", false)), aux = nextState[0]++);
                        setBit(transitionTable, NFA.Key.create(aux, UP), aux = nextState[0]++);
                        setBit(transitionTable, NFA.Key.create(aux, UP), target);
                    }

                    setBit(transitionTable, NFA.Key.create(currentState[0], UP), target);
                    
                    if (bypass[0] != null) {
                        int intermediate = nextState[0]++;
                        
                        setBit(transitionTable, NFA.Key.create(backup, bypass[0]), intermediate);
                        setBit(transitionTable, NFA.Key.create(intermediate, UP), target);
                    }
                    
                    currentState[0] = target;
                }
            }

            Scanner s = new Scanner();

            s.scan(pattern, null);

            finalStates.put(currentState[0], new Res(codeIt.next(), patternIndex++));
        }

        NFA<Input, Res> nfa = NFA.<Input, Res>create(startState, nextState[0], null, transitionTable, finalStates);

        return new BulkPatternImpl(new LinkedList<String>(code), identifiers, requiredContent, nfa);
    }

    private static void setBit(Map<NFA.Key<Input>, NFA.State> transitionTable, NFA.Key<Input> input, int state) {
        NFA.State target = transitionTable.get(input);

        if (target == null) {
            transitionTable.put(input, target = NFA.State.create());
        }

        target.mutableOr(state);
    }

    private static Input normalizeInput(Tree t, boolean[] goDeeper, Input[] bypass) {
        if (t.getKind() == Kind.IDENTIFIER && ((IdentifierTree) t).getName().toString().startsWith("$")) {
            goDeeper[0] = false;
            return new Input(Kind.IDENTIFIER, "$", false);
        }

        if (Utilities.getWildcardTreeName(t) != null) {
            goDeeper[0] = false;
            return new Input(Kind.IDENTIFIER, "$", false);
        }
        
        if (t.getKind() == Kind.IDENTIFIER) {
            goDeeper[0] = false;
            String name = ((IdentifierTree) t).getName().toString();
            return new Input(Kind.IDENTIFIER, name, false);
        }

        if (t.getKind() == Kind.MEMBER_SELECT) {
            String name = ((MemberSelectTree) t).getIdentifier().toString();
            if (name.startsWith("$")) {
                goDeeper[0] = false;//???
                return new Input(Kind.IDENTIFIER, "$", false);
            }
            if (bypass != null && Utilities.isPureMemberSelect(t, true)) {
                bypass[0] = new Input(Kind.IDENTIFIER, name, false);
            }
            goDeeper[0] = true;
            return new Input(Kind.MEMBER_SELECT, name, false);
        }

        goDeeper[0] = true;

        String name;

        switch (t.getKind()) {
            case CLASS: name = ((ClassTree)t).getSimpleName().toString(); break;
            case VARIABLE: name = ((VariableTree)t).getName().toString(); break;
            case METHOD: name = ((MethodTree)t).getName().toString(); break;
            default: name = null;
        }

        if (name != null) {
            if (!name.isEmpty() && name.charAt(0) == '$') {
                name = "$";
            }
        }
        return new Input(t.getKind(), name, false);
    }

    @Override
    public boolean matches(CompilationInfo info, TreePath tree, BulkPattern pattern) {
        //XXX: performance
        return !match(info, tree, pattern).isEmpty();
    }

    private static final Set<Kind> TO_IGNORE = EnumSet.of(Kind.BLOCK, Kind.IDENTIFIER, Kind.MEMBER_SELECT);

    @Override
    public void encode(Tree tree, final EncodingContext ctx) {
        final Set<String> identifiers = new HashSet<String>();
        final List<String> content = new ArrayList<String>();
        if (!ctx.isForDuplicates()) {
            new CollectIdentifiers<Void, Void>(identifiers).scan(tree, null);
            try {
                int size = identifiers.size();
                ctx.getOut().write((size >> 24) & 0xFF);
                ctx.getOut().write((size >> 16) & 0xFF);
                ctx.getOut().write((size >>  8) & 0xFF);
                ctx.getOut().write((size >>  0) & 0xFF);
                for (String ident : identifiers) {
                    ctx.getOut().write(ident.getBytes("UTF-8"));//XXX: might probably contain ';'
                    ctx.getOut().write(';');
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        new CollectIdentifiers<Void, Void>(new HashSet<String>()) {
            @Override
            public Void scan(Tree t, Void v) {
                if (t == null) return null;

                if (t instanceof StatementTree && Utilities.isMultistatementWildcardTree((StatementTree) t)) {
                    return null;
                }

                boolean[] goDeeper = new boolean[1];

                Input i = normalizeInput(t, goDeeper, null);
                try {
                    ctx.getOut().write('(');
                    ctx.getOut().write(kind2Encoded.get(i.kind));
                    if (!TO_IGNORE.contains(i.kind)) {
                        content.add(kind2EncodedString.get(i.kind));
                    }
                    if (i.name != null) {
                        ctx.getOut().write('$');
                        ctx.getOut().write(i.name.getBytes("UTF-8"));
                        ctx.getOut().write(';');
                        content.add(i.name);
                    }

                    if (goDeeper[0]) {
                        super.scan(t, v);
                    }

                    ctx.getOut().write(')');
                } catch (IOException ex) {
                    //XXX
                    Exceptions.printStackTrace(ex);
                }

                return null;
            }
        }.scan(tree, null);

        ctx.setIdentifiers(identifiers);
        ctx.setContent(content);
    }

    @Override
    public boolean matches(InputStream encoded, BulkPattern patternIn) {
        try {
            return matchesImpl(encoded, patternIn);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
    }

    private boolean matchesImpl(InputStream encoded, BulkPattern patternIn) throws IOException {
        BulkPatternImpl pattern = (BulkPatternImpl) patternIn;
        final NFA<Input, Res> nfa = pattern.toNFA();
        Stack<NFA.State> skips = new Stack<NFA.State>();
        NFA.State active = nfa.getStartingState();
        int identSize = 0;

        identSize = encoded.read();
        identSize = (identSize << 8) + encoded.read();
        identSize = (identSize << 8) + encoded.read();
        identSize = (identSize << 8) + encoded.read();

        Set<String> identifiers = new HashSet<String>(2 * identSize);

        while (identSize-- > 0) {
            int read = encoded.read();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            //read name:
            while (read != ';') {
                baos.write(read);
                read = encoded.read();
            }

            identifiers.add(new String(baos.toByteArray(), "UTF-8"));
        }

        int read = encoded.read();
        
        while (read != (-1)) {
            if (read == '(') {
                read = encoded.read(); //kind

                Kind k = encoded2Kind.get((read << 8) + encoded.read());

                read = encoded.read();

                String name;

                if (read == '$') {
                    //XXX:
                    read = encoded.read();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    //read name:
                    while (read != ';') {
                        baos.write(read);
                        read = encoded.read();
                    }

                    read = encoded.read();
                    name = new String(baos.toByteArray(), "UTF-8");
                } else {
                    name = null;
                }
                
                final NFA.State newActiveAfterVariable = nfa.transition(active, new Input(Kind.IDENTIFIER, "$", false));
                Input normalizedInput = new Input(k, name, false);
                boolean ignoreKind = normalizedInput.kind == Kind.IDENTIFIER || normalizedInput.kind == Kind.MEMBER_SELECT;

                NFA.State newActive = nfa.transition(active, normalizedInput);

                if (normalizedInput.name != null && !ignoreKind) {
                    newActive = nfa.join(newActive, nfa.transition(active, new Input(k, "$", false)));
                }

                active = newActive;

                skips.push(newActiveAfterVariable);
            } else {
                NFA.State newActiveAfterVariable = skips.pop();
                NFA.State newActive = nfa.transition(active, UP);
                NFA.State s2 = nfa.transition(newActiveAfterVariable, UP);

                active = nfa.join(newActive, s2);
                
                for (Res res : nfa.getResults(active)) {
                    if (identifiers.containsAll(pattern.getIdentifiers().get(res.patternIndex))) {
                        return true;
                    }
                }

                read = encoded.read();
            }
        }

        return false;
    }

    private static final Map<Kind, byte[]> kind2Encoded;
    private static final Map<Kind, String> kind2EncodedString;
    private static final Map<Integer, Kind> encoded2Kind;

    static {
        kind2Encoded = new EnumMap<Kind, byte[]>(Kind.class);
        kind2EncodedString = new EnumMap<Kind, String>(Kind.class);
        encoded2Kind = new HashMap<Integer, Kind>();

        for (Kind k : Kind.values()) {
            String enc = Integer.toHexString(k.ordinal());

            if (enc.length() < 2) {
                enc = "0" + enc;
            }

            try {
                final byte[] bytes = enc.getBytes("UTF-8");

                assert bytes.length == 2;

                kind2Encoded.put(k, bytes);
                kind2EncodedString.put(k, enc);

                encoded2Kind.put((bytes[0] << 8) + bytes[1], k);
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public static class BulkPatternImpl extends BulkPattern {

        private final NFA<Input, Res> nfa;

        private BulkPatternImpl(List<? extends String> patterns, List<? extends Set<? extends String>> identifiers, List<List<List<String>>> requiredContent, NFA<Input, Res> nfa) {
            super(patterns, identifiers, requiredContent);
            this.nfa = nfa;
        }

        NFA<Input, Res> toNFA() {
            return nfa;
        }
        
        private static BulkPattern create(List<? extends String> patterns, List<? extends Set<? extends String>> identifiers, List<List<List<String>>> requiredContent, NFA<Input, Res> nfa) {
            return new BulkPatternImpl(patterns, identifiers, requiredContent, nfa);
        }

    }

    private static final class Res {
        private final String pattern;
        private final int patternIndex;

        public Res(String pattern, int patternIndex) {
            this.pattern = pattern;
            this.patternIndex = patternIndex;
        }

    }

    private static final class Input {
        private final Kind kind;
        private final String name;
        private final boolean end;

        private Input(Kind kind, String name, boolean end) {
            this.kind = kind;
            this.name = name;
            this.end = end;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Input other = (Input) obj;
            if (this.kind != other.kind) {
                return false;
            }
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            if (this.end != other.end) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + (this.kind != null ? this.kind.hashCode() : 17);
            hash = 47 * hash + (this.name != null ? this.name.hashCode() : 0);
            hash = 47 * hash + (this.end ? 1 : 0);
            return hash;
        }

        @Override
        public String toString() {
            return kind + ", " + name + ", " + end;
        }

    }

    private static final Input UP = new Input(null, null, true);

    private static boolean isIdentifierAcceptable(CharSequence content) {
        if (content.length() == 0) return false;
        if (content.charAt(0) == '$' || content.charAt(0) == '<') return false;
        String stringValue = content.toString();
        if (stringValue.contentEquals("java") || "lang".equals(stringValue)) return false;
        return true;
    }

    private static class CollectIdentifiers<R, P> extends TreeScanner<R, P> {

        private final Set<String> identifiers;

        public CollectIdentifiers(Set<String> identifiers) {
            this.identifiers = identifiers;
        }

        private void addIdentifier(Name ident) {
            if (!isIdentifierAcceptable(ident)) return;
            identifiers.add(ident.toString());
        }

        @Override
        public R visitMemberSelect(MemberSelectTree node, P p) {
            addIdentifier(node.getIdentifier());
            return super.visitMemberSelect(node, p);
        }

        @Override
        public R visitIdentifier(IdentifierTree node, P p) {
            addIdentifier(node.getName());
            return super.visitIdentifier(node, p);
        }

        @Override
        public R visitClass(ClassTree node, P p) {
            if (node.getSimpleName().length() == 0) {
                return scan(Utilities.filterHidden(null, node.getMembers()), p);
            }
            addIdentifier(node.getSimpleName());
            return super.visitClass(node, p);
        }

        @Override
        public R visitMethod(MethodTree node, P p) {
            addIdentifier(node.getName());
            return super.visitMethod(node, p);
        }

        @Override
        public R visitVariable(VariableTree node, P p) {
            addIdentifier(node.getName());
            return super.visitVariable(node, p);
        }

    }
}
