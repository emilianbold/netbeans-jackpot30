/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.backend.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;

/**
 *
 * @author lahvac
 */
public class CategoryStorage {

    public static void setCategoryContent(String categoryId, String categoryName, Set<FileObject> content) {
        Preferences categoriesNode = NbPreferences.forModule(CategoryStorage.class).node("categories");

        categoriesNode.put(categoryId + "_displayName", categoryName);

        StringBuilder roots = new StringBuilder();

        for (FileObject f : content) {
            if (roots.length() > 0) {
                roots.append(';');
            }

            try {
                roots.append(f.getURL().toExternalForm());
            } catch (FileStateInvalidException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        categoriesNode.put(categoryId + "_roots", roots.toString());
    }

    public static Set<FileObject> getCategoryContent(String categoryId) {
        Preferences categoriesNode = NbPreferences.forModule(CategoryStorage.class).node("categories");
        String roots = categoriesNode.get(categoryId + "_roots", "");
        Set<FileObject> result = new HashSet<FileObject>();

        for (String urlString : roots.split(";")) {
            if (urlString.isEmpty()) continue;

            try {
                URL url = new URL(urlString);
                FileObject root = URLMapper.findFileObject(url);

                if (root != null) {
                    result.add(root);
                }
            } catch (MalformedURLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return result;
    }

    public static Map<String, String> listCategoriesWithNames() {
        Map<String, String> result = new HashMap<String, String>();
        Preferences categoriesNode = NbPreferences.forModule(CategoryStorage.class).node("categories");

        try {
            for (String key : categoriesNode.keys()) {
                if (key.endsWith("_displayName")) {
                    String id = key.substring(0, key.length() - "_displayName".length());
                    result.put(id, categoriesNode.get(key, id));
                }
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }

        return result;
    }
}
