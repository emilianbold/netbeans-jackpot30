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

package org.netbeans.modules.jackpot30.hints.pm;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jan Lahoda
 */
public class TreeSerializer extends TreeScanner<Void, Appendable> {

    public static void serializeText(Tree tree, Appendable p) {
        new TreeSerializer(false).scan(tree, p);
    }

    public static int[] serializePatterns(Appendable p, Tree... patterns) {
        TreeSerializer ts = new TreeSerializer(true);
        int[] groups = new int[patterns.length];
        int i = 0;

        for (Tree tree : patterns) {
            groups[i] = ts.group++;
            append(p, i > 0 ? "|(" : "(");
            ts.scan(tree, p);
            append(p, ")");
            i++;
        }

        return groups;
    }

    private int depth;
    private int group;

    private TreeSerializer(boolean pattern) {
        this.depth = !pattern ? 0 : -1;
        this.group = 1;
    }
    
    @Override
    public Void scan(Tree tree, Appendable p) {
        if (tree == null) {
            return null;
        }

        if (   tree.getKind() == Kind.IDENTIFIER
            && ((IdentifierTree) tree).getName().toString().startsWith("$")) {
            append(p, "<([0-9a-z]+)>.*?</\\" + (group++) + ">");
            return null;
        }

        append(p, "<");
        if (depth != (-1)) {
            append(p, Integer.toHexString(depth++));
        } else {
            append(p, "[0-9a-f]+");
        }
        append(p, ">");
        append(p, kindToShortName.get(tree.getKind()));
        super.scan(tree, p);
        append(p, "</");
        if (depth != (-1)) {
            append(p, Integer.toHexString(--depth));
        } else {
            append(p, "[0-9a-f]+");
        }
        append(p, ">");

        return null;
    }

//    @Override
//    public Void visitIdentifier(IdentifierTree node, Appendable p) {
//        append(p, node.getName());
//        return super.visitIdentifier(node, p);
//    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Appendable p) {
        append(p, node.getIdentifier());
        return super.visitMemberSelect(node, p);
    }

    @Override
    public Void visitClass(ClassTree node, Appendable p) {
        append(p, node.getSimpleName());
        return super.visitClass(node, p);
    }

    @Override
    public Void visitVariable(VariableTree node, Appendable p) {
        append(p, node.getName());
        return super.visitVariable(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, Appendable p) {
        append(p, node.getName());
        return super.visitMethod(node, p);
    }

    private static void append(Appendable a, CharSequence what) {
        try {
            a.append(what);
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

}
