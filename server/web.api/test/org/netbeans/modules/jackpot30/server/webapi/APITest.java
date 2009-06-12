package org.netbeans.modules.jackpot30.server.webapi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.server.indexer.StandaloneIndexer;

/**
 *
 * @author lahvac
 */
public class APITest {

    public APITest() {
    }

    private File src;

    @Before
    public void setUp() throws Exception {
        File source = new File(APITest.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        File workingDirectory = new File(source.getParentFile(), "wd");
        File cache = new File(workingDirectory, "cache");

        src = new File(workingDirectory, "src");

        Cache.setStandaloneCacheRoot(cache);

        copyStringToFile(new File(src, "test/Test1.java"), "package test; public class Test {private void test() { new java.io.File(\"\").isDirectory(); } }");

        StandaloneIndexer.index(src);
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

    @Test
    public void testFind() throws Exception {
        Assert.assertEquals("test/Test1.java\n", new API().find(src.getAbsolutePath(), "$1.isDirectory()"));
    }

}