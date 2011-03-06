/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.file;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.file.DeclarativeCondition.Instanceof;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.FixTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.Result;
import org.netbeans.modules.jackpot30.file.conditionapi.Context;
import org.netbeans.modules.jackpot30.spi.ClassPathBasedHintProvider;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintContext.MessageKind;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Acceptor;
import org.netbeans.modules.jackpot30.spi.HintDescription.AdditionalQueryConstraints;
import org.netbeans.modules.jackpot30.spi.HintDescription.Condition;
import org.netbeans.modules.jackpot30.spi.HintDescription.DeclarativeFixDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescriptionFactory;
import org.netbeans.modules.jackpot30.spi.HintMetadata;
import org.netbeans.modules.jackpot30.spi.HintMetadata.HintSeverity;
import org.netbeans.modules.jackpot30.spi.HintProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
@ServiceProvider(service=HintProvider.class)
public class DeclarativeHintRegistry implements HintProvider, ClassPathBasedHintProvider {

    public Map<HintMetadata, Collection<? extends HintDescription>> computeHints() {
        return readHints(findGlobalFiles());
    }

    public Collection<? extends HintDescription> computeHints(ClassPath cp) {
        return join(readHints(findFiles(cp)));
    }

    public static Collection<? extends HintDescription> join(Map<HintMetadata, Collection<? extends HintDescription>> hints) {
        List<HintDescription> descs = new LinkedList<HintDescription>();

        for (Collection<? extends HintDescription> c : hints.values()) {
            descs.addAll(c);
        }

        return descs;
    }

    private Map<HintMetadata, Collection<? extends HintDescription>> readHints(Iterable<? extends FileObject> files) {
        Map<HintMetadata, Collection<? extends HintDescription>> result = new HashMap<HintMetadata, Collection<? extends HintDescription>>();

        for (FileObject f : files) {
            result.putAll(parseHintFile(f));
        }

        return result;
    }

    public static Collection<? extends FileObject> findAllFiles() {
        List<FileObject> files = new LinkedList<FileObject>();

        files.addAll(findGlobalFiles());
        files.addAll(findFiles(GlobalPathRegistry.getDefault().getPaths(ClassPath.BOOT)));
        files.addAll(findFiles(GlobalPathRegistry.getDefault().getPaths(ClassPath.COMPILE)));
        files.addAll(findFiles(GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE)));

