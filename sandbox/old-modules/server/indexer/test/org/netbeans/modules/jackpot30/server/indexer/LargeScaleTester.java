/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.server.indexer;

import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import junit.framework.TestCase;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.indexing.FileBasedIndex;

/**
 *
 * @author lahvac
 */
public class LargeScaleTester extends TestCase {
    
    public LargeScaleTester(String name) {
        super(name);
    }

    private static final boolean createIndex = true;

    private File src;

    public void setUp() throws Exception {
        Logger.getLogger(FileBasedIndex.class.getName()).setLevel(Level.ALL);
        Logger.getLogger(FileBasedIndex.class.getName()).addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                System.err.println(new SimpleFormatter().formatMessage(record));
            }
            @Override
            public void flush() {
                System.err.flush();
            }
            @Override
            public void close() throws SecurityException {}
        });
        
        File source = new File(LargeScaleTester.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File workingDirectory = new File(new File(source.getParentFile(), "wd"), this.getName());

        if (createIndex)
            deleteRecursively(workingDirectory);
        
        File cache = new File(workingDirectory, "cache");
        String toIndex = System.getProperty("index-folder");
        
        if (toIndex == null || !(src = new File(toIndex)).isDirectory()) {
            throw new IllegalStateException("Directory to index not correctly specified - add 'test-sys-prop.index-folder=<directory>' to private.properties");
        }

        Cache.setStandaloneCacheRoot(cache);

        if (createIndex) {
            long start = System.currentTimeMillis();
            StandaloneIndexer.index(src, false, false, null, null);
            long end = System.currentTimeMillis();
            System.err.println("indexing took: " + (end - start));
            System.err.println("cache size total: " + totalSize(cache) + ", lucene=" + totalSize(new File(cache, "s1/jackpot30/fulltext")));
        }
    }

    private static void deleteRecursively(File d) throws IOException {
        if (!d.exists()) return;
        
        if (d.isDirectory()) {
            for (File c : d.listFiles()) {
                deleteRecursively(c);
            }
        }

        if (!d.delete()) throw new IOException();
    }

    private static long totalSize(File d) throws IOException {
        if (!d.exists()) return 0;

        if (d.isDirectory()) {
            long total = 0;

            for (File c : d.listFiles()) {
                total += totalSize(c);
            }

            return total;
        } else {
            return d.length();
        }
    }
    
    public void testFind() throws Exception {
        performTest("new $type() { $mods$ $resultType $methodName($args$) { $statements$; } }");
        performTest("$1.isDirectory()");
    }

    private void performTest(String code) throws Exception {
        System.err.println("searching for " + code);
        long start = System.currentTimeMillis();
        System.err.println("Found occurrences: " + StandaloneFinder.findCandidates(src, code).size());
        long end = System.currentTimeMillis();

        System.err.println("total search time: " + (end - start));
    }
}
