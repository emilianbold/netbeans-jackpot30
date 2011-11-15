/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.cmdline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.core.startup.MainLookup;
import org.netbeans.modules.java.hints.jackpot.impl.MessageImpl;
import org.netbeans.modules.java.hints.jackpot.impl.RulesManager;
import org.netbeans.modules.java.hints.jackpot.impl.Utilities;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch.Folder;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch.VerifiedSpansCallBack;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchUtilities;
import org.netbeans.modules.java.hints.jackpot.impl.batch.ProgressHandleWrapper;
import org.netbeans.modules.java.hints.jackpot.impl.batch.ProgressHandleWrapper.ProgressHandleAbstraction;
import org.netbeans.modules.java.hints.jackpot.impl.batch.Scopes;
import org.netbeans.modules.java.hints.jackpot.spi.HintDescription;
import org.netbeans.modules.java.hints.jackpot.spi.HintMetadata;
import org.netbeans.modules.java.hints.jackpot.spi.HintMetadata.Kind;
import org.netbeans.modules.java.hints.options.HintsSettings;
import org.netbeans.modules.java.source.parsing.JavaPathRecognizer;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class Main {

    private static final String OPTION_NO_APPLY = "no-apply";
    
    public static void main(String... args) throws IOException, ClassNotFoundException {
        System.exit(compile(args));
    }

    public static int compile(String... args) throws IOException, ClassNotFoundException {
        System.setProperty("netbeans.user", "/tmp/tmp-foo");
        
        OptionParser parser = new OptionParser();
//        ArgumentAcceptingOptionSpec<File> projects = parser.accepts("project", "project(s) to refactor").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class);
        ArgumentAcceptingOptionSpec<File> classpath = parser.accepts("classpath", "classpath").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class);
        ArgumentAcceptingOptionSpec<File> bootclasspath = parser.accepts("bootclasspath", "bootclasspath").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class);
        ArgumentAcceptingOptionSpec<File> sourcepath = parser.accepts("sourcepath", "sourcepath").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class);
        ArgumentAcceptingOptionSpec<File> cache = parser.accepts("cache", "a cache directory to store working data").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec<File> out = parser.accepts("out", "output diff").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec<String> hint = parser.accepts("hint", "hint name").withRequiredArg().ofType(String.class);
        ArgumentAcceptingOptionSpec<String> config = parser.accepts("config", "configurations").withRequiredArg().ofType(String.class);

        parser.accepts("list", "list all known hints");
        parser.accepts("progress", "show progress");
        parser.accepts("debug", "enable debugging loggers");
        parser.accepts("help", "prints this help");
        parser.accepts(OPTION_NO_APPLY, "do not apply changes - only print locations were the hint would be applied");

        OptionSet parsed;

        try {
            parsed = parser.parse(args);
        } catch (OptionException ex) {
            System.err.println(ex.getLocalizedMessage());
            parser.printHelpOn(System.out);
            return 1;
        }

        if (parsed.has("help")) {
            parser.printHelpOn(System.out);
            return 0;
        }

        if (!parsed.has("debug")) {
            prepareLoggers();
        }

        File cacheDir = parsed.valueOf(cache);
        boolean deleteCacheDir = false;

        try {
            if (cacheDir == null) {
                cacheDir = File.createTempFile("jackpot", "cache");
                cacheDir.delete();
                if (!(deleteCacheDir = cacheDir.mkdirs())) {
                    System.err.println("cannot create temporary cache");
                    return 1;
                }
            }

            if (cacheDir.isFile()) {
                System.err.println("cache directory exists and is a file");
                return 1;
            }

            String[] cacheDirContent = cacheDir.list();

            if (cacheDirContent != null && cacheDirContent.length > 0 && !new File(cacheDir, "segments").exists()) {
                System.err.println("cache directory is not empty, but was not created by this tool");
                return 1;
            }

            cacheDir.mkdirs();

            CacheFolder.setCacheFolder(FileUtil.toFileObject(FileUtil.normalizeFile(cacheDir)));

            org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
            RepositoryUpdater.getDefault().start(false);

            List<FileObject> roots = new ArrayList<FileObject>();
            List<Folder> rootFolders = new ArrayList<Folder>();

            for (String sr : parsed.nonOptionArguments()) {
                File r = new File(sr);
                FileObject root = FileUtil.toFileObject(r);

                if (root != null) {
                    roots.add(root);
                    rootFolders.add(new Folder(root));
                }
            }

            if (roots.isEmpty() && !parsed.has("list")) {
                System.err.println("no source roots to work on");
                return 1;
            }

            Iterable<? extends HintDescription> hints;
            
            ClassPath bootCP = createClassPath(parsed.has(bootclasspath) ? parsed.valuesOf(bootclasspath) : null, createDefaultBootClassPath());
            ClassPath compileCP = createClassPath(parsed.has(classpath) ? parsed.valuesOf(classpath) : null, ClassPath.EMPTY);
            ClassPath sourceCP = createClassPath(parsed.has(sourcepath) ? parsed.valuesOf(sourcepath) : null, ClassPathSupport.createClassPath(roots.toArray(new FileObject[0])));
            ClassPath hintsCP = ClassPathSupport.createProxyClassPath(bootCP, compileCP, sourceCP);

            if (parsed.has("list")) {
                printHints(hintsCP);
                return 0;
            }

            if (parsed.has(hint)) {
                hints = findHints(hintsCP, parsed.valueOf(hint));
            } else {
                hints = allHints(hintsCP);
            }

            if (!hints.iterator().hasNext()) {
                System.err.println("no hints specified");
                return 1;
            }

            if (parsed.has(config) && !parsed.has(hint)) {
                System.err.println("--config cannot specified when no hint is specified");
                return 1;
            }

            if (parsed.has(config)) {
                Iterator<? extends HintDescription> hit = hints.iterator();
                HintDescription hd = hit.next();

                if (hit.hasNext()) {
                    System.err.println("--config cannot specified when more than one hint is specified");

                    return 1;
                }

                Preferences prefs = RulesManager.getPreferences(hd.getMetadata().id, HintsSettings.getCurrentProfileId());

                if (prefs == null) {
                    System.err.println("hint '" + parsed.valueOf(hint) + "' cannot be configured");
                    return 1;
                }
                
                boolean stop = false;

                for (String c : parsed.valuesOf(config)) {
                    int assign = c.indexOf('=');

                    if (assign == (-1)) {
                        System.err.println("configuration option is missing '=' (" + c + ")");
                        stop = true;
                        continue;
                    }

                    prefs.put(c.substring(0, assign), c.substring(assign + 1));
                }

                if (stop) {
                    return 1;
                }
            }

            try {
                MainLookup.register(new ClassPathProviderImpl(bootCP, compileCP, sourceCP));
                MainLookup.register(new JavaPathRecognizer());
                MainLookup.register(new SourceLevelQueryImpl(sourceCP, "1.7"));
                
                FileUtil.setMIMEType("java", "text/x-java");

                ProgressHandleWrapper progress = parsed.has("progress") ? new ProgressHandleWrapper(new ConsoleProgressHandleAbstraction(), 1) : new ProgressHandleWrapper(1);

                if (parsed.has(OPTION_NO_APPLY)) {
                    findOccurrences(hints, rootFolders.toArray(new Folder[0]), progress, parsed.valueOf(out));
                } else {
                    apply(hints, rootFolders.toArray(new Folder[0]), progress, parsed.valueOf(out));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } finally {
            if (deleteCacheDir) {
                FileObject cacheDirFO = FileUtil.toFileObject(cacheDir);

                if (cacheDirFO != null) {
                    //TODO: would be better to do j.i.File.delete():
                    cacheDirFO.delete();
                }
            }
        }

        return 0;
    }

    private static Map<HintMetadata, Collection<? extends HintDescription>> listHints(ClassPath from) {
        Map<HintMetadata, Collection<? extends HintDescription>> result = new HashMap<HintMetadata, Collection<? extends HintDescription>>();

        for (Map.Entry<HintMetadata, Collection<? extends HintDescription>> entry: RulesManager.getInstance().allHints.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        for (Map.Entry<? extends HintMetadata, ? extends Collection<? extends HintDescription>> entry: org.netbeans.modules.java.hints.jackpot.impl.refactoring.Utilities.sortByMetadata(Utilities.listClassPathHints(Collections.singleton(from))).entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
    
    private static Iterable<? extends HintDescription> findHints(ClassPath from, String name) {
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(from).entrySet()) {
            if (e.getKey().displayName.equals(name)) {
                descs.addAll(e.getValue());
            }
        }

        return descs;
    }

    private static Iterable<? extends HintDescription> allHints(ClassPath from) {
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(from).entrySet()) {
            if (e.getKey().kind != Kind.HINT) continue;
            if (!e.getKey().enabled) continue;
            descs.addAll(e.getValue());
        }

        return descs;
    }

    private static final Logger TOP_LOGGER = Logger.getLogger("");

    private static void prepareLoggers() {
        TOP_LOGGER.setLevel(Level.OFF);
    }
    
    private static void findOccurrences(Iterable<? extends HintDescription> descs, Folder[] sourceRoot, ProgressHandleWrapper progress, File out) throws IOException {
        ProgressHandleWrapper w = progress.startNextPartWithEmbedding(1, 1);
        BatchResult occurrences = BatchSearch.findOccurrences(descs, Scopes.specifiedFoldersScope(sourceRoot), w);

        List<MessageImpl> problems = new LinkedList<MessageImpl>();
        BatchSearch.getVerifiedSpans(occurrences, progress, new VerifiedSpansCallBack() {
            @Override public void groupStarted() {}
            @Override public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                for (ErrorDescription ed : hints) {
                    print(ed);
                }
                return true;
            }
            @Override public void groupFinished() {}
            @Override public void cannotVerifySpan(Resource r) {
                //TODO: ignored - what to do?
            }
        }, problems);
    }

    private static void print(ErrorDescription error) throws IOException {
        int lineNumber = error.getRange().getBegin().getLine();
        String line = error.getFile().asLines().get(lineNumber);
        int column = error.getRange().getBegin().getColumn();
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < column; i++) {
            if (Character.isWhitespace(line.charAt(i))) {
                b.append(line.charAt(i));
            } else {
                b.append(' ');
            }
        }

        b.append('^');

        System.out.println(FileUtil.getFileDisplayName(error.getFile()) + ":" + lineNumber + ": warning: " + error.getDescription());
        System.out.println(line);
        System.out.println(b);
    }

    private static void apply(Iterable<? extends HintDescription> descs, Folder[] sourceRoot, ProgressHandleWrapper progress, File out) throws IOException {
        ProgressHandleWrapper w = progress.startNextPartWithEmbedding(1, 1);
        BatchResult occurrences = BatchSearch.findOccurrences(descs, Scopes.specifiedFoldersScope(sourceRoot), w);

        List<MessageImpl> problems = new LinkedList<MessageImpl>();
        Collection<ModificationResult> diffs = BatchUtilities.applyFixes(occurrences, w, new AtomicBoolean(), problems);

        if (out != null) {
            Writer outS = null;

            try {
                outS = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out)));

                for (ModificationResult mr : diffs) {
                    org.netbeans.modules.jackpot30.impl.batch.BatchUtilities.exportDiff(mr, null, outS);
                }
            } finally {
                try {
                    outS.close();
                } catch (IOException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            for (ModificationResult mr : diffs) {
                mr.commit();
            }
        }
    }

    private static void printHints(ClassPath from) throws IOException {
        Set<String> hints = new TreeSet<String>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(from).entrySet()) {
            hints.add(e.getKey().displayName);
        }

        for (String h : hints) {
            System.out.println(h);
        }
    }

    private static ClassPath createDefaultBootClassPath() throws IOException {
        try {
            String cp = System.getProperty("sun.boot.class.path");
            List<URL> urls = new ArrayList<URL>();
            String[] paths = cp.split(Pattern.quote(System.getProperty("path.separator")));

            for (String path : paths) {
                File f = new File(path);

                if (!f.canRead())
                    continue;

                FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(f));

                if (FileUtil.isArchiveFile(fo)) {
                    fo = FileUtil.getArchiveRoot(fo);
                }

                if (fo != null) {
                    urls.add(fo.getURL());
                }
            }

            return ClassPathSupport.createClassPath(urls.toArray(new URL[0]));
        } catch (FileStateInvalidException e) {
            throw e;
        }
    }

    private static ClassPath createClassPath(Iterable<? extends File> roots, ClassPath def) {
        if (roots == null) return def;

        List<URL> rootURLs = new ArrayList<URL>();

        for (File r : roots) {
            rootURLs.add(FileUtil.urlForArchiveOrDir(r));
        }

        return ClassPathSupport.createClassPath(rootURLs.toArray(new URL[0]));
    }

    @ServiceProvider(service=Lookup.class)
    public static final class LookupProviderImpl extends ProxyLookup {

        public LookupProviderImpl() {
            super(Lookups.forPath("Services/AntBasedProjectTypes"));
        }
    }

    public static final class ClassPathProviderImpl implements ClassPathProvider {
        private final ClassPath boot;
        private final ClassPath compile;
        private final ClassPath source;

        public ClassPathProviderImpl(ClassPath boot, ClassPath compile, ClassPath source) {
            this.boot = boot;
            this.compile = compile;
            this.source = source;
        }

        @Override
        public ClassPath findClassPath(FileObject file, String type) {
            if (source.findOwnerRoot(file) != null) {
                if (ClassPath.BOOT.equals(type)) {
                    return boot;
                } else if (ClassPath.COMPILE.equals(type)) {
                    return compile;
                } else  if (ClassPath.SOURCE.equals(type)) {
                    return source;
                }
            }

            return null;
        }
    }

    public static final class SourceLevelQueryImpl implements SourceLevelQueryImplementation2 {
        private final ClassPath sourceCP;
        private final Result sourceLevel;

        public SourceLevelQueryImpl(ClassPath sourceCP, final String sourceLevel) {
            this.sourceCP = sourceCP;
            this.sourceLevel = new Result() {
                @Override public String getSourceLevel() {
                    return sourceLevel;
                }
                @Override public void addChangeListener(ChangeListener listener) {}
                @Override public void removeChangeListener(ChangeListener listener) {}
            };
        }

        @Override
        public Result getSourceLevel(FileObject javaFile) {
            if (sourceCP.findOwnerRoot(javaFile) != null) {
                return sourceLevel;
            } else {
                return null;
            }
        }

    }

    private static final class ConsoleProgressHandleAbstraction implements ProgressHandleAbstraction {

        private final int width = 80;

        private int total = -1;
        private int current = 0;

        public ConsoleProgressHandleAbstraction() {
        }

        @Override
        public synchronized void start(int totalWork) {
            if (total != (-1)) throw new UnsupportedOperationException();
            total = totalWork;
            update();
        }

        @Override
        public synchronized void progress(int currentWorkDone) {
            current = currentWorkDone;
            update();
        }

        @Override
        public void progress(String message) {
        }

        @Override
        public void finish() {
        }

        private void update() {
            RequestProcessor.getDefault().post(new Runnable() {

                @Override
                public void run() {
                    doUpdate();
                }
            });
        }

        private int currentShownDone = -1;

        private void doUpdate() {
            int done;

            synchronized(this) {
                done = (int) ((((double) width - 2) / total) * current);

                if (done == currentShownDone) {
                    return;
                }

                currentShownDone = done;
            }
            
            int todo = width - done;
            PrintStream pw = System.out;

            pw.print("[");


            while (done-- > 0) {
                pw.print("=");
            }

            while (todo-- > 0) {
                pw.print(" ");
            }

            pw.print("]\r");
        }

    }

}