        return files;
    }

    private static Collection<? extends FileObject> findFiles(Iterable<? extends ClassPath> cps) {
        List<FileObject> result = new LinkedList<FileObject>();

        for (ClassPath cp : cps) {
            result.addAll(findFiles(cp));
        }

        return result;
    }

    private static Collection<? extends FileObject> findFiles(ClassPath cp) {
        List<FileObject> result = new LinkedList<FileObject>();

        for (FileObject folder : cp.findAllResources("META-INF/upgrade")) {
            result.addAll(findFiles(folder));
        }

        return result;
    }

    private static Collection<? extends FileObject> findGlobalFiles() {
        FileObject folder = FileUtil.getConfigFile("org-netbeans-modules-java-hints/declarative");

        if (folder == null) {
            return Collections.emptyList();
        }

        return findFilesRecursive(folder);
    }

    private static Collection<? extends FileObject> findFiles(FileObject folder) {
        List<FileObject> result = new LinkedList<FileObject>();

        for (FileObject f : folder.getChildren()) {
            if (!"hint".equals(f.getExt())) {
                continue;
            }
            result.add(f);
        }

        return result;
    }

    private static Collection<? extends FileObject> findFilesRecursive(FileObject folder) {
        List<FileObject> todo = new LinkedList<FileObject>();
        List<FileObject> result = new LinkedList<FileObject>();

        todo.add(folder);

        while (!todo.isEmpty()) {
            FileObject f = todo.remove(0);

            if (f.isFolder()) {
                todo.addAll(Arrays.asList(f.getChildren()));
                continue;
            }
            if (!"hint".equals(f.getExt())) {
                continue;
            }
            result.add(f);
        }

        return result;
    }

    public static Map<HintMetadata, Collection<? extends HintDescription>> parseHintFile(@NonNull FileObject file) {
        String spec = Utilities.readFile(file);

        return spec != null ? parseHints(file, spec) : Collections.<HintMetadata, Collection<? extends HintDescription>>emptyMap();
    }

    public static Map<HintMetadata, Collection<? extends HintDescription>> parseHints(@NullAllowed FileObject file, String spec) {
        ResourceBundle bundle;

        try {
            if (file != null) {
                ClassLoader l = new URLClassLoader(new URL[] {file.getParent().getURL()});

                bundle = NbBundle.getBundle("Bundle", Locale.getDefault(), l);
            } else {
                bundle = null;
            }
        } catch (FileStateInvalidException ex) {
            bundle = null;
        } catch (MissingResourceException ex) {
            //TODO: log?
            bundle = null;
        }

        TokenHierarchy<?> h = TokenHierarchy.create(spec, DeclarativeHintTokenId.language());
        TokenSequence<DeclarativeHintTokenId> ts = h.tokenSequence(DeclarativeHintTokenId.language());
        Map<HintMetadata, Collection<HintDescription>> result = new LinkedHashMap<HintMetadata, Collection<HintDescription>>();
        Result parsed = new DeclarativeHintsParser().parse(file, spec, ts);

        HintMetadata meta;
        String primarySuppressWarningsKey;
        String id = parsed.options.get("hint");

        if (id != null) {
            String cat = parsed.options.get("hint-category");

            if (cat == null) {
                cat = "general";
            }

            String[] w = suppressWarnings(parsed.options);

            meta = HintMetadata.create(id, bundle, cat, true, HintSeverity.WARNING, null, w);
            primarySuppressWarningsKey = w.length > 0 ? w[0] : null;
        } else {
            meta = null;
            primarySuppressWarningsKey = null;
        }

        int count = 0;

        for (HintTextDescription hint : parsed.hints) {
            HintDescriptionFactory f = HintDescriptionFactory.create();
            String displayName = resolveDisplayName(file, bundle, hint.displayName, true, "TODO: No display name");

            Map<String, String> constraints = new HashMap<String, String>();

            for (Condition c : hint.conditions) {
                if (!(c instanceof Instanceof) || ((Instanceof) c).not)
                    continue;

                Instanceof i = (Instanceof) c;

                constraints.put(i.variable, i.constraint.trim()); //TODO: may i.constraint contain comments? if so, they need to be removed
            }

            String imports = parsed.importsBlock != null ? spec.substring(parsed.importsBlock[0], parsed.importsBlock[1]) : "";

            f = f.setTriggerPattern(PatternDescription.create(spec.substring(hint.textStart, hint.textEnd), constraints, imports));

            List<DeclarativeFixDescription> fixes = new LinkedList<DeclarativeFixDescription>();

            for (FixTextDescription fix : hint.fixes) {
                int[] fixRange = fix.fixSpan;
                String fixDisplayName = resolveDisplayName(file, bundle, fix.displayName, false, null);
                Map<String, String> options = new HashMap<String, String>(parsed.options);

                options.putAll(fix.options);

                //XXX:
//                fixes.add(DeclarativeFix.create(fixDisplayName, spec.substring(fixRange[0], fixRange[1]), fix.conditions, options));
                fixes.add(new DeclarativeFixDescription(fix.conditions, new HintsFixAcceptor(fix.conditions, fix.options), spec.substring(fixRange[0], fixRange[1])));
            }

            HintMetadata currentMeta = meta;

            if (currentMeta == null) {
                String[] w = suppressWarnings(hint.options);
                String currentId = hint.options.get("hint");
                String cat = parsed.options.get("hint-category");

                if (cat == null) {
                    cat = "general";
                }

                if (currentId != null) {
                    currentMeta = HintMetadata.create(currentId, bundle, cat, true, HintSeverity.WARNING, null, w);
                } else {
                    currentId = file != null ? file.getNameExt() + "-" + count : String.valueOf(count);
                    currentMeta = HintMetadata.create(currentId, displayName, "No Description", cat, true, HintMetadata.Kind.HINT, HintSeverity.WARNING, null, Arrays.asList(w));
                }

                primarySuppressWarningsKey = w.length > 0 ? w[0] : null;
            }

            Map<String, String> options = new HashMap<String, String>(parsed.options);

            options.putAll(hint.options);

            //XXX:
//            f = f.setWorker(new DeclarativeHintsWorker(displayName, hint.conditions, imports, fixes, options, primarySuppressWarningsKey));
            f = f.setWorker(new HintDescription.MarksWorker(hint.conditions, new HintsFixAcceptor(hint.conditions, hint.options), fixes));
            f = f.setMetadata(currentMeta);
            f = f.setAdditionalConstraints(new AdditionalQueryConstraints(new HashSet<String>(constraints.values())));

            Collection<HintDescription> hints = result.get(currentMeta);

            if (hints == null) {
                result.put(currentMeta, hints = new LinkedList<HintDescription>());
            }

            hints.add(f.produce());

            count++;
        }

        return new LinkedHashMap<HintMetadata, Collection<? extends HintDescription>>(result);
    }

    private static String[] suppressWarnings(Map<String, String> options) {
        String suppressWarnings = options.get("suppress-warnings");

        if (suppressWarnings != null) {
            return suppressWarnings.split(",");
        } else {
            return new String[0];
        }
    }

    private static @NonNull String resolveDisplayName(@NonNull FileObject hintFile, @NullAllowed ResourceBundle bundle, String displayNameSpec, boolean fallbackToFileName, String def) {
        if (bundle != null) {
            if (displayNameSpec == null) {
                if (!fallbackToFileName) {
                    return def;
                }

                String dnKey = "DN_" + hintFile.getName();
                try {
                    return bundle.getString(dnKey);
                } catch (MissingResourceException e) {
                    Logger.getLogger(DeclarativeHintRegistry.class.getName()).log(Level.FINE, null, e);
                    return fileDefaultDisplayName(hintFile, def);
                }
            }

            if (displayNameSpec.startsWith("#")) {
                String dnKey = "DN_" + displayNameSpec.substring(1);
                try {
                    return bundle.getString(dnKey);
                } catch (MissingResourceException e) {
                    Logger.getLogger(DeclarativeHintRegistry.class.getName()).log(Level.FINE, null, e);
                    return "XXX: missing display name key in the bundle (key=" + dnKey + ")";
                }
            }
        }

        return displayNameSpec != null ? displayNameSpec
                                       : fallbackToFileName ? fileDefaultDisplayName(hintFile, def)
                                                            : def;
    }

    private static @NonNull String fileDefaultDisplayName(@NullAllowed FileObject hintFile, String def) {
        if (hintFile == null) {
            return def;
        }

        return hintFile.getName();
    }

    private static void reportErrorWarning(HintContext ctx, Map<String, String> options) {
        String errorText = options.get("error");

        if (errorText != null)  {
            ctx.reportMessage(MessageKind.ERROR, errorText);
        }

        String warningText = options.get("warning");

        if (warningText != null)  {
            ctx.reportMessage(MessageKind.WARNING, warningText);
        }
    }
    
    private static final class HintsFixAcceptor implements Acceptor {

        private final List<Condition> conditions;
        private final Map<String, String> options;

        public HintsFixAcceptor(List<Condition> conditions, Map<String, String> options) {
            this.conditions = conditions;
            this.options = options;
        }

        public boolean accept(HintContext ctx) {
//            for (Condition c : conditions) {
//                if (!c.holds(ctx)) {
//                    return false;
//                }
//            }

            reportErrorWarning(ctx, options);

            return true;
        }
    }

}
