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
package org.netbeans.api.java.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Source;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.tools.Diagnostic;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.modules.java.source.parsing.CompilationInfoImpl;
import org.netbeans.modules.java.source.parsing.HackAccessor;
import org.netbeans.modules.java.source.save.ElementOverlay;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.SnapshotHack;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class CompilationInfoHack extends WorkingCopy {

    private final Context context;
    private final ClasspathInfo cpInfo;
    private FileObject file;
    private String text;
    private TokenHierarchy<?> th;
    private final CompilationUnitTree cut;
    private PositionConverter conv;
    
    public CompilationInfoHack(Context context, ClasspathInfo cpInfo, JCCompilationUnit cut) {
        super(HackAccessor.createCII(cpInfo), ElementOverlay.getOrCreateOverlay());
        this.context = context;
        this.cpInfo = cpInfo;
        try {
            this.file = URLMapper.findFileObject(cut.sourcefile.toUri().toURL());
            this.text = cut.sourcefile.getCharContent(false).toString();
            this.th = TokenHierarchy.create(text, JavaTokenId.language());

            conv = new PositionConverter(SnapshotHack.create(text));
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        this.cut = cut;

        try {
            Field javacTask = CompilationInfoImpl.class.getDeclaredField("javacTask");

            javacTask.setAccessible(true);

            JavacTask prevTask = context.get(JavacTask.class);

            try {
                context.put(JavacTask.class, (JavacTask) null);
                javacTask.set(this.impl, new JavacTaskImpl(context) {});
            } finally {
                context.put(JavacTask.class, (JavacTask) null);
                context.put(JavacTask.class, prevTask);
            }

            Method init = WorkingCopy.class.getDeclaredMethod("init");

            init.setAccessible(true);
            init.invoke(this);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchFieldException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public TreePath getChangedTree() {
        return null;
    }

    @Override
    public ClasspathInfo getClasspathInfo() {
        return cpInfo;
    }

    @Override
    public CompilationUnitTree getCompilationUnit() {
        return cut;
    }

    @Override
    public List<Diagnostic> getDiagnostics() {
        //could be enabled if necessary:
        throw new UnsupportedOperationException();
    }

    @Override
    public Document getDocument() throws IOException {
        return null;
    }

    @Override
    public synchronized ElementUtilities getElementUtilities() {
        return super.getElementUtilities();
    }

    @Override
    public Elements getElements() {
        return JavacElements.instance(context);
    }

    @Override
    public FileObject getFileObject() {
        return file;
    }

    public CompilationInfoImpl getImpl() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JavaSource getJavaSource() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Phase getPhase() {
        return Phase.RESOLVED;
    }

    @Override
    public PositionConverter getPositionConverter() {
        return conv;
    }

    @Override
    public Snapshot getSnapshot() {
        return org.netbeans.modules.parsing.api.Source.create(file).createSnapshot();
    }

    @Override
    public SourceVersion getSourceVersion() {
        return Source.toSourceVersion(Source.instance(context));
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public TokenHierarchy<?> getTokenHierarchy() {
        return th;
    }

    @Override
    public List<? extends TypeElement> getTopLevelElements() throws IllegalStateException {
        final List<TypeElement> result = new ArrayList<TypeElement>();
        CompilationUnitTree cu = getCompilationUnit();
        if (cu == null) {
            return null;
        }
        final Trees trees = getTrees();
        assert trees != null;
        List<? extends Tree> typeDecls = cu.getTypeDecls();
        TreePath cuPath = new TreePath(cu);
        for( Tree t : typeDecls ) {
            TreePath p = new TreePath(cuPath,t);
            Element e = trees.getElement(p);
            if ( e != null && ( e.getKind().isClass() || e.getKind().isInterface() ) ) {
                result.add((TypeElement)e);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized TreeUtilities getTreeUtilities() {
        return super.getTreeUtilities();
    }

    @Override
    public Trees getTrees() {
        return JavacTrees.instance(context);
    }

    @Override
    public synchronized TypeUtilities getTypeUtilities() {
        return super.getTypeUtilities();
    }

    @Override
    public Types getTypes() {
        return JavacTypes.instance(context);
    }

    public Context getContext() {
        return context;
    }

    public ModificationResult computeResult() throws IOException, BadLocationException {
        ModificationResult mr = new ModificationResult();

        mr.diffs.put(getFileObject(), getChanges(new HashMap<Object, int[]>()));

        return mr;
    }
}
