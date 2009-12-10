/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 1997-2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.impl.indexing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.SPIAccessor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author lahvac
 */
public class Cache {

    public static final int VERSION = 1;

    private static File standaloneCacheRoot;
    private static Map<String, Cache> name2Cache = new HashMap<String, Cache>();

    public static Cache findCache(String indexName) {
        Cache cache = name2Cache.get(indexName);

        if (cache == null) {
            name2Cache.put(indexName, cache = new Cache(indexName));
        }

        return cache;
    }

    private final String name;

    private Cache(String name) {
        this.name = name;
    }
    
    public File findCacheRoot(URL sourceRoot) throws IOException {
        if (standaloneCacheRoot != null) {
            return getDataFolder(sourceRoot, name);
        } else {
            return findCacheRootNB(sourceRoot);
        }
    }

    private File findCacheRootNB(URL sourceRoot) throws IOException {
        FileObject indexBaseFolder = CacheFolder.getDataFolder(sourceRoot);
        String path = SPIAccessor.getInstance().getIndexerPath(name, VERSION);
        FileObject indexFolder = FileUtil.createFolder(indexBaseFolder, path);
        return FileUtil.toFile(indexFolder);
    }

    public static void setStandaloneCacheRoot(File standaloneCacheRoot) {
        Cache.standaloneCacheRoot = standaloneCacheRoot;
    }


    private static Properties segments;
    private static Map<String, String> invertedSegments;
    private static int index = 0;

    private static final String SEGMENTS_FILE = "segments";      //NOI18N
    private static final String SLICE_PREFIX = "s";              //NOI18N

    private static void loadSegments () throws IOException {
        if (segments == null) {
            final File folder = standaloneCacheRoot;
            assert folder != null;
            segments = new Properties ();
            invertedSegments = new HashMap<String,String> ();
            final File segmentsFile =  new File(folder, SEGMENTS_FILE);
            if (segmentsFile.canRead()) {
                final InputStream in = new FileInputStream(segmentsFile);
                try {
                    segments.load (in);
                } finally {
                    in.close();
                }
            }
            for (Entry<Object, Object> entry : segments.entrySet()) {
                String segment = (String) entry.getKey();
                String root = (String) entry.getValue();
                invertedSegments.put(root,segment);
                try {
                    index = Math.max (index,Integer.parseInt(segment.substring(SLICE_PREFIX.length())));
                } catch (NumberFormatException nfe) {
                    throw new IllegalStateException(nfe); //TODO: maybe just log the exception?
                }
            }
        }
    }

    public static Iterable<? extends String> knownSourceRoots() throws IOException {
        loadSegments();

        List<String> known = new LinkedList<String>();
        
        for (String segment : invertedSegments.keySet()) {
            known.add(segment.substring("file:".length()));
        }

        return known;
    }


    private static void storeSegments () throws IOException {
        final File folder = standaloneCacheRoot;
        assert folder != null;
        final File segmentsFile = new File(folder, SEGMENTS_FILE);
        segmentsFile.getParentFile().mkdirs();
        final OutputStream out = new FileOutputStream(segmentsFile);
        try {
            segments.store(out,null);
        } finally {
            out.close();
        }
    }

    private static synchronized File getDataFolder (final URL root, String name) throws IOException {
        loadSegments ();
        final String rootName = root.toExternalForm();
        String slice = invertedSegments.get (rootName);
        if ( slice == null) {
            slice = SLICE_PREFIX + (++index);
            while (segments.getProperty(slice) != null) {
                slice = SLICE_PREFIX + (++index);
            }
            segments.put (slice,rootName);
            invertedSegments.put(rootName, slice);
            storeSegments ();
        }
        final File folder = standaloneCacheRoot;
        return new File(new File(folder, slice), name);
    }

    private static synchronized File getCacheFolder () {
        return standaloneCacheRoot;
    }


}
