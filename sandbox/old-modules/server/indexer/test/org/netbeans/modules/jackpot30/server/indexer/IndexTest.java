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
import junit.framework.TestCase;

/**
 *
 * @author lahvac
 */
public class IndexTest extends TestCase {

    public IndexTest(String name) {
        super(name);
    }

    public void testCmdLine() throws IOException {
        assertCmdLineOk("/foo", false, false, null, null, "/foo", "/cache");
        assertCmdLineOk("/foo", false, false, "/modified.list", "/removed.list", "/foo", "/cache", "/modified.list", "/removed.list");
        assertCmdLineOk("/foo", true, false, null, null, Index.PARAM_CONSTRUCT_DUPLICATES_INDEX, "/foo", "/cache");
        assertCmdLineOk("/foo", true, false, "/modified.list", "/removed.list", Index.PARAM_CONSTRUCT_DUPLICATES_INDEX, "/foo", "/cache", "/modified.list", "/removed.list");
        assertCmdLineOk("/foo", false, true, null, null, Index.PARAM_STORE_SOURCES, "/foo", "/cache");
        assertCmdLineOk("/foo", false, true, "/modified.list", "/removed.list", Index.PARAM_STORE_SOURCES, "/foo", "/cache", "/modified.list", "/removed.list");
        assertCmdLineOk("/foo", true, true, null, null, Index.PARAM_CONSTRUCT_DUPLICATES_INDEX, Index.PARAM_STORE_SOURCES, "/foo", "/cache");
        assertCmdLineOk("/foo", true, true, "/modified.list", "/removed.list", Index.PARAM_CONSTRUCT_DUPLICATES_INDEX, Index.PARAM_STORE_SOURCES, "/foo", "/cache", "/modified.list", "/removed.list");
        assertCmdLineFailed();
        assertCmdLineFailed("/foo");
    }

    private static void assertCmdLineOk(final String rootGolden, final boolean duplicatesIndexGolden, final boolean storeSourcesGolden, final String modifiedGolden, final String removedGolden, String... cmdLine) throws IOException {
        new Index() {
            @Override
            protected void invokeIndexer(File root, boolean duplicatesIndex, boolean storeSources, String modified, String removed) throws IOException {
                assertEquals(rootGolden, root.getPath());
                assertEquals(duplicatesIndexGolden, duplicatesIndex);
                assertEquals(storeSourcesGolden, storeSources);
                assertEquals(modifiedGolden, modified);
                assertEquals(removedGolden, removed);
            }
            @Override
            protected void printHelp() {
                fail();
            }
        }.index(cmdLine);
    }

    private static void assertCmdLineFailed(String... cmdLine) throws IOException {
        new Index() {
            @Override
            protected void invokeIndexer(File root, boolean duplicatesIndex, boolean storeSources, String modified, String removed) throws IOException {
                fail();
            }
            @Override
            protected void printHelp() {}
        }.index(cmdLine);
    }
}