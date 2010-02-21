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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;

/**
 *
 * @author lahvac
 */
public class StandaloneFinderTest {
    
    public StandaloneFinderTest() {}

    private File src;

    @Before
    public void setUp() throws Exception {
        File source = new File(StandaloneFinderTest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File workingDirectory = new File(source.getParentFile(), "wd");
        File cache = new File(workingDirectory, "cache");

        src = new File(workingDirectory, "src");

        Cache.setStandaloneCacheRoot(cache);

        copyStringToFile(new File(src, "test/Test1.java"), "package test; public class Test {private void test() { new java.io.File(\"\").isDirectory(); } }");

        StandaloneIndexer.index(src, false);
    }

    public final static void copyStringToFile (File f, String content) throws Exception {
        f.getParentFile().mkdirs();

        FileOutputStream os = new FileOutputStream(f);
        InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
        int read;
        while ((read = is.read()) != (-1)) {
            os.write(read);
        }
        os.close ();
        is.close();
    }

    private static List<Integer> toIntegerList(int[] arr) {
        List<Integer> res = new ArrayList<Integer>(arr.length);

        for (int e : arr) {
            res.add(e);
        }

        return res;
    }
    
    @Test
    public void testFindSpans() throws Exception {
        Assert.assertEquals(Arrays.asList(55, 89), toIntegerList(StandaloneFinder.findCandidateOccurrenceSpans(src, "test/Test1.java", "$1.isDirectory()")));
    }
}
