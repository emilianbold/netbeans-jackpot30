/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.util.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.EncodingContext;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author lahvac
 */
public class BulkSearchPerformance extends NbTestCase {

    public BulkSearchPerformance(String name) {
        super(name);
    }

    public void testPerformance() throws Exception {
        File root = new File("/media/karta/space/src/nb-main/uml");
        List<FileObject> files = new LinkedList<FileObject>();

        gatherFiles(FileUtil.toFileObject(root), files);

        String[] patterns = new String[] {"$1.isDirectory()", "new ImageIcon()"};

        for (int c = 0; c < 5; c++) {
            performPerformanceTest(files, new NFABasedBulkSearch(), patterns);
            performPerformanceTest(files, new REBasedBulkSearch(), patterns);
            performPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);
        }

        performPerformanceTest(files, new NFABasedBulkSearch(), patterns);
        long nfa1 = performPerformanceTest(files, new NFABasedBulkSearch(), patterns);
        performPerformanceTest(files, new REBasedBulkSearch(), patterns);
        long regexp1 = performPerformanceTest(files, new REBasedBulkSearch(), patterns);
        performPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);
        long cf1 = performPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);

        performPerformanceTest(files, new REBasedBulkSearch(), patterns);
        long regexp2 = performPerformanceTest(files, new REBasedBulkSearch(), patterns);
        performPerformanceTest(files, new NFABasedBulkSearch(), patterns);
        long nfa2 = performPerformanceTest(files, new NFABasedBulkSearch(), patterns);
        performPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);
        long cf2 = performPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);

        System.err.println("regexp1=" + regexp1);
        System.err.println("nfa1=" + nfa1);
        System.err.println("cf1=" + cf1);
        System.err.println("regexp2=" + regexp2);
        System.err.println("nfa2=" + nfa2);
        System.err.println("cf2=" + cf2);

        System.err.println("perf (nfa1/regexp1): " + ((1000 * nfa1 / regexp1) /10.0) + "%");
        System.err.println("perf (nfa2/regexp2): " + ((1000 * nfa2 / regexp2) /10.0) + "%");
        System.err.println("perf (cf1/regexp1): " + ((1000 * cf1 / regexp1) /10.0) + "%");
        System.err.println("perf (cf2/regexp2): " + ((1000 * cf2 / regexp2) /10.0) + "%");
    }

    public void testIndexingPerformance() throws Exception {
        File root = new File("/media/karta/space/src/nb-main/uml");
//        File root = new File("/media/karta/space/src/nb-main/contrib/javahints");
        List<FileObject> files = new LinkedList<FileObject>();

        gatherFiles(FileUtil.toFileObject(root), files);

        String[] patterns = new String[] {"$1.isDirectory()", "new ImageIcon()"};

        for (int c = 0; c < 5; c++) {
            performIndexingPerformanceTest(files, new NFABasedBulkSearch(), patterns);
            performIndexingPerformanceTest(files, new REBasedBulkSearch(), patterns);
//            performIndexingPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);
        }

        performIndexingPerformanceTest(files, new NFABasedBulkSearch(), patterns);
        long[] nfa1 = performIndexingPerformanceTest(files, new NFABasedBulkSearch(), patterns);
        performIndexingPerformanceTest(files, new REBasedBulkSearch(), patterns);
        long[] regexp1 = performIndexingPerformanceTest(files, new REBasedBulkSearch(), patterns);
//        performIndexingPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);
//        long cf1 = performIndexingPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);

        performIndexingPerformanceTest(files, new REBasedBulkSearch(), patterns);
        long[] regexp2 = performIndexingPerformanceTest(files, new REBasedBulkSearch(), patterns);
        performIndexingPerformanceTest(files, new NFABasedBulkSearch(), patterns);
        long[] nfa2 = performIndexingPerformanceTest(files, new NFABasedBulkSearch(), patterns);
//        performIndexingPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);
//        long cf2 = performIndexingPerformanceTest(files, new CopyFinderBasedBulkSearch(), patterns);

        System.err.println("i.regexp1=" + regexp1[0]);
        System.err.println("i.nfa1=" + nfa1[0]);
        System.err.println("m.regexp1=" + regexp1[1]);
        System.err.println("m.nfa1=" + nfa1[1]);
//        System.err.println("cf1=" + cf1);
        System.err.println("i.regexp2=" + regexp2[0]);
        System.err.println("i.nfa2=" + nfa2[0]);
        System.err.println("m.regexp2=" + regexp2[1]);
        System.err.println("m.nfa2=" + nfa2[1]);
//        System.err.println("cf2=" + cf2);

        System.err.println("perf i.(nfa1/regexp1): " + ((1000 * nfa1[0] / regexp1[0]) /10.0) + "%");
        System.err.println("perf m.(nfa1/regexp1): " + ((1000 * nfa1[1] / regexp1[1]) /10.0) + "%");
        System.err.println("perf i.(nfa2/regexp2): " + ((1000 * nfa2[0] / regexp2[0]) /10.0) + "%");
        System.err.println("perf m.(nfa2/regexp2): " + ((1000 * nfa2[1] / regexp2[1]) /10.0) + "%");
//        System.err.println("perf (cf1/regexp1): " + ((1000 * cf1 / regexp1) /10.0) + "%");
//        System.err.println("perf (cf2/regexp2): " + ((1000 * cf2 / regexp2) /10.0) + "%");
    }
    
    private long performPerformanceTest(List<FileObject> files, final BulkSearch search, final String... pattern) throws IOException {
        ClasspathInfo cpInfo = Utilities.createUniversalCPInfo();

        final BulkPattern[] bulkPattern = new BulkPattern[1];
        JavaSource.create(cpInfo).runUserActionTask(new Task<CompilationController>() {

            public void run(CompilationController parameter) throws Exception {
                bulkPattern[0] = search.create(parameter, pattern);
            }
        }, true);

        long start = System.currentTimeMillis();
        JavaSource.create(cpInfo, files).runUserActionTask(new Task<CompilationController>() {

            public void run(CompilationController parameter) throws Exception {
                if (parameter.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0)
                    return;

                search.match(parameter, new TreePath(parameter.getCompilationUnit()), bulkPattern[0]);
            }
        }, true);
        long end = System.currentTimeMillis();

        return end - start;
    }

    private long[] performIndexingPerformanceTest(List<FileObject> files, final BulkSearch search, final String... pattern) throws IOException {
        final List<byte[]> index = new LinkedList<byte[]>();
        long[] result = new long[2];
        ClasspathInfo cpInfo = Utilities.createUniversalCPInfo();

        final BulkPattern[] bulkPattern = new BulkPattern[1];
        JavaSource.create(cpInfo).runUserActionTask(new Task<CompilationController>() {

            public void run(CompilationController parameter) throws Exception {
                bulkPattern[0] = search.create(parameter, pattern);
            }
        }, true);

        long start = System.currentTimeMillis();
        JavaSource.create(cpInfo, files).runUserActionTask(new Task<CompilationController>() {

            public void run(CompilationController parameter) throws Exception {
                if (parameter.toPhase(Phase.PARSED).compareTo(Phase.PARSED) < 0)
                    return;

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                EncodingContext ec = new EncodingContext(out);
                search.encode(parameter.getCompilationUnit(), ec);

                index.add(out.toByteArray());
            }
        }, true);
        long end = System.currentTimeMillis();

        result[0] = end - start;

        start = System.currentTimeMillis();

        for (byte[] data : index) {
            search.matches(new ByteArrayInputStream(data), bulkPattern[0]);
        }

        end = System.currentTimeMillis();

        result[1] = end - start;

        return result;
    }
    
    private static void gatherFiles(FileObject root, List<FileObject> files) {
        if ("java".equals(root.getExt())) {
            files.add(root);
        }
        
        if (root.isFolder()) {
            for (FileObject c : root.getChildren()) {
                gatherFiles(c, files);
            }
        }
    }
}
