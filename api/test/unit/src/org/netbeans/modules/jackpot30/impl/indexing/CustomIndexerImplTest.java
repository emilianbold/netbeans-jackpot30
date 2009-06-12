package org.netbeans.modules.jackpot30.impl.indexing;

import com.sun.source.tree.Tree;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.core.startup.Main;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.CustomIndexerImpl.CustomIndexerFactoryImpl;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.modules.parsing.impl.indexing.Util;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class CustomIndexerImplTest extends NbTestCase {

    public CustomIndexerImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[0], new Object[0]);
        Main.initializeURLFactory();
        org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        prepareTest();
        Util.allMimeTypes = Collections.singleton("text/x-java");
        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {ClassPathSupport.createClassPath(src)});
        RepositoryUpdater.getDefault().start(true);
        super.setUp();
    }

    public void testIndexing() throws Exception {
        writeFilesAndIndex(new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); } }"),
                           new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));

        verifyIndex("$1.isDirectory()", "test/Test1.java");
        verifyIndex("new ImageIcon($1)", "test/Test2.java");
        
        writeFilesAndIndex(new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); new javax.swing.ImageIcon(null); } }"),
                           new File("test/Test2.java", "package test; public class Test2 { private void test() { new javax.swing.ImageIcon(null); } }"));

        verifyIndex("$1.isDirectory()", "test/Test1.java");
        verifyIndex("new ImageIcon($1)", "test/Test1.java", "test/Test2.java");
        
        writeFilesAndIndex(new File("test/Test1.java", "package test; public class Test1 { private void test() { java.io.File f = null; f.isDirectory(); new javax.swing.ImageIcon(null); } }"));

        verifyIndex("$1.isDirectory()", "test/Test1.java");
        verifyIndex("new ImageIcon($1)", "test/Test1.java");
    }
    
    private FileObject src;

    private void prepareTest() throws Exception {
        FileObject workdir = SourceUtilsTestUtil.makeScratchDir(this);

        src = FileUtil.createFolder(workdir, "src");

        FileObject cache = FileUtil.createFolder(workdir, "cache");

        CacheFolder.setCacheFolder(cache);
    }

    private void writeFilesAndIndex(File... files) throws Exception {
        for (FileObject c : src.getChildren()) {
            c.delete();
        }
        
        for (File f : files) {
            FileObject fo = FileUtil.createData(src, f.filename);
            TestUtilities.copyStringToFile(fo, f.content);
        }

        SourceUtils.waitScanFinished();
    }

    private void verifyIndex(final String pattern, String... containedIn) throws Exception {
        ClassPath EMPTY = ClassPathSupport.createClassPath(new FileObject[0]);
        ClasspathInfo cpInfo = ClasspathInfo.create(ClassPathSupport.createClassPath(SourceUtilsTestUtil.getBootClassPath().toArray(new URL[0])),
                                                    EMPTY,
                                                    EMPTY);

        final Set<String> real = new HashSet<String>();
        
        JavaSource.create(cpInfo).runUserActionTask(new Task<CompilationController>() {
            public void run(CompilationController parameter) throws Exception {
                Tree patternTree = Utilities.parseAndAttribute(parameter, pattern, null);
                real.addAll(Index.get(src.getURL()).findCandidates(TreeSerializer.serializePatterns(patternTree)));
            }
        }, true);

        Set<String> golden = new HashSet<String>(Arrays.asList(containedIn));

        assertEquals(golden, real);
    }
    
    private static final class File {
        public final String filename;
        public final String content;

        public File(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }
    }

    @ServiceProvider(service=MimeDataProvider.class)
    public static final class MimeDataProviderImpl implements MimeDataProvider {

        private static final Lookup INDEXER = Lookups.fixed(new CustomIndexerFactoryImpl());
        
        public Lookup getLookup(MimePath mimePath) {
            if ("text/x-java".equals(mimePath.getPath())) {
                return INDEXER;
            }
            return null;
        }
        
    }

}