package org.netbeans.modules.jackpot30.impl.batch;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.core.startup.Main;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Container;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.impl.indexing.IndexingTestUtils.File;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.PatternConvertor;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.modules.parsing.impl.indexing.Util;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static org.netbeans.modules.jackpot30.impl.indexing.IndexingTestUtils.writeFilesAndWaitForScan;

/**
 *
 * @author lahvac
 */
public class BatchSearchTest extends NbTestCase {

    public BatchSearchTest(String name) {
        super(name);
    }

    //XXX: copied from CustomIndexerImplTest:
    @Override
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[0], new Object[0]);
        Main.initializeURLFactory();
        org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        prepareTest();
        Util.allMimeTypes = Collections.singleton("text/x-java");
        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {ClassPathSupport.createClassPath(src1, src2)});
        RepositoryUpdater.getDefault().start(true);
        super.setUp();
    }

    public void testBatchSearch1() throws Exception {
        writeFilesAndWaitForScan(src1,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));
        writeFilesAndWaitForScan(src2,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));

        HintDescription hint = PatternConvertor.create("$1.isDirectory()");
        BatchResult result = BatchSearch.findOccurrences(hint, Scope.ALL_OPENED_PROJECTS);
        Map<String, Iterable<String>> output = new HashMap<String, Iterable<String>>();

        for (Entry<? extends Container, ? extends Iterable<? extends Resource>> e : result.projectId2Resources.entrySet()) {
            Collection<String> resourcesRepr = new LinkedList<String>();

            for (Resource r : e.getValue()) {
                resourcesRepr.add(r.getRelativePath());
            }

            output.put(e.getKey().toDebugString(), resourcesRepr);
        }

        Map<String, Iterable<String>> golden = new HashMap<String, Iterable<String>>();

        golden.put(src1.getURL().toExternalForm(), Arrays.asList("test/Test1.java"));
        golden.put(src2.getURL().toExternalForm(), Arrays.asList("test/Test1.java"));
        
        assertEquals(golden, output);
    }
    
    public void testBatchSearchSpanAndVerification() throws Exception {
        String code = "package test;\n" +
                      "public class Test {\n" +
                      "    private void m() {\n" +
                      "        a(c.i().getFileObject());\n" +
                      "        if (span != null && span[0] != (-1) && span[1] != (-1));\n" +
                      "        c.i().getFileObject(\"\");\n" +
                      "    }\n" +
                      "}\n";

        writeFilesAndWaitForScan(src1, new File("test/Test.java", code));

        HintDescription hint = PatternConvertor.create("$0.getFileObject($1)");
        BatchResult result = BatchSearch.findOccurrences(hint, Scope.ALL_OPENED_PROJECTS);

        assertEquals(1, result.projectId2Resources.size());
        Iterator<? extends Resource> resources = result.projectId2Resources.values().iterator().next().iterator();
        Resource r = resources.next();

        assertFalse(resources.hasNext());

        Set<String> snipets = new HashSet<String>();

        for (int[] span : r.getCandidateSpans()) {
            snipets.add(code.substring(span[0], span[1]));
        }

        Set<String> golden = new HashSet<String>(Arrays.asList("c.i().getFileObject(\"\")"));
        assertEquals(golden, snipets);
    }

    private FileObject src1;
    private FileObject src2;

    private void prepareTest() throws Exception {
        FileObject workdir = SourceUtilsTestUtil.makeScratchDir(this);

        src1 = FileUtil.createFolder(workdir, "src1");
        src2 = FileUtil.createFolder(workdir, "src2");

        FileObject cache = FileUtil.createFolder(workdir, "cache");

        CacheFolder.setCacheFolder(cache);
    }

}