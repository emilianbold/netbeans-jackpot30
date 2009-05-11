/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.java.source.parsing.FileObjects;
import org.openide.util.Exceptions;

/**XXX: cancelability!
 *
 * @author Jan Lahoda
 */
public class Pattern {

    private final CompilationInfo info;
    private final Tree patternTree;
    private final Iterable<Tree> antipatterns;

    private final Map<String, TypeMirror> constraintsHack;

    public Pattern(CompilationInfo info, Tree patternTree, Iterable<Tree> antipatterns, Map<String, TypeMirror> constraintsHack) {
        this.info = info;
        this.patternTree = patternTree;
        this.antipatterns = antipatterns;
        this.constraintsHack = constraintsHack;
    }

    static Pattern compile(CompilationInfo info, String pattern) {
        Map<String, TypeMirror> constraints = new HashMap<String, TypeMirror>();
        pattern = parseOutTypesFromPattern(info, pattern, constraints);

        return compile(info, pattern, constraints);
    }

    public static Pattern compile(CompilationInfo info, String pattern, Map<String, TypeMirror> constraints) {
        return compile(info, pattern, Collections.<String>emptyList(), constraints);
    }

    public static Pattern compile(CompilationInfo info, String pattern, Iterable<String> antipatterns, Map<String, TypeMirror> constraints) {
        Scope[] scope = new Scope[1];
        Tree patternTree = parseAndAttribute(info, pattern, constraints, scope);

        List<Tree> antipatternsTrees = new LinkedList<Tree>();

        for (String ap : antipatterns) {
            Tree p = info.getTreeUtilities().parseExpression(ap, new SourcePositions[1]);

            info.getTreeUtilities().attributeTree(p, scope[0]);

            antipatternsTrees.add(p);
        }
        
        return new Pattern(info, patternTree, antipatternsTrees, constraints);
    }

    public Map<String, TreePath> match(TreePath toCheck) {
        return CopyFinder.computeVariables(info, new TreePath(new TreePath(info.getCompilationUnit()), patternTree), toCheck, new AtomicBoolean(), constraintsHack);
    }

    public boolean checkAntipatterns(TreePath tp) {
        for (Tree ap : antipatterns) {
            if (CopyFinder.computeVariables(info, new TreePath(new TreePath(info.getCompilationUnit()), ap), tp, new AtomicBoolean(), constraintsHack) != null) {
                return false;
            }
        }

        return true;
    }

    public Map<String, TypeMirror> getConstraints() {
        return constraintsHack;
    }

    public Tree getPattern() {
        return patternTree;
    }
    
    private static String parseOutTypesFromPattern(CompilationInfo info, String pattern, Map<String, TypeMirror> variablesToTypes) {
        //XXX:
        TypeElement scope = (TypeElement) info.getTrees().getElement(new TreePath(new TreePath(info.getCompilationUnit()), info.getCompilationUnit().getTypeDecls().get(0)));
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\$.)(\\{([^}]*)\\})?");
        StringBuffer filtered = new StringBuffer();
        Matcher m = p.matcher(pattern);
        int i = 0;

        while (m.find()) {
            filtered.append(pattern.substring(i, m.start()));
            i = m.end();
            
            String var  = m.group(1);
            String type = m.group(3);

            filtered.append(var);
            variablesToTypes.put(var, type != null ? info.getTreeUtilities().parseType(type, scope) : null);
        }

        filtered.append(pattern.substring(i));

        return filtered.toString();
    }

    private static long inc;

    private static Scope constructScope(CompilationInfo info, Map<String, TypeMirror> constraints) {
        StringBuilder clazz = new StringBuilder();

        clazz.append("package $; public class $" + (inc++) + "{");

        for (Entry<String, TypeMirror> e : constraints.entrySet()) {
            if (e.getValue() != null) {
                clazz.append("private ");
                clazz.append(e.getValue().toString()); //XXX
                clazz.append(" ");
                clazz.append(e.getKey());
                clazz.append(";\n");
            }
        }

        clazz.append("private void test() {\n");
        clazz.append("}\n");
        clazz.append("}\n");

        JavacTaskImpl jti = JavaSourceAccessor.getINSTANCE().getJavacTask(info);
        Context context = jti.getContext();

        Log.instance(context).nerrors = 0;

        JavaFileObject jfo = FileObjects.memoryFileObject("$", "$", new File("/tmp/t.java").toURI(), System.currentTimeMillis(), clazz.toString());

        try {
            Iterable<? extends CompilationUnitTree> parsed = jti.parse(jfo);
            CompilationUnitTree cut = parsed.iterator().next();

            jti.analyze(jti.enter(parsed));

            return new ScannerImpl().scan(cut, info);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }
    
//    private static Scope constructScope2(CompilationInfo info, Map<String, TypeMirror> constraints) {
//        JavacScope s = (JavacScope) info.getTrees().getScope(new TreePath(info.getCompilationUnit()));
//        Env<AttrContext> env = s.getEnv();
//
//        env = env.dup(env.tree);
//
//        env.info.
//    }
    
    public static Tree parseAndAttribute(CompilationInfo info, String pattern, Map<String, TypeMirror> constraints, Scope[] scope) {
        scope[0] = constructScope(info, constraints);

        if (scope == null) {
            return null;
        }

        Tree patternTree = info.getTreeUtilities().parseExpression(pattern, new SourcePositions[1]);
        TypeMirror type = info.getTreeUtilities().attributeTree(patternTree, scope[0]);

        if (isError(type)) {
            //maybe type?
            if (Utilities.isPureMemberSelect(patternTree, false) && info.getElements().getTypeElement(pattern) != null) {
                Tree var = info.getTreeUtilities().parseExpression(pattern + ".class;", new SourcePositions[1]);

                type = info.getTreeUtilities().attributeTree(var, scope[0]);

                Tree typeTree = ((MemberSelectTree) var).getExpression();

                if (!isError(info.getTrees().getElement(new TreePath(new TreePath(info.getCompilationUnit()), typeTree)))) {
                    patternTree = typeTree;
                }
            }
        }

        if (isError(type)) {
            //or statement?
            //XXX: how to verify?
            patternTree = info.getTreeUtilities().parseStatement(pattern, new SourcePositions[1]);
            type = info.getTreeUtilities().attributeTree(patternTree, scope[0]);
        }

        return patternTree;
    }

    private static boolean isError(Element el) {
        return (el == null || (el.getKind() == ElementKind.CLASS) && isError(((TypeElement) el).asType()));
    }
    
    private static boolean isError(TypeMirror type) {
        return type == null || type.getKind() == TypeKind.ERROR;
    }
    
    private static final class ScannerImpl extends TreePathScanner<Scope, CompilationInfo> {

        @Override
        public Scope visitBlock(BlockTree node, CompilationInfo p) {
            return p.getTrees().getScope(getCurrentPath());
        }

        @Override
        public Scope visitMethod(MethodTree node, CompilationInfo p) {
            if (node.getReturnType() == null) {
                return null;
            }
            return super.visitMethod(node, p);
        }

        @Override
        public Scope reduce(Scope r1, Scope r2) {
            return r1 != null ? r1 : r2;
        }

    }
    
}
