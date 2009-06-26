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

package org.netbeans.modules.jackpot30.impl;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.TreeMaker;
import org.netbeans.modules.jackpot30.spi.ClassPathBasedHintProvider;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintProvider;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.java.source.builder.TreeFactory;
import org.netbeans.modules.java.source.parsing.FileObjects;
import org.netbeans.modules.java.source.pretty.ImportAnalysis2;
import org.netbeans.modules.java.source.transform.ImmutableTreeTranslator;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbCollections;

/**
 *
 * @author Jan Lahoda
 */
public class Utilities {

    private Utilities() {}

    public static <E> Iterable<E> checkedIterableByFilter(final Iterable raw, final Class<E> type, final boolean strict) {
        return new Iterable<E>() {
            public Iterator<E> iterator() {
                return NbCollections.checkedIteratorByFilter(raw.iterator(), type, strict);
            }
        };
    }
    
//    public static AnnotationTree constructConstraint(WorkingCopy wc, String name, TypeMirror tm) {
//        TreeMaker make = wc.getTreeMaker();
//        ExpressionTree variable = prepareAssignment(make, "variable", make.Literal(name));
//        ExpressionTree type     = prepareAssignment(make, "type", make.MemberSelect((ExpressionTree) make.Type(wc.getTypes().erasure(tm)), "class"));
//        TypeElement constraint  = wc.getElements().getTypeElement(Annotations.CONSTRAINT.toFQN());
//
//        return make.Annotation(make.QualIdent(constraint), Arrays.asList(variable, type));
//    }

    public static ExpressionTree prepareAssignment(TreeMaker make, String name, ExpressionTree value) {
        return make.Assignment(make.Identifier(name), value);
    }

    public static ExpressionTree findValue(AnnotationTree m, String name) {
        for (ExpressionTree et : m.getArguments()) {
            if (et.getKind() == Kind.ASSIGNMENT) {
                AssignmentTree at = (AssignmentTree) et;
                String varName = ((IdentifierTree) at.getVariable()).getName().toString();

                if (varName.equals(name)) {
                    return at.getExpression();
                }
            }

            if (et instanceof LiteralTree/*XXX*/ && "value".equals(name)) {
                return et;
            }
        }

        return null;
    }

    public static List<AnnotationTree> findArrayValue(AnnotationTree at, String name) {
        ExpressionTree fixesArray = findValue(at, name);
        List<AnnotationTree> fixes = new LinkedList<AnnotationTree>();

        if (fixesArray != null && fixesArray.getKind() == Kind.NEW_ARRAY) {
            NewArrayTree trees = (NewArrayTree) fixesArray;

            for (ExpressionTree fix : trees.getInitializers()) {
                if (fix.getKind() == Kind.ANNOTATION) {
                    fixes.add((AnnotationTree) fix);
                }
            }
        }

        if (fixesArray != null && fixesArray.getKind() == Kind.ANNOTATION) {
            fixes.add((AnnotationTree) fixesArray);
        }
        
        return fixes;
    }

    public static boolean isPureMemberSelect(Tree mst, boolean allowVariables) {
        switch (mst.getKind()) {
            case IDENTIFIER: return allowVariables || ((IdentifierTree) mst).getName().charAt(0) != '$';
            case MEMBER_SELECT: return isPureMemberSelect(((MemberSelectTree) mst).getExpression(), allowVariables);
            default: return false;
        }
    }

    public static Map<String, Collection<HintDescription>> sortOutHints(Iterable<? extends HintDescription> hints, Map<String, Collection<HintDescription>> output) {
        for (HintDescription d : hints) {
            Collection<HintDescription> h = output.get(d.getDisplayName());

            if (h == null) {
                output.put(d.getDisplayName(), h = new LinkedList<HintDescription>());
            }

            h.add(d);
        }

        return output;
    }

    public static List<HintDescription> listAllHints(Set<ClassPath> cps) {
        List<HintDescription> result = new LinkedList<HintDescription>();

        for (HintProvider p : Lookup.getDefault().lookupAll(HintProvider.class)) {
            for (HintDescription hd : p.computeHints()) {
                if (hd.getTriggerPattern() == null) continue; //TODO: only pattern based hints are currently supported
                result.add(hd);
            }
        }

        ClassPath cp = ClassPathSupport.createProxyClassPath(cps.toArray(new ClassPath[cps.size()]));

        for (ClassPathBasedHintProvider p : Lookup.getDefault().lookupAll(ClassPathBasedHintProvider.class)) {
            result.addAll(p.computeHints(cp));
        }

        return result;
    }
    
    public static Tree parseAndAttribute(CompilationInfo info, String pattern, Scope scope) {
        return parseAndAttribute(info, JavaSourceAccessor.getINSTANCE().getJavacTask(info), pattern, scope);
    }

    public static Tree parseAndAttribute(JavacTaskImpl jti, String pattern) {
        return parseAndAttribute(null, jti, pattern, null);
    }

