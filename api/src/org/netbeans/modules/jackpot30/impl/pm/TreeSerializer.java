/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.modules.jackpot30.impl.Utilities;

/**
 *
 * @author Jan Lahoda
 */
public class TreeSerializer extends TreeScanner<Void, Appendable> {

    public static Result serializeText(Tree tree) {
        StringBuilder p = new StringBuilder();
        TreeSerializer serializer = new TreeSerializer(false);

        serializer.scan(tree, p);

        return new Result(p.toString(), serializer.serializedStart2Tree, null, null, serializer.identifiers, serializer.treeKinds);
    }

    public static Result serializePatterns(Tree... patterns) {
        StringBuilder p = new StringBuilder();
        TreeSerializer ts = new TreeSerializer(true);
        int[] groups = new int[patterns.length];
        Set<Integer> patternsWithUnrolledBlocks = new HashSet<Integer>();
        int i = 0;

        for (Tree tree : patterns) {
            groups[i] = ts.group++;
            ts.append(p, i > 0 ? "|(" : "(");
            //XXX HACK: see BulkSearchTest.testNoExponentialTimeComplexity
            if (   tree.getKind() == Kind.BLOCK
                && Utilities.isMultistatementWildcardTree(((BlockTree) tree).getStatements().get(0))) {

                List<? extends StatementTree> statements = ((BlockTree) tree).getStatements();

                for (Tree t : statements.subList(1, statements.size())) {
                    ts.scan(t, p);
                }

                patternsWithUnrolledBlocks.add(i);
            } else {
            //XXX end hack
            ts.scan(tree, p);
            }
            ts.append(p, ")");
            i++;
        }

        return new Result(p.toString(), null, groups, patternsWithUnrolledBlocks, ts.identifiers, ts.treeKinds);
    }

    private int depth;
    private int group;
    private boolean method;
    private Map<Integer, List<TreePath>> serializedStart2Tree;
    private int length;
    private TreePath currentPath;

    private final Set<String> identifiers;
    private final Set<String> treeKinds;

    private TreeSerializer(boolean pattern) {
        this.depth = !pattern ? 0 : -1;
        this.group = 1;
        this.serializedStart2Tree = pattern ? null : new HashMap<Integer, List<TreePath>>();
        this.length = 0;

        this.identifiers = new HashSet<String>();
        this.treeKinds = new HashSet<String>();
    }
    
    @Override
    public Void scan(Tree tree, Appendable p) {
        if (tree == null) {
            return null;
        }

        TreePath originalTreePath = currentPath;

        if (this.serializedStart2Tree != null) {
            if (currentPath != null) {
                currentPath = new TreePath(currentPath, tree);
            } else {
                assert tree.getKind() == Kind.COMPILATION_UNIT;
                currentPath = new TreePath((CompilationUnitTree) tree);
            }
        }

        boolean closeWithBracket = false;
        
        try {
        //shouldn't this be handled by visitIdentifier???
        if (   tree.getKind() == Kind.IDENTIFIER
            && ((IdentifierTree) tree).getName().toString().startsWith("$")) {
            append(p, "<([0-9a-z]+)>.*?</\\" + (group++) + ">");
            return null;
        }

        CharSequence wildcardTreeName = Utilities.getWildcardTreeName(tree);

        if (wildcardTreeName != null) {
            boolean isMultistatementWildcard = Utilities.isMultistatementWildcard(wildcardTreeName);

            if (isMultistatementWildcard) {
                append(p, "(?:");
            }

            append(p, "<([0-9a-z]+)>.*?</\\" + (group++) + ">");

            if (isMultistatementWildcard) {
                append(p, ")*?");
            }

            return null;
        }

        if (tree.getKind() == Kind.BLOCK) {
            BlockTree bt = (BlockTree) tree;

            if (!bt.isStatic()) {
                switch (bt.getStatements().size()) {
                    case 1:
                        tree = bt.getStatements().get(0);
                        if (currentPath != null) {
                            currentPath = new TreePath(currentPath, tree);
                        }
                        break;
                    case 2:
                        if (Utilities.isMultistatementWildcardTree(bt.getStatements().get(0))) {
                            append(p, "(?:(?:");
                            scan(bt.getStatements().get(1), p);
                            append(p, ")|(?:");
                            closeWithBracket = true;
                        } else {
                            if (Utilities.isMultistatementWildcardTree(bt.getStatements().get(1))) {
                                append(p, "(?:(?:");
                                scan(bt.getStatements().get(0), p);
                                append(p, ")|(?:");
                                closeWithBracket = true;
                            }
                        }
                        break;
                    case 3:
                        if (Utilities.isMultistatementWildcardTree(bt.getStatements().get(0)) && Utilities.isMultistatementWildcardTree(bt.getStatements().get(2))) {
                            append(p, "(?:(?:");
                            scan(bt.getStatements().get(1), p);
                            append(p, ")|(?:");
                            closeWithBracket = true;
                        }
                        break;
                }
            }
        }

        append(p, "<");
        if (depth != (-1)) {
            append(p, Integer.toHexString(depth++));
        } else {
            append(p, "[0-9a-f]+");
        }
        append(p, ">");

        boolean m = method;

        method = false;

        if (tree.getKind() != Kind.IDENTIFIER && (tree.getKind() != Kind.MEMBER_SELECT || !Utilities.isPureMemberSelect(tree, true))) {
            append(p, kindToShortName.get(tree.getKind()));
            if (treeKinds != null) {
                treeKinds.add(kindToShortName.get(tree.getKind()));
            }
            super.scan(tree, p);
        } else {
            boolean memberSelectWithVariables = tree.getKind() == Kind.MEMBER_SELECT && !Utilities.isPureMemberSelect(tree, false);

            if (memberSelectWithVariables) {
                append(p, "(?:");
                append(p, "(?:");
                append(p, kindToShortName.get(tree.getKind()));
                super.scan(tree, p);
                append(p, ")|(?:");
            }

            append(p, "ID");

            //XXX: is this correct??
            if (/*m && */(tree.getKind() == Kind.IDENTIFIER)) {
                printName(p, ((IdentifierTree) tree).getName());
            }
            if (/*m && */(tree.getKind() == Kind.MEMBER_SELECT)) {
                printName(p, ((MemberSelectTree) tree).getIdentifier());
            }

            if (memberSelectWithVariables) {
                append(p, ")");
                append(p, ")");
            }
        }
        append(p, "</");
        if (depth != (-1)) {
            append(p, Integer.toHexString(--depth));
        } else {
            append(p, "[0-9a-f]+");
        }
        append(p, ">");

        } finally {
        if (this.serializedStart2Tree != null) {
            List<TreePath> paths = this.serializedStart2Tree.get(length);

            if (paths == null) {
                this.serializedStart2Tree.put(length, paths = new LinkedList<TreePath>());
        }
        
            paths.add(currentPath);
            currentPath = originalTreePath;
        }
            if (closeWithBracket) {
                append(p, "))");
            }
        }

        return null;
    }

