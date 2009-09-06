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
import java.util.Map.Entry;
import org.netbeans.api.java.classpath.ClassPath;
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
        FileObject folder = FileUtil.getConfigFile("org-netbeans-modules-java-hints/declarative");

        if (folder == null) {
            return Collections.emptyList();
        }

        List<HintDescription> result = new LinkedList<HintDescription>();
        
        readHintsFromFolder(folder, result);

        return result;
    }

    public Collection<? extends HintDescription> computeHints(ClassPath cp) {
        List<HintDescription> result = new LinkedList<HintDescription>();
        
        for (FileObject folder : cp.findAllResources("META-INF/upgrade")) {
            readHintsFromFolder(folder, result);
        }

        return result;
    }

    private void readHintsFromFolder(FileObject folder, List<HintDescription> result) {
        for (FileObject f : folder.getChildren()) {
            if (!"hint".equals(f.getExt())) {
                continue;
            }
            result.addAll(parseHintFile(f));
        }
    }
    
    public static List<HintDescription> parseHintFile(FileObject file) {
        StringBuilder sb = new StringBuilder();

        Reader r = null;
        
        try {
            r = new InputStreamReader(file.getInputStream(), "UTF-8");

            int read;

            while ((read = r.read()) != (-1)) {
                sb.append((char) read);
            }

            return parseHints(sb.toString());
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return Collections.emptyList();
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    public static List<HintDescription> parseHints(String spec) {
        TokenHierarchy<?> h = TokenHierarchy.create(spec, DeclarativeHintTokenId.language());
        TokenSequence<DeclarativeHintTokenId> ts = h.tokenSequence(DeclarativeHintTokenId.language());
        List<HintDescription> result = new LinkedList<HintDescription>();
        Result parsed = new DeclarativeHintsParser().parse(spec, ts);

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
