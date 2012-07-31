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

import com.sun.jersey.api.NotFoundException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.codeviation.pojson.Pojson;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.lucene.support.Index;
import org.netbeans.modules.parsing.lucene.support.IndexManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author lahvac
 */
public class CategoryStorage {

    public static synchronized void setCacheRoot(File cacheRoot) {
        CategoryStorage.cacheRoot = cacheRoot;
        categoryCache = null;
    }

    public static void internalReset() {
        setCacheRoot(cacheRoot);
    }

    private static File cacheRoot;
    private static Reference<Iterable<? extends CategoryStorage>> categoryCache;

    public static synchronized Iterable<? extends CategoryStorage> listCategories() {
        Iterable<? extends CategoryStorage> cached = categoryCache != null ? categoryCache.get() : null;

        if (cached != null) return cached;

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

        categoryCache = new SoftReference<Iterable<? extends CategoryStorage>>(result);
        
        return result;
    }

    public static CategoryStorage forId(String id) {
        for (CategoryStorage s : listCategories()) {
            if (s.id.equals(id)) return s;
        }

        throw new NotFoundException("No category with id: " + id);
    }
    
    private final String id;
    private final String displayName;

    private CategoryStorage(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    private Iterable<? extends URL> getCategoryIndexFolders() {
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
                if (!c.startsWith("rel:")) continue;
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

    public Iterable<? extends String> getSourceRoots() {
        List<String> result = new ArrayList<String>();

        for (URL srcRoot : getCategoryIndexFolders()) {
            if (!"rel".equals(srcRoot.getProtocol())) continue;
                result.add(srcRoot.getPath().substring(1));
        }

        return result;
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

    private File getIndexFile() {
        return new File(new File(cacheRoot, id), "index");
    }

    private Reference<Index> cachedIndex;

    public synchronized Index getIndex() {
        Index cached = cachedIndex != null ? cachedIndex.get() : null;

        if (cached != null) return cached;

        try {
            Index index = IndexManager.createIndex(getIndexFile(), new KeywordAnalyzer());

            index.getStatus(true);

            cachedIndex = new SoftReference<Index>(index);

            return index;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public File getSegment(String relPath) {
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
            String segment = invertedSegments.get(relPath);

            if (segment == null) {
                segment = invertedSegments.get("rel:/" + relPath);
            }

            if (segment == null) {
                segment = invertedSegments.get("rel:/" + relPath + "/");
            }
            
            if (segment != null) {
                return new File(new File(cacheRoot, id), segment);
            } else {
                return null;
            }
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
        return null;
    }


    private long getSize() {
        long result = 0;

        for (Enumeration<? extends FileObject> en = getCacheRoot().getChildren(true); en.hasMoreElements(); ) {
            FileObject f = en.nextElement();

            if (f.isData()) {
                result += f.getSize();
            }
        }

        return result;
    }

    private AtomicReference<String> info = new AtomicReference<String>();

    public String getInfo() {
        String result = info.get();

        if (result != null) return result;

        FileObject infoFile = getCacheRoot().getFileObject("info");
        String content;
        try {
            content = infoFile != null ? infoFile.asText("UTF-8") : "{}";
        } catch (IOException ex) {
            Logger.getLogger(CategoryStorage.class.getName()).log(Level.SEVERE, null, ex);
            content = "{}";
        }
        Map<String, Object> infoData = Pojson.load(HashMap.class, content);

        if (!infoData.containsKey("indexSize")) {
            infoData.put("indexSize", getSize());
        }

        info.set(result = Pojson.save(infoData));

        return result;
    }
}
