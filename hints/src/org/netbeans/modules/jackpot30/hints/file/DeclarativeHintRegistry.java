/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javahints.file;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.util.Exceptions;

/**
 *
 * @author Jan Lahoda
 */
public class DeclarativeHintRegistry {

    public static Iterable<DeclarativeHint> getAllHints() {
        FileObject folder = Repository.getDefault().getDefaultFileSystem().getRoot().getFileObject("org-netbeans-modules-java-hints/declarative");

        if (folder == null) {
            return Collections.emptyList();
        }

        List<DeclarativeHint> result = new LinkedList<DeclarativeHint>();

        for (FileObject f : folder.getChildren()) {
            if (!"hint".equals(f.getExt())) {
                continue;
            }

            result.addAll(parseHintFile(f));
        }

        return result;
    }

    private static List<DeclarativeHint> parseHintFile(FileObject file) {
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

    private static List<DeclarativeHint> parseHints(String spec) {
        String[] split = spec.split(";");
        List<DeclarativeHint> result = new LinkedList<DeclarativeHint>();

        for (String s : split) {
            s = s.trim();

            if (s.length() > 0) {
                result.add(DeclarativeHint.parse(s));
            }
        }

        return result;
    }
}
