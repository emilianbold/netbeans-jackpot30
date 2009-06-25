package org.netbeans.modules.jackpot30.impl.batch;

import java.net.URL;
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
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import org.openide.util.lookup.ServiceProvider;
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
    
    public void testBatchSearchSpan() throws Exception {
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

    public void testBatchSearchNotIndexed() throws Exception {
        writeFilesAndWaitForScan(src1,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));
        writeFilesAndWaitForScan(src3,
                                 new File("test/Test1.java", "package test; public class Test1 { private void test() { Test2 f = null; f.isDirectory(); } }"),
                                 new File("test/Test2.java", "package test; public class Test2 { public boolean isDirectory() {return false} }"));

        HintDescription hint = PatternConvertor.create("$1.isDirectory() :: $1 instanceof test.Test2 ;;");
        BatchResult result = BatchSearch.findOccurrences(hint, Scope.GIVEN_SOURCE_ROOTS, src1, src3);
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
        golden.put(src3.getURL().toExternalForm(), Arrays.asList("test/Test1.java"));

        assertEquals(golden, output);

        //check verification:
        Map<String, Iterable<String>> verifiedOutput = new HashMap<String, Iterable<String>>();
        
        for (Entry<? extends Container, ? extends Iterable<? extends Resource>> e : result.projectId2Resources.entrySet()) {
            Collection<String> resourcesRepr = new LinkedList<String>();

            for (Resource r : e.getValue()) {
                for (ErrorDescription ed : r.getVerifiedSpans()) {
                    resourcesRepr.add(ed.toString());
                }
            }

            verifiedOutput.put(e.getKey().toDebugString(), resourcesRepr);
        }

        Map<String, Iterable<String>> verifiedGolden = new HashMap<String, Iterable<String>>();

        verifiedGolden.put(src1.getURL().toExternalForm(), Arrays.asList("0:82-0:93:verifier:TODO: No display name"));
        verifiedGolden.put(src3.getURL().toExternalForm(), Arrays.asList("0:75-0:86:verifier:TODO: No display name"));

        assertEquals(verifiedGolden, verifiedOutput);
    }

    private FileObject src1;
    private FileObject src2;
    private FileObject src3;

    private void prepareTest() throws Exception {
        FileObject workdir = SourceUtilsTestUtil.makeScratchDir(this);

        src1 = FileUtil.createFolder(workdir, "src1");
        src2 = FileUtil.createFolder(workdir, "src2");
        src3 = FileUtil.createFolder(workdir, "src3");

        ClassPathProviderImpl.setSourceRoots(Arrays.asList(src1, src2, src3));
        
        FileObject cache = FileUtil.createFolder(workdir, "cache");

        CacheFolder.setCacheFolder(cache);
    }

    @ServiceProvider(service=ClassPathProvider.class)
    public static final class ClassPathProviderImpl implements ClassPathProvider {

        private static Collection<FileObject> sourceRoots;

        public synchronized static void setSourceRoots(Collection<FileObject> sourceRoots) {
            ClassPathProviderImpl.sourceRoots = sourceRoots;
        }

        public synchronized static Collection<FileObject> getSourceRoots() {
            return sourceRoots;
        }

        public ClassPath findClassPath(FileObject file, String type) {
            if (ClassPath.BOOT.equals(type)) {
                return ClassPathSupport.createClassPath(SourceUtilsTestUtil.getBootClassPath().toArray(new URL[0]));
            }
            
            if (ClassPath.COMPILE.equals(type)) {
                return ClassPathSupport.createClassPath(new URL[0]);
            }

            if (ClassPath.SOURCE.equals(type)) {
                for (FileObject sr : sourceRoots) {
                    if (file.equals(sr) || FileUtil.isParentOf(sr, file)) {
                        return ClassPathSupport.createClassPath(sr);
                    }
                }
            }

            return null;
        }
        
    }
}