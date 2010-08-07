/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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

package org.netbeans.modules.jackpot30.impl.examples;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.netbeans.modules.jackpot30.impl.examples.Example.Option;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class LoadExamples {

    public static Iterable<? extends Example> loadExamples() {
        return loadExamples("examples.list");
    }

    private static final Pattern EXAMPLE_SPLIT = Pattern.compile("^%%", Pattern.MULTILINE);
    
    static Iterable<? extends Example> loadExamples(String examplesListName) {
        String examplesList = loadToString(examplesListName);

        if (examplesList == null) return Collections.emptyList();

        List<Example> examples = new LinkedList<Example>();

        for (String exampleName : examplesList.split("\n")) {
            if (exampleName.isEmpty()) continue;

            String exampleCode = loadToString(exampleName);

            for (String oneExample : EXAMPLE_SPLIT.split(exampleCode)) {
                if (oneExample.isEmpty()) continue;
                
                String[] parts = oneExample.split("\n", 2);
                String key = null;
                Set<Option> options = EnumSet.noneOf(Option.class);
                
                for (String option : parts[0].split(",")) {
                    if (option.startsWith("example=")) {
                        key = option.substring("example=".length());
                        continue;
                    }

                    options.add(Option.valueOf(option.toUpperCase()));
                }

                examples.add(new Example(key, parts[1], options));
            }
        }

        return examples;
    }

    private static String loadToString(String name) {
        InputStream examplesList = null;

        try {
            examplesList = LoadExamples.class.getResourceAsStream(name);

            if (examplesList == null) return null;

            StringBuilder result = new StringBuilder();
            int read;

            while ((read = examplesList.read()) != (-1)) {
                result.append((char) read);
            }

            return result.toString();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        } finally {
            if (examplesList != null) {
                try {
                    examplesList.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
}
