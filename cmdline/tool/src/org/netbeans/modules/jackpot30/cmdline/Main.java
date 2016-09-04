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
import java.util.HashSet;
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
import java.util.prefs.AbstractPreferences;
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
import org.netbeans.modules.jackpot30.cmdline.lib.Utils;
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
import org.netbeans.spi.editor.hints.Severity;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.java.hints.Hint.Kind;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Pair;
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
    private static final String OPTION_FAIL_ON_WARNINGS = "fail-on-warnings";
    private static final String SOURCE_LEVEL_DEFAULT = "1.7";
    private static final String ACCEPTABLE_SOURCE_LEVEL_PATTERN = "(1\\.)?[2-9][0-9]*";
    
    public static void main(String... args) throws IOException, ClassNotFoundException {
        System.exit(compile(args));
    }

    public static int compile(String... args) throws IOException, ClassNotFoundException {
        System.setProperty("netbeans.user", "/tmp/tmp-foo");

        OptionParser parser = new OptionParser();
//        ArgumentAcceptingOptionSpec<File> projects = parser.accepts("project", "project(s) to refactor").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class);
        GroupOptions globalGroupOptions = setupGroupParser(parser);
        ArgumentAcceptingOptionSpec<File> cache = parser.accepts("cache", "a cache directory to store working data").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec<File> out = parser.accepts("out", "output diff").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec<File> configFile = parser.accepts("config-file", "configuration file").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec<String> hint = parser.accepts("hint", "hint name").withRequiredArg().ofType(String.class);
        ArgumentAcceptingOptionSpec<String> config = parser.accepts("config", "configurations").withRequiredArg().ofType(String.class);
        ArgumentAcceptingOptionSpec<File> hintFile = parser.accepts("hint-file", "file with rules that should be performed").withRequiredArg().ofType(File.class);
        ArgumentAcceptingOptionSpec<String> group = parser.accepts("group", "specify roots to process alongside with their classpath").withRequiredArg().ofType(String.class);

        parser.accepts("list", "list all known hints");
        parser.accepts("progress", "show progress");
        parser.accepts("debug", "enable debugging loggers");
        parser.accepts("help", "prints this help");
        parser.accepts(OPTION_NO_APPLY, "do not apply changes - only print locations were the hint would be applied");
        parser.accepts(OPTION_APPLY, "apply changes");
        parser.accepts("show-gui", "show configuration dialog");
        parser.accepts(OPTION_FAIL_ON_WARNINGS, "fail when warnings are detected");

        OptionSet parsed;

        try {
            parsed = parser.parse(args);
        } catch (OptionException ex) {
            System.err.println(ex.getLocalizedMessage());
            parser.printHelpOn(System.out);
            return 1;
        }

        if (!parsed.has("debug")) {
            prepareLoggers();
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

        final List<RootConfiguration> groups = new ArrayList<>();

        groups.add(new RootConfiguration(parsed, globalGroupOptions));

        for (String groupValue : parsed.valuesOf(group)) {
            OptionParser groupParser = new OptionParser();
            GroupOptions groupOptions = setupGroupParser(groupParser);
            OptionSet parsedGroup = groupParser.parse(splitGroupArg(groupValue));

            groups.add(new RootConfiguration(parsedGroup, groupOptions));
        }

        if (parsed.has("show-gui")) {
            if (parsed.has(configFile)) {
                final File settingsFile = parsed.valueOf(configFile);
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        @Override public void run() {
                            try {
                                Pair<ClassPath, ClassPath> sourceAndBinaryCP = jointSourceAndBinaryCP(groups);
                                showGUICustomizer(settingsFile, sourceAndBinaryCP.second(), sourceAndBinaryCP.first());
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

            if (parsed.has("list")) {
                Pair<ClassPath, ClassPath> sourceAndBinaryCP = jointSourceAndBinaryCP(groups);
                printHints(sourceAndBinaryCP.first(),
                           sourceAndBinaryCP.second());
                return 0;
            }

            int totalGroups = 0;

            for (RootConfiguration groupConfig : groups) {
                if (!groupConfig.rootFolders.isEmpty()) totalGroups++;
            }

            System.err.println("totalGroups=" + totalGroups);

            ProgressHandleWrapper progress = parsed.has("progress") ? new ProgressHandleWrapper(new ConsoleProgressHandleAbstraction(), ProgressHandleWrapper.prepareParts(totalGroups)) : new ProgressHandleWrapper(1);

            Preferences hintSettingsPreferences;
            boolean apply;
            boolean runDeclarative;

            if (parsed.has(configFile)) {
                Preferences settingsFromConfigFile;
                settingsFromConfigFile = XMLHintPreferences.from(parsed.valueOf(configFile));
                hintSettingsPreferences = settingsFromConfigFile.node("settings");
                apply = settingsFromConfigFile.getBoolean("apply", false);
                runDeclarative = settingsFromConfigFile.getBoolean("runDeclarative", true);
                if (parsed.has(hint)) {
                    System.err.println("cannot specify --hint and --config-file together");
                    return 1;
                } else if (parsed.has(hintFile)) {
                    System.err.println("cannot specify --hint-file and --config-file together");
                    return 1;
                }
            } else {
                hintSettingsPreferences = null;
                apply = false;
                runDeclarative = true;
            }

            if (parsed.has(config) && !parsed.has(hint)) {
                System.err.println("--config cannot specified when no hint is specified");
                return 1;
            }

            if (parsed.has(OPTION_NO_APPLY)) {
                apply = false;
            } else if (parsed.has(OPTION_APPLY)) {
                apply = true;
            }

            GroupResult result = GroupResult.NOTHING_TO_DO;

            try (Writer outS = parsed.has(out) ? new BufferedWriter(new OutputStreamWriter(new FileOutputStream(parsed.valueOf(out)))) : null) {
                GlobalConfiguration globalConfig = new GlobalConfiguration(hintSettingsPreferences, apply, runDeclarative, parsed.valueOf(hint), parsed.valueOf(hintFile), outS, parsed.has(OPTION_FAIL_ON_WARNINGS));

                for (RootConfiguration groupConfig : groups) {
                    result = result.join(handleGroup(groupConfig, progress, globalConfig, parsed.valuesOf(config)));
                }
            }

            progress.finish();

            if (result == GroupResult.NOTHING_TO_DO) {
                System.err.println("no source roots to work on");
                return 1;
            }

            return result == GroupResult.SUCCESS ? 0 : 1;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        } finally {
            if (deleteCacheDir) {
                FileObject cacheDirFO = FileUtil.toFileObject(cacheDir);

                if (cacheDirFO != null) {
                    //TODO: would be better to do j.i.File.delete():
                    cacheDirFO.delete();
                }
            }
        }
    }

    private static Pair<ClassPath, ClassPath> jointSourceAndBinaryCP(List<RootConfiguration> groups) {
        Set<FileObject> sourceRoots = new HashSet<>();
        Set<FileObject> binaryRoots = new HashSet<>();
        for (RootConfiguration groupConfig : groups) {
            sourceRoots.addAll(Arrays.asList(groupConfig.sourceCP.getRoots()));
            binaryRoots.addAll(Arrays.asList(groupConfig.binaryCP.getRoots()));
        }
        return Pair.of(ClassPathSupport.createClassPath(sourceRoots.toArray(new FileObject[0])),
                       ClassPathSupport.createClassPath(binaryRoots.toArray(new FileObject[0])));
    }

    private static GroupOptions setupGroupParser(OptionParser parser) {
        return new GroupOptions(parser.accepts("classpath", "classpath").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class),
                                parser.accepts("bootclasspath", "bootclasspath").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class),
                                parser.accepts("sourcepath", "sourcepath").withRequiredArg().withValuesSeparatedBy(File.pathSeparatorChar).ofType(File.class),
                                parser.accepts("source", "source level").withRequiredArg().ofType(String.class).defaultsTo(SOURCE_LEVEL_DEFAULT));
    }

    private static final class GroupOptions {
        private final ArgumentAcceptingOptionSpec<File> classpath;
        private final ArgumentAcceptingOptionSpec<File> bootclasspath;
        private final ArgumentAcceptingOptionSpec<File> sourcepath;
        private final ArgumentAcceptingOptionSpec<String> source;

        public GroupOptions(ArgumentAcceptingOptionSpec<File> classpath, ArgumentAcceptingOptionSpec<File> bootclasspath, ArgumentAcceptingOptionSpec<File> sourcepath, ArgumentAcceptingOptionSpec<String> source) {
            this.classpath = classpath;
            this.bootclasspath = bootclasspath;
            this.sourcepath = sourcepath;
            this.source = source;
        }

    }

    private static Map<HintMetadata, Collection<? extends HintDescription>> listHints(ClassPath sourceFrom, ClassPath binaryFrom) {
        Map<HintMetadata, Collection<? extends HintDescription>> result = new HashMap<HintMetadata, Collection<? extends HintDescription>>();

        for (Entry<HintMetadata, ? extends Collection<? extends HintDescription>> entry: RulesManager.getInstance().readHints(null, Arrays.asList(sourceFrom, binaryFrom), null).entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    private static GroupResult handleGroup(RootConfiguration rootConfiguration, ProgressHandleWrapper w, GlobalConfiguration globalConfig, List<String> config) throws IOException {
        Iterable<? extends HintDescription> hints;

        if (rootConfiguration.rootFolders.isEmpty()) {
            return GroupResult.NOTHING_TO_DO;
        }

        ProgressHandleWrapper progress = w.startNextPartWithEmbedding(1);
        Preferences settings = globalConfig.configurationPreferences != null ? globalConfig.configurationPreferences : new MemoryPreferences();
        HintsSettings hintSettings = HintsSettings.createPreferencesBasedHintsSettings(settings, false, null);

        if (globalConfig.hint != null) {
            hints = findHints(rootConfiguration.sourceCP, rootConfiguration.binaryCP, globalConfig.hint, hintSettings);
        } else if (globalConfig.hintFile != null) {
            FileObject hintFileFO = FileUtil.toFileObject(globalConfig.hintFile);
            assert hintFileFO != null;
            hints = PatternConvertor.create(hintFileFO.asText());
            for (HintDescription hd : hints) {
                hintSettings.setEnabled(hd.getMetadata(), true);
            }
        } else {
            hints = readHints(rootConfiguration.sourceCP, rootConfiguration.binaryCP, hintSettings, settings, globalConfig.runDeclarative);
        }

        if (config != null && !config.isEmpty()) {
            Iterator<? extends HintDescription> hit = hints.iterator();
            HintDescription hd = hit.next();

            if (hit.hasNext()) {
                System.err.println("--config cannot specified when more than one hint is specified");

                return GroupResult.FAILURE;
            }

            Preferences prefs = hintSettings.getHintPreferences(hd.getMetadata());

            boolean stop = false;

            for (String c : config) {
                int assign = c.indexOf('=');

                if (assign == (-1)) {
                    System.err.println("configuration option is missing '=' (" + c + ")");
                    stop = true;
                    continue;
                }

                prefs.put(c.substring(0, assign), c.substring(assign + 1));
            }

            if (stop) {
                return GroupResult.FAILURE;
            }
        }

        String sourceLevel = rootConfiguration.sourceLevel;

        if (!Pattern.compile(ACCEPTABLE_SOURCE_LEVEL_PATTERN).matcher(sourceLevel).matches()) {
            System.err.println("unrecognized source level specification: " + sourceLevel);
            return GroupResult.FAILURE;
        }

        if (globalConfig.apply && !hints.iterator().hasNext()) {
            System.err.println("no hints specified");
            return GroupResult.FAILURE;
        }

        Object[] register2Lookup = new Object[] {
            new ClassPathProviderImpl(rootConfiguration.bootCP, rootConfiguration.compileCP, rootConfiguration.sourceCP),
            new JavaPathRecognizer(),
            new SourceLevelQueryImpl(rootConfiguration.sourceCP, sourceLevel)
        };

        try {
            for (Object toRegister : register2Lookup) {
                MainLookup.register(toRegister);
            }

            if (globalConfig.apply) {
                apply(hints, rootConfiguration.rootFolders.toArray(new Folder[0]), progress, hintSettings, globalConfig.out);

                return GroupResult.SUCCESS;
            } else {
                WarningsAndErrors wae = new WarningsAndErrors();

                findOccurrences(hints, rootConfiguration.rootFolders.toArray(new Folder[0]), progress, hintSettings, wae);

                if (wae.errors != 0 || (wae.warnings != 0 && globalConfig.failOnWarnings)) {
                    return GroupResult.FAILURE;
                } else {
                    return GroupResult.SUCCESS;
                }
            }
        } finally {
            for (Object toUnRegister : register2Lookup) {
                MainLookup.unregister(toUnRegister);
            }
        }
    }

    private static class MemoryPreferences extends AbstractPreferences {

        private final Map<String, String> values = new HashMap<>();
        private final Map<String, MemoryPreferences> nodes = new HashMap<>();

        public MemoryPreferences() {
            this(null, "");
        }

        public MemoryPreferences(MemoryPreferences parent, String name) {
            super(parent, name);
        }
        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() throws BackingStoreException {
            ((MemoryPreferences) parent()).nodes.remove(name());
        }

        @Override
        protected String[] keysSpi() throws BackingStoreException {
            return values.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi() throws BackingStoreException {
            return nodes.keySet().toArray(new String[0]);
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            MemoryPreferences result = nodes.get(name);

            if (result == null) {
                nodes.put(name, result = new MemoryPreferences(this, name));
            }

            return result;
        }

        @Override
        protected void syncSpi() throws BackingStoreException {
        }

        @Override
        protected void flushSpi() throws BackingStoreException {
        }
    }

    private enum GroupResult {
        NOTHING_TO_DO {
            @Override
            public GroupResult join(GroupResult other) {
                return other;
            }
        },
        SUCCESS {
            @Override
            public GroupResult join(GroupResult other) {
                if (other == FAILURE) return other;
                return this;
            }
        },
        FAILURE {
            @Override
            public GroupResult join(GroupResult other) {
                return this;
            }
        };

        public abstract GroupResult join(GroupResult other);
    }
    
    private static Iterable<? extends HintDescription> findHints(ClassPath sourceFrom, ClassPath binaryFrom, String name, HintsSettings toEnableIn) {
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(sourceFrom, binaryFrom).entrySet()) {
            if (e.getKey().displayName.equals(name)) {
                descs.addAll(e.getValue());
                toEnableIn.setEnabled(e.getKey(), true);
            }
        }

        return descs;
    }

    private static Iterable<? extends HintDescription> allHints(ClassPath sourceFrom, ClassPath binaryFrom, HintsSettings toEnableIn) {
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, Collection<? extends HintDescription>> e : listHints(sourceFrom, binaryFrom).entrySet()) {
            if (e.getKey().kind != Kind.INSPECTION) continue;
            if (!e.getKey().enabled) continue;
            descs.addAll(e.getValue());
            toEnableIn.setEnabled(e.getKey(), true);
        }

        return descs;
    }

    private static Iterable<? extends HintDescription> readHints(ClassPath sourceFrom, ClassPath binaryFrom, HintsSettings toEnableIn, Preferences toEnableInPreferencesHack, boolean declarativeEnabledByDefault) {
        Map<HintMetadata, ? extends Collection<? extends HintDescription>> hardcoded = RulesManager.getInstance().readHints(null, Arrays.<ClassPath>asList(), null);
        Map<HintMetadata, ? extends Collection<? extends HintDescription>> all = RulesManager.getInstance().readHints(null, Arrays.asList(sourceFrom, binaryFrom), null);
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Entry<HintMetadata, ? extends Collection<? extends HintDescription>> entry: all.entrySet()) {
            if (hardcoded.containsKey(entry.getKey())) {
                if (toEnableIn.isEnabled(entry.getKey())) {
                    descs.addAll(entry.getValue());
                }
            } else {
                if (/*XXX: hack*/toEnableInPreferencesHack.node(entry.getKey().id).getBoolean("enabled", declarativeEnabledByDefault)) {
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
    
    private static void findOccurrences(Iterable<? extends HintDescription> descs, Folder[] sourceRoot, ProgressHandleWrapper progress, HintsSettings settings, final WarningsAndErrors wae) throws IOException {
        final Map<String, String> id2DisplayName = Utils.computeId2DisplayName(descs);
        ProgressHandleWrapper w = progress.startNextPartWithEmbedding(1, 1);
        BatchResult occurrences = BatchSearch.findOccurrences(descs, Scopes.specifiedFoldersScope(sourceRoot), w, settings);

        List<MessageImpl> problems = new LinkedList<MessageImpl>();
        BatchSearch.getVerifiedSpans(occurrences, w, new VerifiedSpansCallBack() {
            @Override public void groupStarted() {}
            @Override public boolean spansVerified(CompilationController wc, Resource r, Collection<? extends ErrorDescription> hints) throws Exception {
                for (ErrorDescription ed : hints) {
                    print(ed, wae, id2DisplayName);
                }
                return true;
            }
            @Override public void groupFinished() {}
            @Override public void cannotVerifySpan(Resource r) {
                //TODO: ignored - what to do?
            }
        }, problems, new AtomicBoolean());
    }

    private static void print(ErrorDescription error, WarningsAndErrors wae, Map<String, String> id2DisplayName) throws IOException {
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

        String idDisplayName = Utils.categoryName(error.getId(), id2DisplayName);
        String severity;
        if (error.getSeverity() == Severity.ERROR) {
            severity = "error";
            wae.errors++;
        } else {
            severity = "warning";
            wae.warnings++;
        }
        System.out.println(FileUtil.getFileDisplayName(error.getFile()) + ":" + (lineNumber + 1) + ": " + severity + ": " + idDisplayName + error.getDescription());
        System.out.println(line);
        System.out.println(b);
    }

    private static void apply(Iterable<? extends HintDescription> descs, Folder[] sourceRoot, ProgressHandleWrapper progress, HintsSettings settings, Writer out) throws IOException {
        ProgressHandleWrapper w = progress.startNextPartWithEmbedding(1, 1);
        BatchResult occurrences = BatchSearch.findOccurrences(descs, Scopes.specifiedFoldersScope(sourceRoot), w, settings);

        List<MessageImpl> problems = new LinkedList<MessageImpl>();
        Collection<ModificationResult> diffs = BatchUtilities.applyFixes(occurrences, w, new AtomicBoolean(), problems);

        if (out != null) {
            for (ModificationResult mr : diffs) {
                org.netbeans.modules.jackpot30.indexing.batch.BatchUtilities.exportDiff(mr, null, out);
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
        JPanel hintPanel = new HintsPanel(p.node("settings"), hints, true);
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

    static String[] splitGroupArg(String arg) {
        List<String> result = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();

        for (int i = 0; i < arg.length(); i++) {
            switch (arg.charAt(i)) {
                case '\\':
                    if (++i < arg.length()) {
                        currentPart.append(arg.charAt(i));
                    }
                    break;
                case ' ':
                    if (currentPart.length() > 0) {
                        result.add(currentPart.toString());
                        currentPart.delete(0, currentPart.length());
                    }
                    break;
                default:
                    currentPart.append(arg.charAt(i));
                    break;
            }
        }

        if (currentPart.length() > 0) {
            result.add(currentPart.toString());
        }

        return result.toArray(new String[0]);
    }

    private static final class WarningsAndErrors {
        private int warnings;
        private int errors;
    }

    private static final class RootConfiguration {
        private final List<Folder> rootFolders;
        private final ClassPath bootCP;
        private final ClassPath compileCP;
        private final ClassPath sourceCP;
        private final ClassPath binaryCP;
        private final String    sourceLevel;

        public RootConfiguration(OptionSet parsed, GroupOptions groupOptions) throws IOException {
            this.rootFolders = new ArrayList<>();

            List<FileObject> roots = new ArrayList<>();

            for (String sr : parsed.nonOptionArguments()) {
                File r = new File(sr);
                FileObject root = FileUtil.toFileObject(r);

                if (root != null) {
                    roots.add(root);
                    rootFolders.add(new Folder(root));
                }
            }

            this.bootCP = createClassPath(parsed.has(groupOptions.bootclasspath) ? parsed.valuesOf(groupOptions.bootclasspath) : null, createDefaultBootClassPath());
            this.compileCP = createClassPath(parsed.has(groupOptions.classpath) ? parsed.valuesOf(groupOptions.classpath) : null, ClassPath.EMPTY);
            this.sourceCP = createClassPath(parsed.has(groupOptions.sourcepath) ? parsed.valuesOf(groupOptions.sourcepath) : null, ClassPathSupport.createClassPath(roots.toArray(new FileObject[0])));
            this.binaryCP = ClassPathSupport.createProxyClassPath(bootCP, compileCP);
            this.sourceLevel = parsed.valueOf(groupOptions.source);
        }

    }

    private static final class GlobalConfiguration {
        private final Preferences configurationPreferences;
        private final boolean apply;
        private final boolean runDeclarative;
        private final String hint;
        private final File hintFile;
        private final Writer out;
        private final boolean failOnWarnings;

        public GlobalConfiguration(Preferences configurationPreferences, boolean apply, boolean runDeclarative, String hint, File hintFile, Writer out, boolean failOnWarnings) {
            this.configurationPreferences = configurationPreferences;
            this.apply = apply;
            this.runDeclarative = runDeclarative;
            this.hint = hint;
            this.hintFile = hintFile;
            this.out = out;
            this.failOnWarnings = failOnWarnings;
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

        private final int width = 80 - 2;

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
        public synchronized void finish() {
            current = total;
            RequestProcessor.getDefault().post(new Runnable() {
                @Override
                public void run() {
                    doUpdate(false);
                    System.out.println();
                }
            });
        }

        private void update() {
            RequestProcessor.getDefault().post(new Runnable() {
                @Override
                public void run() {
                    doUpdate(true);
                }
            });
        }

        private int currentShownDone = -1;

        private void doUpdate(boolean moveCaret) {
            int done;

            synchronized(this) {
                done = (int) ((((double) width) / total) * current);

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

            pw.print("]");

            if (moveCaret)
                pw.print("\r");
        }

    }

}
