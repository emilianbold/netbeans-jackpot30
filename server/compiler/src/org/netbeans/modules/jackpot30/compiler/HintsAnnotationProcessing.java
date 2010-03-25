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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import org.netbeans.api.diff.Difference;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationInfoHack;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.modules.diff.builtin.provider.BuiltInDiffProvider;
import org.netbeans.modules.diff.builtin.visualizer.TextDiffVisualizer;
import org.netbeans.modules.jackpot30.impl.RulesManager;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.batch.JavaFixImpl;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintMetadata;
import org.netbeans.modules.jackpot30.spi.HintsRunner;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.java.source.parsing.JavacParserFactory;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.spi.diff.DiffProvider;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
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
@SupportedOptions(value={"jackpot30_enable_cp_hints","jackpot30_apply_cp_hints","jackpot30_enabled_hc_hints","jackpot30_apply_hc_hints"})
@ServiceProvider(service=Processor.class)
public class HintsAnnotationProcessing extends AbstractProcessor {

    private final Collection<Element> types = new LinkedList<Element>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.err.println("INVOKED");
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

            FileUtil.refreshFor(tmp.getParentFile());

            org.openide.filesystems.FileObject tmpFO = FileUtil.toFileObject(FileUtil.normalizeFile(tmp));

            if (tmpFO == null) {
                return false;
            }

            CacheFolder.setCacheFolder(tmpFO);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        OutputStream diff = null;
        Context c = ((JavacProcessingEnvironment) processingEnv).getContext();
        StandardJavaFileManager s = (StandardJavaFileManager) c.get(JavaFileManager.class);
        ClassPath boot = computeClassPath(s, StandardLocation.PLATFORM_CLASS_PATH);
        ClassPath compile = computeClassPath(s, StandardLocation.CLASS_PATH);
        ClassPath source = computeClassPath(s, StandardLocation.SOURCE_PATH);
        Trees trees = JavacTrees.instance(c);
        Collection<CompilationUnitTree> toClean = new LinkedList<CompilationUnitTree>();
        Log log = Log.instance(c);
        
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
            
            int nerrors = log.nerrors;
            int nwarnings = log.nwarnings;
            
            CompilationInfoHack info = new CompilationInfoHack(c, ClasspathInfo.create(boot, compile, source), cut);
            Set<HintDescription> hardCodedHints = new LinkedHashSet<HintDescription>();

            for (Collection<? extends HintDescription> v : RulesManager.computeAllHints().values()) {
                hardCodedHints.addAll(v);
            }

            ContainsChecker<String> enabledHints = readHintsId("jackpot30_enabled_hc_hints", new SettingsBasedChecker());
            
            for (Iterator<HintDescription> it = hardCodedHints.iterator(); it.hasNext(); ) {
                HintMetadata current = it.next().getMetadata();

                if (   (current.kind == HintMetadata.Kind.HINT || current.kind == HintMetadata.Kind.HINT_NON_GUI)
                    && enabledHints.contains(current.id)) {
                    continue;
                }
                
                it.remove();
            }

            ContainsChecker<String> enabledApplyHints = readHintsId("jackpot30_apply_hc_hints", new HardcodedContainsChecker<String>(true));

            List<HintDescription> hintDescriptions = new LinkedList<HintDescription>(hardCodedHints);

            if (isEnabled("jackpot30_enable_cp_hints")) {
                hintDescriptions.addAll(new LinkedList<HintDescription>(Utilities.listClassPathHints(new HashSet<ClassPath>(Arrays.asList(/*compile, */source)))));
            }

            boolean applyCPHints = isEnabled("jackpot30_apply_cp_hints");

            System.err.println("hintDescriptions=" + hintDescriptions);
            Map<HintDescription, List<ErrorDescription>> hints = HintsRunner.computeErrors(info, hintDescriptions, new AtomicBoolean());

            System.err.println("hint=" + hints);
            log.nerrors = nerrors;
            log.nwarnings = nwarnings;

            JavaFileObject currentSource = log.currentSourceFile();