    private void printName(Appendable p, CharSequence name) {
        if (name.length() == 0 || name.charAt(0) != '$') {
            append(p, name);
            if (identifiers != null) {
                identifiers.add(name.toString());
            }
        } else {
            append(p, "[^<>]+?");
        }
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Appendable p) {
        printName(p, node.getName());
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Appendable p) {
        printName(p, node.getIdentifier());
        return super.visitMemberSelect(node, p);
    }

    @Override
    public Void visitClass(ClassTree node, Appendable p) {
        printName(p, node.getSimpleName());
        return super.visitClass(node, p);
    }

    @Override
    public Void visitVariable(VariableTree node, Appendable p) {
        printName(p, node.getName());
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, Appendable p) {
        printName(p, node.getName());
        return super.visitMethod(node, p);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Appendable p) {
        method = true;
        return super.visitMethodInvocation(node, p);
    }

    private void append(Appendable a, CharSequence what) {
        try {
            a.append(what);
            length += what.length();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
    private static final Map<Kind, String> kindToShortName;

    static {
        kindToShortName = new EnumMap<Kind, String>(Kind.class);
        Set<String> used = new HashSet<String>();

        for (Kind k : Kind.values()) {
            String classSimpleName = k.name();
            StringBuilder shortNameBuilder = new StringBuilder();

            for (String s : classSimpleName.split("_")) {
                shortNameBuilder.append(s.substring(0, 2));
            }

            String shortName = shortNameBuilder.toString();
            
            if (used.contains(shortName)) {
                shortName = classSimpleName;
            }

            kindToShortName.put(k, shortName);
            used.add(shortName);
        }
    }

    public static final class Result {

        public final String encoded;
        public final Map<Integer, List<TreePath>> serializedEnd2Tree;
        public final int[] groups;
        public final Set<Integer> patternsWithUnrolledBlocks;
        public final Set<String> identifiers;
        public final Set<String> treeKinds;

        private Result(String encoded, Map<Integer, List<TreePath>> serializedEnd2Tree, int[] groups, Set<Integer> patternsWithUnrolledBlocks, Set<String> identifiers, Set<String> treeKinds) {
            this.encoded = encoded;
            this.serializedEnd2Tree = serializedEnd2Tree;
            this.groups = groups;
            this.patternsWithUnrolledBlocks = patternsWithUnrolledBlocks;
            this.identifiers = identifiers;
            this.treeKinds = treeKinds;
        }

    }
}
