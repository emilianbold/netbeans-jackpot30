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

package org.netbeans.modules.jackpot30.file;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.file.Condition.Instanceof;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.FixTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.Result;
import org.netbeans.modules.jackpot30.spi.ClassPathBasedHintProvider;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescriptionFactory;
import org.netbeans.modules.jackpot30.spi.HintProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
@ServiceProvider(service=HintProvider.class)
public class DeclarativeHintRegistry implements HintProvider, ClassPathBasedHintProvider {

    public Collection<? extends HintDescription> computeHints() {
        return readHints(findGlobalFiles());
    }

    public Collection<? extends HintDescription> computeHints(ClassPath cp) {
        return readHints(findFiles(cp));
    }

    private List<HintDescription> readHints(Iterable<? extends FileObject> files) {
        List<HintDescription> result = new LinkedList<HintDescription>();

        for (FileObject f : files) {
            result.addAll(parseHintFile(f));
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

        return findFiles(folder);
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

    public static List<HintDescription> parseHintFile(@NonNull FileObject file) {
        String spec = Utilities.readFile(file);

        return spec != null ? parseHints(file, spec) : Collections.<HintDescription>emptyList();
    }

    public static List<HintDescription> parseHints(@NullAllowed FileObject file, String spec) {
        TokenHierarchy<?> h = TokenHierarchy.create(spec, DeclarativeHintTokenId.language());
        TokenSequence<DeclarativeHintTokenId> ts = h.tokenSequence(DeclarativeHintTokenId.language());
        List<HintDescription> result = new LinkedList<HintDescription>();
        Result parsed = new DeclarativeHintsParser().parse(file, spec, ts);

        for (HintTextDescription hint : parsed.hints) {
            HintDescriptionFactory f = HintDescriptionFactory.create();
            String displayName;

            if (hint.displayName != null) {
                displayName = hint.displayName;
            } else {
                displayName = "TODO: No display name";
            }

            Map<String, String> constraints = new HashMap<String, String>();
            
            for (Condition c : hint.conditions) {
                if (!(c instanceof Instanceof) || c.not)
                    continue;

                Instanceof i = (Instanceof) c;

                constraints.put(i.variable, i.constraint);
            }

            String imports = parsed.importsBlock != null ? spec.substring(parsed.importsBlock[0], parsed.importsBlock[1]) : "";
            
            f = f.setTriggerPattern(PatternDescription.create(spec.substring(hint.textStart, hint.textEnd), constraints, imports));

            List<DeclarativeFix> fixes = new LinkedList<DeclarativeFix>();

            for (FixTextDescription fix : hint.fixes) {
                int[] fixRange = fix.fixSpan;
                fixes.add(DeclarativeFix.create(null, spec.substring(fixRange[0], fixRange[1]), fix.conditions, fix.options));
            }

            String suppressWarnings = hint.options.get("suppress-warnings");
            String primarySuppressWarningsKey = null;

            if (suppressWarnings != null) {
                String[] keys = suppressWarnings.split(",");
                
                f.addSuppressWarningsKeys(keys);
                primarySuppressWarningsKey = keys[0];
            }

            f = f.setWorker(new DeclarativeHintsWorker(displayName, hint.conditions, imports, fixes, hint.options, primarySuppressWarningsKey));
            f = f.setDisplayName(displayName);

            result.add(f.produce());
        }

        return result;
    }

}
