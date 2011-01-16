/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2011 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.compiler;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.jvm.Gen;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.swing.event.ChangeListener;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationInfoHack;
import org.netbeans.modules.jackpot30.compiler.AbstractHintsAnnotationProcessing.Reporter;
import org.netbeans.modules.java.source.parsing.JavacParserFactory;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.NbPreferences.Provider;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@SupportedAnnotationTypes("*")
@ServiceProvider(service=Processor.class)
public final class HintsAnnotationProcessingImpl extends AbstractProcessor {

    private final Collection<Element> types = new LinkedList<Element>();

    @Override
    public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            return doProcess(annotations, roundEnv);
        } catch (Throwable ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        types.addAll(roundEnv.getRootElements());
        
        if (!roundEnv.processingOver())
            return false;

        try {
            File tmp = File.createTempFile("jackpot30", null);

            tmp.delete();
            tmp.mkdirs();
            tmp.deleteOnExit();

            tmp = FileUtil.normalizeFile(tmp);
            FileUtil.refreshFor(tmp.getParentFile());

            org.openide.filesystems.FileObject tmpFO = FileUtil.toFileObject(tmp);

            if (tmpFO == null) {
                return false;
            }

            CacheFolder.setCacheFolder(tmpFO);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Context c = ((JavacProcessingEnvironment) processingEnv).getContext();
        StandardJavaFileManager s = (StandardJavaFileManager) c.get(JavaFileManager.class);
        ClassPath boot = computeClassPath(s, StandardLocation.PLATFORM_CLASS_PATH);
        ClassPath compile = computeClassPath(s, StandardLocation.CLASS_PATH);
        ClassPath source = computeClassPath(s, StandardLocation.SOURCE_PATH);
        Trees trees = JavacTrees.instance(c);
        Collection<CompilationUnitTree> toClean = new LinkedList<CompilationUnitTree>();
        final Log log = Log.instance(c);

        List<AbstractHintsAnnotationProcessing> processors = new ArrayList<AbstractHintsAnnotationProcessing>();

        for (AbstractHintsAnnotationProcessing p : Lookup.getDefault().lookupAll(AbstractHintsAnnotationProcessing.class)) {
            if (p.initialize(processingEnv)) {
                processors.add(p);
            }
        }

        try {
            for (Element el : types) {
                if (!el.getKind().isClass() && !el.getKind().isInterface()) {
    //                processingEnv.getMessager().printMessage(Kind.NOTE, "Not a class", el);
                    continue;
                }

                TreePath elTree = trees.getPath(el);
                JCCompilationUnit cut = (JCCompilationUnit) elTree.getCompilationUnit();

                if (!cut.sourcefile.toUri().isAbsolute()) {
                    processingEnv.getMessager().printMessage(Kind.NOTE, "Not an absolute URI: " + cut.sourcefile.toUri().toASCIIString(), el);
                    continue; //XXX
                }

                toClean.add(cut);

                doAttribute(c, cut);

                CompilationInfoHack info = new CompilationInfoHack(c, ClasspathInfo.create(boot, compile, source), cut);
                JavaFileObject origSourceFile = log.currentSourceFile();

                try {
                    log.useSource(cut.sourcefile);

                    for (AbstractHintsAnnotationProcessing p : processors) {
                        p.doProcess(info, processingEnv, new Reporter() {
                            @Override public void warning(int offset, String message) {
                                log.warning(offset, "proc.messager", message);
                            }
                        });
                    }
                } finally {
                    log.useSource(origSourceFile);
                }
            }
        } finally {
            for (AbstractHintsAnnotationProcessing p : processors) {
                p.finish();
            }
        }

        for (CompilationUnitTree cut : toClean) {
            new ThoroughTreeCleaner(cut, trees.getSourcePositions()).scan(cut, null);
        }

        try {
            //XXX: workarounding a bug in CRTable (see HintsAnnotationProcessingTest.testCRTable):
            Options.instance(c).remove("-Xjcov");
            Field f = Gen.class.getDeclaredField("genCrt");
            f.setAccessible(true);
            f.set(Gen.instance(c), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedOptions() {
        Set<String> options = new HashSet<String>();

        for (AbstractHintsAnnotationProcessing p : Lookup.getDefault().lookupAll(AbstractHintsAnnotationProcessing.class)) {
            options.addAll(p.getSupportedOptions());
        }

        return options;
    }

    private static ClassPath computeClassPath(StandardJavaFileManager m, StandardLocation kind) {
        List<URL> urls = new LinkedList<URL>();
        Iterable<? extends File> files = m.getLocation(kind);

        if (files != null) {
            for (File f : files) {
                urls.add(FileUtil.urlForArchiveOrDir(f));
            }
        }

        return ClassPathSupport.createClassPath(urls.toArray(new URL[0]));
    }

    private static void doAttribute(Context c, JCCompilationUnit cut) {
        JavaCompiler jc = JavaCompiler.instance(c);
        final Enter enter = Enter.instance(c);
        final Queue<Env<AttrContext>> queued = new LinkedList<Env<AttrContext>>();

        queued.add(enter.getTopLevelEnv(cut));

        new TreeScanner<Void, Void>() {
            @Override
            public Void visitClass(ClassTree node, Void p) {
                Env<AttrContext> env = enter.getEnv(((JCClassDecl) node).sym);

                if (env != null)
                    queued.add(env);

                return super.visitClass(node, p);
            }
        }.scan(cut, null);

        Attr attr = Attr.instance(c);

        for (Env<AttrContext> env : queued) {
            attr.attribClass(env.tree.pos(), env.enclClass.sym);
        }
    }



    static {
        try {
            ClassLoader l = HintsAnnotationProcessingImpl.class.getClassLoader();

            if (l == null) {
                l = ClassLoader.getSystemClassLoader();
            }

            l.setClassAssertionStatus("org.netbeans.api.java.source.CompilationInfo", false);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static final class ThoroughTreeCleaner extends TreeScanner<Void, Void> {

        private final CompilationUnitTree cut;
        private final SourcePositions positions;

        public ThoroughTreeCleaner(CompilationUnitTree cut, SourcePositions positions) {
            this.cut = cut;
            this.positions = positions;
        }

        @Override
        public Void scan(Tree node, Void p) {
            if (node != null) ((JCTree) node).type = null;
            return super.scan(node, p);
        }

        @Override
        public Void visitParameterizedType(ParameterizedTypeTree node, Void p) {
            return super.visitParameterizedType(node, p);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, Void p) {
            ((JCFieldAccess) node).sym = null;
            return super.visitMemberSelect(node, p);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, Void p) {
            ((JCIdent) node).sym = null;
            return super.visitIdentifier(node, p);
        }

        @Override
        public Void visitMethod(MethodTree node, Void p) {
            JCMethodDecl method = (JCMethodDecl) node;

            if (method.sym != null && method.sym.getKind() == ElementKind.CONSTRUCTOR && method.sym.owner.getKind() == ElementKind.ENUM) {
                if (positions.getEndPosition(cut, method.body.stats.head) == (-1)) {
                    method.body.stats = method.body.stats.tail;
                }
            }

            return super.visitMethod(node, p);
        }

        @Override
        public Void visitClass(ClassTree node, Void p) {
            JCClassDecl decl = (JCClassDecl) node;

            if (decl.sym.getKind() == ElementKind.ENUM) {
                if (positions.getEndPosition(cut, decl.defs.head) == (-1)) {
                    decl.defs = decl.defs.tail;
                }
            }
            return super.visitClass(node, p);
        }

    }

    @ServiceProvider(service=MimeDataProvider.class)
    public static final class MimeDataProviderImpl implements MimeDataProvider {

        private static final Lookup L = Lookups.fixed(new JavacParserFactory());

        public Lookup getLookup(MimePath mimePath) {
            if ("text/x-java".equals(mimePath.getPath()))
                return L;
            return null;
        }
        
    }

    public static final class StandaloneMimeDataProviderImpl implements MimeDataProvider {

        private static final Lookup L = Lookups.fixed(NbPreferences.forModule(HintsAnnotationProcessing.class), new JavacParserFactory());

        public Lookup getLookup(MimePath mimePath) {
            if ("text/x-java".equals(mimePath.getPath()))
                return L;
            return null;
        }

    }

    @ServiceProvider(service=SourceForBinaryQueryImplementation.class, position=0)
    public static final class EmptySourceForBinaryQueryImpl implements SourceForBinaryQueryImplementation2 {
        public Result findSourceRoots2(URL binaryRoot) {
            return INSTANCE;
        }
        public SourceForBinaryQuery.Result findSourceRoots(URL binaryRoot) {
            return findSourceRoots2(binaryRoot);
        }
        private static final Result INSTANCE = new Result() {
            public boolean preferSources() {
                return false;
            }
            public org.openide.filesystems.FileObject[] getRoots() {
                return new org.openide.filesystems.FileObject[0];
            }
            public void addChangeListener(ChangeListener l) {}
            public void removeChangeListener(ChangeListener l) {}
        };
    }

    public static class PreferencesProvider implements Provider {

        private final MemoryPreferencesFactory f;

        public PreferencesProvider() {
            this.f = new MemoryPreferencesFactory();
        }

        @Override
        public Preferences preferencesForModule(Class cls) {
            return f.userRoot().node(cls.getPackage().getName());
        }

        @Override
        public Preferences preferencesRoot() {
            return f.userRoot();
        }

    }
    //copied from NB junit:
    public static class MemoryPreferencesFactory implements PreferencesFactory {
        /** Creates a new instance  */
        public MemoryPreferencesFactory() {}

        public Preferences userRoot() {
            return NbPreferences.userRootImpl();
        }

        public Preferences systemRoot() {
            return NbPreferences.systemRootImpl();
        }

        private static class NbPreferences extends AbstractPreferences {
            private static Preferences USER_ROOT;
            private static Preferences SYSTEM_ROOT;

            /*private*/Properties properties;

            static Preferences userRootImpl() {
                if (USER_ROOT == null) {
                    USER_ROOT = new NbPreferences();
                }
                return USER_ROOT;
            }

            static Preferences systemRootImpl() {
                if (SYSTEM_ROOT == null) {
                    SYSTEM_ROOT = new NbPreferences();
                }
                return SYSTEM_ROOT;
            }


            private NbPreferences() {
                super(null, "");
            }

            /** Creates a new instance of PreferencesImpl */
            private  NbPreferences(NbPreferences parent, String name)  {
                super(parent, name);
                newNode = true;
            }

            protected final String getSpi(String key) {
                return properties().getProperty(key);
            }

            protected final String[] childrenNamesSpi() throws BackingStoreException {
                return new String[0];
            }

            protected final String[] keysSpi() throws BackingStoreException {
                return properties().keySet().toArray(new String[0]);
            }

            protected final void putSpi(String key, String value) {
                properties().put(key,value);
            }

            protected final void removeSpi(String key) {
                properties().remove(key);
            }

            protected final void removeNodeSpi() throws BackingStoreException {}
            protected  void flushSpi() throws BackingStoreException {}
            protected void syncSpi() throws BackingStoreException {
                properties().clear();
            }

            @Override
            public void put(String key, String value) {
                try {
                    super.put(key, value);
                } catch (IllegalArgumentException iae) {
                    if (iae.getMessage().contains("too long")) {
                        // Not for us!
                        putSpi(key, value);
                    } else {
                        throw iae;
                    }
                }
            }

            Properties properties()  {
                if (properties == null) {
                    properties = new Properties();
                }
                return properties;
            }

            protected AbstractPreferences childSpi(String name) {
                return new NbPreferences(this, name);
            }
        }

    }
}
