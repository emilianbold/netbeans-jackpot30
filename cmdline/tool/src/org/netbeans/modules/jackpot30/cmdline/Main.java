/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010-2011 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010-2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.cmdline;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.core.startup.MainLookup;
import org.netbeans.modules.jackpot30.ui.settings.XMLHintPreferences;
import org.netbeans.modules.java.hints.jackpot.spi.PatternConvertor;
import org.netbeans.modules.java.hints.providers.spi.HintDescription;
import org.netbeans.modules.java.hints.providers.spi.HintMetadata;
import org.netbeans.modules.java.hints.spiimpl.MessageImpl;
import org.netbeans.modules.java.hints.spiimpl.RulesManager;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch.Folder;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch.Resource;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchSearch.VerifiedSpansCallBack;
import org.netbeans.modules.java.hints.spiimpl.batch.BatchUtilities;
import org.netbeans.modules.java.hints.spiimpl.batch.ProgressHandleWrapper;
import org.netbeans.modules.java.hints.spiimpl.batch.ProgressHandleWrapper.ProgressHandleAbstraction;
import org.netbeans.modules.java.hints.spiimpl.batch.Scopes;
import org.netbeans.modules.java.hints.spiimpl.options.HintsPanel;
import org.netbeans.modules.java.hints.spiimpl.options.HintsSettings;
import org.netbeans.modules.java.hints.spiimpl.refactoring.Utilities.ClassPathBasedHintWrapper;
import org.netbeans.modules.java.source.parsing.JavaPathRecognizer;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.hints.Hint.Kind;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class Main {

    private static final String OPTION_APPLY = "apply";
    private static final String OPTION_NO_APPLY = "no-apply";
    private static final String SOURCE_LEVEL_DEFAULT = "1.7";
    private static final String ACCEPTABLE_SOURCE_LEVEL_PATTERN = "(1\\.)?[2-9][0-9]*";
    
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
        ArgumentAcceptingOptionSpec<File> configFile = parser.accepts("config-file", "configuration file").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec<String> hint = parser.accepts("hint", "hint name").withRequiredArg().ofType(String.class);
        ArgumentAcceptingOptionSpec<String> config = parser.accepts("config", "configurations").withRequiredArg().ofType(String.class);
        ArgumentAcceptingOptionSpec<String> source = parser.accepts("source", "source level").withRequiredArg().ofType(String.class).defaultsTo(SOURCE_LEVEL_DEFAULT);
        ArgumentAcceptingOptionSpec<File> hintFile = parser.accepts("hint-file", "file with rules that should be performed").withRequiredArg().ofType(File.class);

        parser.accepts("list", "list all known hints");
        parser.accepts("progress", "show progress");
        parser.accepts("debug", "enable debugging loggers");
        parser.accepts("help", "prints this help");
        parser.accepts(OPTION_NO_APPLY, "do not apply changes - only print locations were the hint would be applied");
        parser.accepts(OPTION_APPLY, "apply changes");
        parser.accepts("show-gui", "show configuration dialog");

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

        ClassPath bootCP = createClassPath(parsed.has(bootclasspath) ? parsed.valuesOf(bootclasspath) : null, createDefaultBootClassPath());
        ClassPath compileCP = createClassPath(parsed.has(classpath) ? parsed.valuesOf(classpath) : null, ClassPath.EMPTY);
        final ClassPath sourceCP = createClassPath(parsed.has(sourcepath) ? parsed.valuesOf(sourcepath) : null, ClassPathSupport.createClassPath(roots.toArray(new FileObject[0])));
        final ClassPath binaryCP = ClassPathSupport.createProxyClassPath(bootCP, compileCP);

        if (parsed.has("show-gui")) {
            if (parsed.has(configFile)) {
                final File settingsFile = parsed.valueOf(configFile);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override public void run() {
                            try {
                                showGUICustomizer(settingsFile, binaryCP, sourceCP);
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            } catch (BackingStoreException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }
                    });
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (InvocationTargetException ex) {
                    Exceptions.printStackTrace(ex);
                }

                return 0;
            } else {
                System.err.println("show-gui requires config-file");
                return 1;
            }
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

            if (roots.isEmpty() && !parsed.has("list")) {
                System.err.println("no source roots to work on");
                return 1;
            }

            Iterable<? extends HintDescription> hints;
            
            if (parsed.has("list")) {
                printHints(sourceCP, binaryCP);
                return 0;
            }

            Preferences settingsFromConfigFile;
            Preferences hintSettings;
            boolean apply;

            if (parsed.has(configFile)) {
                settingsFromConfigFile = XMLHintPreferences.from(parsed.valueOf(configFile));
                hintSettings = settingsFromConfigFile.node("settings");
                apply = settingsFromConfigFile.getBoolean("apply", false);
            } else {
                settingsFromConfigFile = null;
                hintSettings = NbPreferences.root().node("tempSettings");
                apply = false;
            }

            if (parsed.has(hint)) {
                if (settingsFromConfigFile != null) {
                    System.err.println("cannot specify --hint and --config-file together");
                    return 1;
                }
                hints = findHints(sourceCP, binaryCP, parsed.valueOf(hint), hintSettings);
            } else if (parsed.has(hintFile)) {
                if (settingsFromConfigFile != null) {
                    System.err.println("cannot specify --hint-file and --config-file together");
                    return 1;
                }
                FileObject hintFileFO = FileUtil.toFileObject(parsed.valueOf(hintFile));
                assert hintFileFO != null;
                hints = PatternConvertor.create(hintFileFO.asText());
                for (HintDescription hd : hints) {
                    HintsSettings.setEnabled(hintSettings.node(hd.getMetadata().id), true);
                }
            } else {
                hints = readHints(sourceCP, binaryCP, hintSettings, settingsFromConfigFile != null ? settingsFromConfigFile.getBoolean("runDeclarative", true) : true);
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

                Preferences prefs = hintSettings.node(hd.getMetadata().id);

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

            String sourceLevel = parsed.valueOf(source);

            if (!Pattern.compile(ACCEPTABLE_SOURCE_LEVEL_PATTERN).matcher(sourceLevel).matches()) {
                System.err.println("unrecognized source level specification: " + sourceLevel);
                return 1;
            }

            if (parsed.has(OPTION_NO_APPLY)) {
                apply = false;
            } else if (parsed.has(OPTION_APPLY)) {
                apply = true;
            }
            
            if (apply && !hints.iterator().hasNext()) {
                System.err.println("no hints specified");
                return 1;
            }

            Object[] register2Lookup = new Object[] {
                new ClassPathProviderImpl(bootCP, compileCP, sourceCP),
                new JavaPathRecognizer(),
                new SourceLevelQueryImpl(sourceCP, sourceLevel)
            };

            try {
                for (Object toRegister : register2Lookup) {
                    MainLookup.register(toRegister);
                }

                setHintPreferences(hintSettings);
                
                ProgressHandleWrapper progress = parsed.has("progress") ? new ProgressHandleWrapper(new ConsoleProgressHandleAbstraction(), 1) : new ProgressHandleWrapper(1);

                if (apply) {
                    apply(hints, rootFolders.toArray(new Folder[0]), progress, parsed.valueOf(out));
                } else {
                    findOccurrences(hints, rootFolders.toArray(new Folder[0]), progress, parsed.valueOf(out));
                }
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                for (Object toUnRegister : register2Lookup) {
                    MainLookup.unregister(toUnRegister);
                }
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

    private static void setHintPreferences(final Preferences prefs) {
        HintsSettings.setPreferencesOverride(new Map<String, Preferences>() {
            @Override public int size() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public boolean isEmpty() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public boolean containsKey(Object key) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public boolean containsValue(Object value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public Preferences get(Object key) {
                Preferences res = prefs.node((String) key);

                if (res.get("enabled", null) == null) {
                    res.putBoolean("enabled", false);
                }
                
                return res;
            }
            @Override public Preferences put(String key, Preferences value) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public Preferences remove(Object key) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public void putAll(Map<? extends String, ? extends Preferences> m) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public void clear() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public Set<String> keySet() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public Collection<Preferences> values() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override public Set<Entry<String, Preferences>> entrySet() {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    private static Map<HintMetadata, Collection<? extends HintDescription>> listHints(ClassPath sourceFrom, ClassPath binaryFrom) {
        Map<HintMetadata, Collection<? extends HintDescription>> result = new HashMap<HintMetadata, Collection<? extends HintDescription>>();

        for (Entry<HintMetadata, ? extends Collection<? extends HintDescription>> entry: RulesManager.getInstance().readHints(null, Arrays.asList(sourceFrom, binaryFrom), null).entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }
    
    private static Iterable<? extends HintDescription> findHints(ClassPath sourceFrom, ClassPath binaryFrom, String name, Preferences toEnableIn) {
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(sourceFrom, binaryFrom).entrySet()) {
            if (e.getKey().displayName.equals(name)) {
                descs.addAll(e.getValue());
                HintsSettings.setEnabled(toEnableIn.node(e.getKey().id), true);
            }
        }

        return descs;
    }

    private static Iterable<? extends HintDescription> allHints(ClassPath sourceFrom, ClassPath binaryFrom, Preferences toEnableIn) {
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(sourceFrom, binaryFrom).entrySet()) {
            if (e.getKey().kind != Kind.INSPECTION) continue;
            if (!e.getKey().enabled) continue;
            descs.addAll(e.getValue());
            HintsSettings.setEnabled(toEnableIn.node(e.getKey().id), true);
        }

        return descs;
    }

    private static Iterable<? extends HintDescription> readHints(ClassPath sourceFrom, ClassPath binaryFrom, Preferences toEnableIn, boolean declarativeEnabledByDefault) {
        Map<HintMetadata, ? extends Collection<? extends HintDescription>> hardcoded = RulesManager.getInstance().readHints(null, Arrays.<ClassPath>asList(), null);
        Map<HintMetadata, ? extends Collection<? extends HintDescription>> all = RulesManager.getInstance().readHints(null, Arrays.asList(sourceFrom, binaryFrom), null);
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, ? extends Collection<? extends HintDescription>> entry: all.entrySet()) {
            if (hardcoded.containsKey(entry.getKey())) {
                if (HintsSettings.isEnabledWithDefault(toEnableIn.node(entry.getKey().id), false)) {
                    descs.addAll(entry.getValue());
                }
            } else {
                if (HintsSettings.isEnabledWithDefault(toEnableIn.node(entry.getKey().id), declarativeEnabledByDefault)) {
                    descs.addAll(entry.getValue());
                }
            }
        }

        return descs;
    }

    private static final Logger TOP_LOGGER = Logger.getLogger("");

    private static void prepareLoggers() {
        TOP_LOGGER.setLevel(Level.OFF);
        System.setProperty("RepositoryUpdate.increasedLogLevel", "OFF");
    }
    
    private static void findOccurrences(Iterable<? extends HintDescription> descs, Folder[] sourceRoot, ProgressHandleWrapper progress, File out) throws IOException {
        final Map<String, String> id2DisplayName = new HashMap<String, String>();

        for (HintDescription hd : descs) {
            if (hd.getMetadata() != null) {
                id2DisplayName.put(hd.getMetadata().id, hd.getMetadata().displayName);
            }
        }

        ProgressHandleWrapper w = progress.startNextPartWithEmbedding(1, 1);
        BatchResult occurrences = BatchSearch.findOccurrences(descs, Scopes.specifiedFoldersScope(sourceRoot), w);

        List<MessageImpl> problems = new LinkedList<MessageImpl>();
        BatchSearch.getVerifiedSpans(occurrences, progress, new VerifiedSpansCallBack() {
            @Override public void groupStarted() {}
            @Override public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                for (ErrorDescription ed : hints) {
                    print(ed, id2DisplayName);
                }
                return true;
            }
            @Override public void groupFinished() {}
            @Override public void cannotVerifySpan(Resource r) {
                //TODO: ignored - what to do?
            }
        }, problems, new AtomicBoolean());
    }

    private static void print(ErrorDescription error, Map<String, String> id2DisplayName) throws IOException {
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

        String id = error.getId();

        if (id != null && id.startsWith("text/x-java:")) {
            id = id.substring("text/x-java:".length());
        }

        String idDisplayName = id2DisplayName.get(id);

        if (idDisplayName == null) {
            idDisplayName = "unknown";
        }

        for (Entry<String, String> remap : toIdRemap.entrySet()) {
            idDisplayName = idDisplayName.replace(remap.getKey(), remap.getValue());
        }

        idDisplayName = idDisplayName.replaceAll("[^A-Za-z0-9]", "_").replaceAll("_+", "_");

        idDisplayName = "[" + idDisplayName + "] ";

        System.out.println(FileUtil.getFileDisplayName(error.getFile()) + ":" + (lineNumber + 1) + ": warning: " + idDisplayName + error.getDescription());
        System.out.println(line);
        System.out.println(b);
    }

    private static final Map<String, String> toIdRemap = new HashMap<String, String>() {{
        put("==", "equals");
        put("!=", "not_equals");
    }};

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
                    org.netbeans.modules.jackpot30.indexing.batch.BatchUtilities.exportDiff(mr, null, outS);
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

    private static void printHints(ClassPath sourceFrom, ClassPath binaryFrom) throws IOException {
        Set<String> hints = new TreeSet<String>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(sourceFrom, binaryFrom).entrySet()) {
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

    private static void showGUICustomizer(File settingsFile, ClassPath binaryCP, ClassPath sourceCP) throws IOException, BackingStoreException {
        GlobalPathRegistry.getDefault().register(ClassPath.COMPILE, new ClassPath[] {binaryCP});
        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {sourceCP});
        ClassPathBasedHintWrapper hints = new ClassPathBasedHintWrapper();
        final Preferences p = XMLHintPreferences.from(settingsFile);
        JPanel hintPanel = new HintsPanel(p.node("settings"), hints);
        final JCheckBox runDeclarativeHints = new JCheckBox("Always Run Declarative Rules");

        runDeclarativeHints.setToolTipText("Always run the declarative rules found on classpath? (Only those selected above will be run when unchecked.)");
        runDeclarativeHints.setSelected(p.getBoolean("runDeclarative", true));
        runDeclarativeHints.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                p.putBoolean("runDeclarative", runDeclarativeHints.isSelected());
            }
        });

        JPanel customizer = new JPanel(new BorderLayout());

        customizer.add(hintPanel, BorderLayout.CENTER);
        customizer.add(runDeclarativeHints, BorderLayout.SOUTH);
        JOptionPane jop = new JOptionPane(customizer, JOptionPane.PLAIN_MESSAGE);
        JDialog dialog = jop.createDialog("Select Hints");

        jop.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();

        Object result = jop.getValue();

        if (result.equals(JOptionPane.OK_OPTION)) {
            p.flush();
        }
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