    private static Tree parseAndAttribute(CompilationInfo info, JavacTaskImpl jti, String pattern, Scope scope) {
        Tree patternTree = !isStatement(pattern) ? jti.parseExpression(pattern, new SourcePositions[1]) : null;
        boolean expression = true;

        if (patternTree == null || patternTree.getKind() == Kind.ERRONEOUS || (patternTree.getKind() == Kind.IDENTIFIER && ((IdentifierTree) patternTree).getName().contentEquals("<error>"))) { //TODO: <error>...
            patternTree = jti.parseStatement("{" + pattern + "}", new SourcePositions[1]);

            assert patternTree.getKind() == Kind.BLOCK : patternTree.getKind();

            patternTree = ((BlockTree) patternTree).getStatements().get(0);
            
            expression = false;
        }

        FixTree fixTree = new FixTree();
        Context c = jti.getContext();

        //TODO: workaround, ImmutableTreeTranslator needs a CompilationUnitTree (rewriteChildren(BlockTree))
        //but sometimes no CompilationUnitTree (e.g. during BatchApply):
        CompilationUnitTree cut = TreeFactory.instance(c).CompilationUnit(null, Collections.<ImportTree>emptyList(), Collections.<Tree>emptyList(), null);

        fixTree.attach(c, new ImportAnalysis2(c), cut, null);

        patternTree = fixTree.translate(patternTree);

        if (scope == null) {
            return patternTree;
        }

        assert info != null;

        TypeMirror type = info.getTreeUtilities().attributeTree(patternTree, scope);

        if (isError(type) && expression) {
            //maybe type?
            if (Utilities.isPureMemberSelect(patternTree, false) && info.getElements().getTypeElement(pattern) != null) {
                Tree var = info.getTreeUtilities().parseExpression(pattern + ".class;", new SourcePositions[1]);

                type = info.getTreeUtilities().attributeTree(var, scope);

                Tree typeTree = ((MemberSelectTree) var).getExpression();

                if (!isError(info.getTrees().getElement(new TreePath(new TreePath(info.getCompilationUnit()), typeTree)))) {
                    patternTree = typeTree;
                }
            }
        }

        return patternTree;
    }

    private static boolean isError(Element el) {
        return (el == null || (el.getKind() == ElementKind.CLASS) && isError(((TypeElement) el).asType()));
    }

    private static boolean isError(TypeMirror type) {
        return type == null || type.getKind() == TypeKind.ERROR;
    }

    private static boolean isStatement(String pattern) {
        return pattern.trim().endsWith(";");
    }
    
    private static final class FixTree extends ImmutableTreeTranslator {

        @Override
        public Tree translate(Tree tree) {
            if (tree != null && tree.getKind() == Kind.EXPRESSION_STATEMENT) {
                ExpressionStatementTree et = (ExpressionStatementTree) tree;

                if (et.getExpression().getKind() == Kind.ERRONEOUS) {
                    ErroneousTree err = (ErroneousTree) et.getExpression();

                    if (err.getErrorTrees().size() == 1 && err.getErrorTrees().get(0).getKind() == Kind.IDENTIFIER) {
                        IdentifierTree idTree = (IdentifierTree) err.getErrorTrees().get(0);
                        CharSequence id = idTree.getName().toString();

                        if (id.length() > 0 && id.charAt(0) == '$') {
                            return make.ExpressionStatement(idTree);
                        }
                    }
                }
            }
            return super.translate(tree);
        }

    }

    public static @CheckForNull CharSequence getWildcardTreeName(@NonNull Tree t) {
        if (t.getKind() == Kind.EXPRESSION_STATEMENT && ((ExpressionStatementTree) t).getExpression().getKind() == Kind.IDENTIFIER) {
            IdentifierTree identTree = (IdentifierTree) ((ExpressionStatementTree) t).getExpression();
            
            return identTree.getName().toString();
        }

        return null;
    }

    public static boolean isMultistatementWildcard(@NonNull CharSequence name) {
        return name.charAt(name.length() - 1) == '$';
    }

    public static boolean isMultistatementWildcardTree(StatementTree tree) {
        CharSequence name = Utilities.getWildcardTreeName(tree);

        return name != null && Utilities.isMultistatementWildcard(name);
    }

    private static long inc;

    public static Scope constructScope(CompilationInfo info, Map<String, TypeMirror> constraints) {
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

//    private static Scope constructScope2(CompilationInfo info, Map<String, TypeMirror> constraints) {
//        JavacScope s = (JavacScope) info.getTrees().getScope(new TreePath(info.getCompilationUnit()));
//        Env<AttrContext> env = s.getEnv();
//
//        env = env.dup(env.tree);
//
//        env.info.
//    }

    public static String toHumanReadableTime(double d) {
        StringBuilder result = new StringBuilder();
        long inSeconds = (long) (d / 1000);
        int seconds = (int) (inSeconds % 60);
        long inMinutes = inSeconds / 60;

        result.append(inMinutes);
        result.append("m");
        result.append(seconds);
        result.append("s");

        return result.toString();
    }

    public static ClasspathInfo createUniversalCPInfo() {
        //TODO: cannot be a class constant, would break the 
        final ClassPath EMPTY = ClassPathSupport.createClassPath(new URL[0]);
        ClassPath bootstrap = JavaPlatform.getDefault().getBootstrapLibraries();

        return ClasspathInfo.create(bootstrap, EMPTY, EMPTY);
    }
}
