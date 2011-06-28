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
package org.netbeans.modules.jackpot30.backend.base;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codeviation.pojson.Pojson;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author lahvac
 */
public class CategoryStorage {

    public static void setCacheRoot(File cacheRoot) {
        CategoryStorage.cacheRoot = cacheRoot;
    }

    private static File cacheRoot;

    public static Iterable<? extends CategoryStorage> listCategories() {
        List<CategoryStorage> result = new ArrayList<CategoryStorage>();

        for (File cat : cacheRoot.listFiles()) {
            File info = new File(cat, "info");
            String displayName = cat.getName();
            if (info.canRead()) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = Pojson.load(HashMap.class, info);
                    if (data.containsKey("displayName")) {
                        displayName = (String) data.get("displayName"); //XXX: should check type!
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            result.add(new CategoryStorage(cat.getName(), displayName));
        }

        return result;
    }

    public static CategoryStorage forId(String id) {
        for (CategoryStorage s : listCategories()) {
            if (s.id.equals(id)) return s;
        }

        return null;
    }
    
    private final String id;
    private final String displayName;

    private CategoryStorage(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public Iterable<? extends URL> getCategoryIndexFolders() {
        try {
            FileObject root = getCacheRoot();
            CacheFolder.setCacheFolder(root);
            Set<URL> result = new HashSet<URL>();

            CacheFolder.getDataFolder(new URL("file:/"), true);
            
            //XXX:
            Field invertedSegmentsField = CacheFolder.class.getDeclaredField("invertedSegments");

            invertedSegmentsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, String> invertedSegments = (Map<String, String>) invertedSegmentsField.get(null);

            for (String c : invertedSegments.keySet()) {
                result.add(new URL(c));
            }

            return result;
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchFieldException ex) {
            Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public FileObject getCacheRoot() {
        return FileUtil.toFileObject(FileUtil.normalizeFile(new File(cacheRoot, id)));
    }
}