            try {
                boolean fixPerformed = false;

                log.useSource(cut.sourcefile);

                for (Entry<HintDescription, List<ErrorDescription>> e : hints.entrySet()) {
                    boolean applyFix = hardCodedHints.contains(e.getKey()) ? enabledApplyHints.contains(e.getKey().getMetadata().id) : applyCPHints;
                    
                    for (ErrorDescription ed : e.getValue()) {
                        log.warning(ed.getRange().getBegin().getOffset(), "proc.messager", ed.getDescription());

                        if (!applyFix) continue;

                        nerrors = log.nerrors;
                        nwarnings = log.nwarnings;
                        Fix f = ed.getFixes().getFixes().get(0);

                        if (!(f instanceof JavaFixImpl)) {
                            log.warning(ed.getRange().getBegin().getOffset(), "proc.messager", "Cannot apply primary fix (not a JavaFix)");
                            continue;
                        }

                        JavaFixImpl jfi = (JavaFixImpl) f;

                        try {
                            JavaFixImpl.Accessor.INSTANCE.process(jfi.jf, info, false);
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        } finally {
                            log.nerrors = nerrors;
                            log.nwarnings = nwarnings;
                        }

                        fixPerformed = true;
                    }
                }

                System.err.println("fixPerformed=" + fixPerformed);
                if (fixPerformed) {
                    ModificationResult mr = info.computeResult();
                    String orig = info.getText();
                    String nue = mr.getResultingSource(info.getFileObject());

                    if (diff == null) {
                        FileObject upgradeDiffFO = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "META-INF/upgrade/upgrade.diff");

                        diff = upgradeDiffFO.openOutputStream();
                    }

                    exportDiff(cut.sourcefile.toUri().getPath(), orig, nue, diff);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                log.useSource(currentSource);
            }
        }

        //XXX: should be in finally!
        if (diff != null) {
            try {
                diff.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        for (CompilationUnitTree cut : toClean) {
            new ThoroughTreeCleaner(cut, trees.getSourcePositions()).scan(cut, null);
        }

        //XXX: workarounding a bug in CRTable (see HintsAnnotationProcessingTest.testCRTable):
        //the workaround is not working anymore:
//        Options.instance(c).remove("-Xjcov");
        
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
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

    //copied from the diff module:
    private static void exportDiff(String name, String original, String modified, OutputStream out) throws IOException {
        DiffProvider diff = new BuiltInDiffProvider();//(DiffProvider) Lookup.getDefault().lookup(DiffProvider.class);

        Reader r1 = null;
        Reader r2 = null;
        Difference[] differences;

        try {
            r1 = new StringReader(original);
            r2 = new StringReader(modified);
            differences = diff.computeDiff(r1, r2);
        } finally {
            if (r1 != null) try { r1.close(); } catch (Exception e) {}
            if (r2 != null) try { r2.close(); } catch (Exception e) {}
        }

        try {
            InputStream is;
            r1 = new StringReader(original);
            r2 = new StringReader(modified);
            TextDiffVisualizer.TextDiffInfo info = new TextDiffVisualizer.TextDiffInfo(
                name, // NOI18N
                name,  // NOI18N
                null,
                null,
                r1,
                r2,
                differences
            );
            info.setContextMode(true, 3);
            String diffText;
//            if (format == unifiedFilter) {
                diffText = TextDiffVisualizer.differenceToUnifiedDiffText(info);
//            } else {
//                diffText = TextDiffVisualizer.differenceToNormalDiffText(info);
//            }
            is = new ByteArrayInputStream(diffText.getBytes("utf8"));  // NOI18N
            while(true) {
                int i = is.read();
                if (i == -1) break;
                out.write(i);
            }
        } finally {
            if (r1 != null) try { r1.close(); } catch (Exception e) {}
            if (r2 != null) try { r2.close(); } catch (Exception e) {}
        }
    }

    static {
        try {
            ClassLoader l = HintsAnnotationProcessing.class.getClassLoader();

            if (l == null) {
                l = ClassLoader.getSystemClassLoader();
            }

            l.setClassAssertionStatus("org.netbeans.api.java.source.CompilationInfo", false);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private boolean isEnabled(String key) {
        if (processingEnv.getOptions().containsKey(key)) {
            return Boolean.valueOf(processingEnv.getOptions().get(key));
        }

        return true;
    }

    private ContainsChecker<String> readHintsId(String key, ContainsChecker<String> def) {
        if (processingEnv.getOptions().containsKey(key)) {
            String toSplit = processingEnv.getOptions().get(key);
            
            if (toSplit == null) {
                return new HardcodedContainsChecker<String>(false);
            }

            return new SetBasedContainsChecker<String>(new HashSet<String>(Arrays.asList(toSplit.split(":"))));
        }

        return def;
    }

    private interface ContainsChecker<T> {
        public boolean contains(T t);
    }

    private static final class SetBasedContainsChecker<T> implements ContainsChecker<T> {
        private final Set<T> set;
        public SetBasedContainsChecker(Set<T> set) {
            this.set = set;
        }
        public boolean contains(T t) {
            return set.contains(t);
        }
    }

    private static final class HardcodedContainsChecker<T> implements ContainsChecker<T> {
        private final boolean result;
        public HardcodedContainsChecker(boolean result) {
            this.result = result;
        }
        public boolean contains(T t) {
            return result;
        }
    }

    private static final class SettingsBasedChecker implements ContainsChecker<String> {
        private static final Set<String> enabled = new HashSet<String>();
        public SettingsBasedChecker() {
            for (HintMetadata hm : RulesManager.getInstance().allHints.keySet()) {
                if (   RulesManager.getInstance().isHintEnabled(hm)
                    && RulesManager.getInstance().getHintSeverity(hm) != HintMetadata.HintSeverity.CURRENT_LINE_WARNING) {
                    enabled.add(hm.id);
                }
            }
        }
        public boolean contains(String t) {
            return enabled.contains(t);
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

        private static final Lookup L = Lookups.fixed(NbPreferences.forModule(HintsAnnotationProcessing.class));

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
