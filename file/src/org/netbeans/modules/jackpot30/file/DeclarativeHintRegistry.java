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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
@ServiceProvider(service=HintProvider.class)
public class DeclarativeHintRegistry implements HintProvider {

    public Collection<? extends HintDescription> computeHints() {
        FileObject folder = Repository.getDefault().getDefaultFileSystem().getRoot().getFileObject("org-netbeans-modules-java-hints/declarative");

        if (folder == null) {
            return Collections.emptyList();
        }

        List<HintDescription> result = new LinkedList<HintDescription>();

        for (FileObject f : folder.getChildren()) {
            if (!"hint".equals(f.getExt())) {
                continue;
            }

            result.addAll(parseHintFile(f));
        }

        return result;
    }

    private static List<HintDescription> parseHintFile(FileObject file) {
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

    private static List<HintDescription> parseHints(String spec) {
        String[] split = spec.split(";;");
        List<HintDescription> result = new LinkedList<HintDescription>();

        for (String s : split) {
            s = s.trim();

            if (s.length() > 0) {
                result.add(parse(s));
            }
        }

        return result;
    }

    static HintDescription parse(String spec) {
        List<String> split = Arrays.asList(spec.split("=>"));

        assert split.size() >= 1;

        List<DeclarativeFix> fixes = new LinkedList<DeclarativeFix>();

        for (String s : split.subList(1, split.size())) {
            fixes.add(DeclarativeFix.parse(s));
        }

        String[] s = splitNameAndPattern(split.get(0));

        Map<String, String> constraints = new HashMap<String, String>();
        String pattern = parseOutTypesFromPattern(s[1], constraints);

        return HintDescription.create(PatternDescription.create(pattern, constraints), new DeclarativeHintsWorker(s[0], fixes));
    }

    static String[] splitNameAndPattern(String spec) {
        spec = spec.trim();

        String[] s = spec.split("\"");

        return new String[] {
            s[1],
            s[2].substring(1)
        };
    }

    private static String parseOutTypesFromPattern(String pattern, Map<String, String> constraints) {
        //XXX:
        Pattern p = Pattern.compile("(\\$.)(\\{([^}]*)\\})?");
        StringBuffer filtered = new StringBuffer();
        Matcher m = p.matcher(pattern);
        int i = 0;

        while (m.find()) {
            filtered.append(pattern.substring(i, m.start()));
            i = m.end();

            String var  = m.group(1);
            String type = m.group(3);

            filtered.append(var);
            constraints.put(var, type); //XXX: set non-null at most once
        }

        filtered.append(pattern.substring(i));

        return filtered.toString();
    }
}
