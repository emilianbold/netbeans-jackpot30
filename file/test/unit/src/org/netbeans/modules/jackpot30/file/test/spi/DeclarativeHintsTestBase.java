package org.netbeans.modules.jackpot30.file.test.spi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import junit.framework.TestSuite;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.NbTestSuite;
import org.netbeans.modules.jackpot30.file.test.TestParser;
import org.netbeans.modules.jackpot30.file.test.TestParser.TestCase;
import org.netbeans.modules.jackpot30.file.test.TestPerformer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintsTestBase extends NbTestCase {

    private final FileObject hintFile;
    private final FileObject testFile;
    private final TestCase test;

    public DeclarativeHintsTestBase() {
        super(null);
        throw new IllegalStateException();
    }

    public DeclarativeHintsTestBase(FileObject hintFile, FileObject testFile, TestCase test) {
        super(FileUtil.getFileDisplayName(testFile) + "/" + test.getName());
        this.hintFile = hintFile;
        this.testFile = testFile;
        this.test = test;
    }

    @Override
    protected void setUp() throws Exception {
        clearWorkDir();
        System.setProperty("netbeans.user", getWorkDir().getAbsolutePath());
        super.setUp();
    }

    public static TestSuite suite(Class<?> clazz) {
        NbTestSuite result = new NbTestSuite();

        for (String test : listTests(clazz)) {
            //TODO:
            URL testURL = DeclarativeHintsTestBase.class.getClassLoader().getResource(test);
            
            assertNotNull(testURL);

            FileObject testFO = URLMapper.findFileObject(testURL);

            assertNotNull(testFO);

            String hint = test.substring(0, test.length() - ".test".length()) + ".hint";
            URL hintURL = DeclarativeHintsTestBase.class.getClassLoader().getResource(hint);

            assertNotNull(hintURL);
            
            FileObject hintFO = URLMapper.findFileObject(hintURL);

            assertNotNull(hintFO);

            try {
                for (TestCase tc : TestParser.parse(testFO.asText("UTF-8"))) {
                    result.addTest(new DeclarativeHintsTestBase(hintFO, testFO, tc));
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        return result;
    }

    @Override
    protected void runTest() throws Throwable {
        Map<TestCase, Collection<String>> result = TestPerformer.performTest(hintFile, testFile, new TestCase[]{test}, new AtomicBoolean());
        Collection<String> actualResults = result.get(test);

        assertNotNull(actualResults);
        assertEquals(Arrays.asList(test.getResults()), actualResults);
    }

    private static Collection<String> listTests(Class<?> clazz) {
        File dirOrArchive = FileUtil.archiveOrDirForURL(clazz.getProtectionDomain().getCodeSource().getLocation());

        assertTrue(dirOrArchive.exists());

        if (dirOrArchive.isFile()) {
            return listTestsFromJar(dirOrArchive);
        } else {
            Collection<String> result = new LinkedList<String>();

            listTestsFromFilesystem(dirOrArchive, "", result);

            return result;
        }
    }
    
    private static Collection<String> listTestsFromJar(File archive) {
        Collection<String> result = new LinkedList<String>();

        try {
            JarFile jf = new JarFile(archive);
            Enumeration<JarEntry> entries = jf.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().endsWith(".test")) {
                    result.add(entry.getName());
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        
        return result;
    }

    private static void listTestsFromFilesystem(File file, String prefix, Collection<String> output) {
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                listTestsFromFilesystem(f, (prefix.length() > 0 ? (prefix + "/") : "") + f.getName(), output);
            }
        } else {
            if (file.getName().endsWith(".test")) {
                output.add(prefix);
            }
        }
    }
}