//    public static void main(String... args) throws IOException, ClassNotFoundException {
//        System.setProperty("netbeans.user", "/tmp/tmp-foo");
//
//        OptionParser parser = new OptionParser();
////        ArgumentAcceptingOptionSpec<File> projects = parser.accepts("project", "project(s) to refactor").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class);
//        ArgumentAcceptingOptionSpec<File> classpath = parser.accepts("classpath", "classpath").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class);
//        ArgumentAcceptingOptionSpec<File> cache = parser.accepts("cache", "source directory").withRequiredArg().ofType(File.class);
//        ArgumentAcceptingOptionSpec<String> hint = parser.accepts("hint", "hint name").withRequiredArg().ofType(String.class);
//
//        parser.accepts("list", "list all known hints");
//
//        OptionSet parsed;
//
//        try {
//            parsed = parser.parse(args);
//        } catch (OptionException ex) {
//            System.err.println(ex.getLocalizedMessage());
//            parser.printHelpOn(System.err);
//            return;
//        }
//
//        if (parsed.has("list")) {
//            listHints();
//            System.exit(0);
//        }
//
//        File cacheDir = parsed.valueOf(cache);
//
//        if (cacheDir.isFile()) {
//            System.err.println("cache directory exists and is a file");
//            System.exit(1);
//        }
//
//        String[] cacheDirContent = cacheDir.list();
//
//        if (cacheDirContent != null && cacheDirContent.length > 0 && !new File(cacheDir, "segments").exists()) {
//            System.err.println("cache directory is not empty, but was not created by this tool");
//            System.exit(1);
//        }
//
//        cacheDir.mkdirs();
//
//        CacheFolder.setCacheFolder(FileUtil.toFileObject(FileUtil.normalizeFile(cacheDir)));
//
//
//        Map<String, Object> attrs = new HashMap<String, Object>();
//
//        attrs.put("type", "org.netbeans.modules.java.j2seproject");
//        attrs.put("iconResource", "org/netbeans/modules/java/j2seproject/ui/resources/j2seProject.png");
//        attrs.put("sharedName", "data");
//        attrs.put("sharedNamespace", "http://www.netbeans.org/ns/j2se-project/3");
//        attrs.put("privateName", "data");
//        attrs.put("privateNamespace", "http://www.netbeans.org/ns/j2se-project-private/1");
//        attrs.put("className", "org.netbeans.modules.java.j2seproject.J2SEProject");
//        attrs.put("instanceClass", "org.netbeans.spi.project.support.ant.AntBasedProjectType");
//
//        MainLookup.register(AntBasedProjectFactorySingleton.create(attrs));
//
//        //XXX:
//        MainLookup.register(new AntBasedProjectFactorySingleton());
//
//        System.err.println(Lookup.getDefault().lookupAll(ClassPathProvider.class));
//
////        for (Object o : Lookups.forPath("Services/AntBasedProjectTypes").lookupAll(Object.class)) {
////            MainLookup.register(o);
////        }
//
//        org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
//        RepositoryUpdater.getDefault().start(false);
//
//        List<FileObject> roots = new ArrayList<FileObject>();
//        List<File> projectRoots = parsed.valuesOf(projects);
//
//        if (projectRoots.isEmpty()) {
//            System.err.println("no projects to work on specified");
//            System.exit(1);
//        }
//
//        for (File projectRoot : projectRoots) {
//            if (!projectRoot.isDirectory()) {
//                System.err.println("project: " + projectRoot + " does not exist");
//                continue;
//            }
//
//            Project prj = ProjectManager.getDefault().findProject(FileUtil.toFileObject(FileUtil.normalizeFile(projectRoot)));
//
//            if (prj == null) {
//                System.err.println("project: " + projectRoot + " cannot be resolved to NetBeans project");
//                continue;
//            }
//
//            SourceGroup[] sourceGroups = ProjectUtils.getSources(prj).getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
//
//            for (SourceGroup sg : sourceGroups) {
//                roots.add(sg.getRootFolder());
//            }
//        }
//
//        if (roots.isEmpty()) {
//            System.err.println("no source roots to work on");
//            System.exit(1);
//        }
//
//        Iterable<? extends HintDescription> hints = findHints(parsed.valueOf(hint));
//
//        if (!hints.iterator().hasNext()) {
//            System.err.println("no hints specified");
//            System.exit(1);
//        }
//
//        try {
//            perform(hints, roots.toArray(new FileObject[0]));
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//
//        System.exit(0);
//    }
