package org.netbeans.modules.jackpot30.file.test;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.UnitTestForSourceQuery;
import org.netbeans.spi.gototest.TestLocator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.URLMapper;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=TestLocator.class)
public class TestLocatorImpl implements TestLocator {

    public boolean appliesTo(FileObject fo) {
        String mimeType = FileUtil.getMIMEType(fo);

        if ("text/x-javahints".equals(mimeType)) {
            return true;
        }
        
        if ("text/x-javahintstest".equals(mimeType)) {
            return true;
        }

        return false;
    }

    public boolean asynchronous() {
        return false;
    }

    public LocationResult findOpposite(FileObject fo, int caretOffset) {
        String mimeType = FileUtil.getMIMEType(fo);

        if ("text/x-javahints".equals(mimeType)) {
            FileObject test = findOpposite(fo, true);

            if (test != null) {
                return new LocationResult(test, -1);
            } else {
                return new LocationResult("Test not found.");
            }
        }

        if ("text/x-javahintstest".equals(mimeType)) {
            FileObject rule = findOpposite(fo, false);

            if (rule != null) {
                return new LocationResult(rule, -1);
            } else {
                return new LocationResult("Rule file not found.");
            }
        }

        throw new IllegalArgumentException();
    }

    public void findOpposite(FileObject fo, int caretOffset, LocationListener callback) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public FileType getFileType(FileObject fo) {
        String mimeType = FileUtil.getMIMEType(fo);

        if ("text/x-javahints".equals(mimeType)) {
            return FileType.TESTED;
        }

        if ("text/x-javahintstest".equals(mimeType)) {
            return FileType.TEST;
        }

        return FileType.NEITHER;
    }

    static @CheckForNull FileObject findOpposite(FileObject rule, boolean toTest) {
        ClassPath cp = ClassPath.getClassPath(rule, ClassPath.SOURCE);
        String resourceName = cp != null ? cp.getResourceName(rule) : null;

        if (resourceName == null) {
            Logger.getLogger(TestLocatorImpl.class.getName()).log(Level.FINE, "cp==null or rule file cannot be found on its own source cp");
            return null;
        }

        String testFileName = resourceName.substring(0, resourceName.lastIndexOf('.')) + (toTest ? ".test" : ".hint");

        FileObject testFile = cp.findResource(testFileName);

        if (testFile == null) {
            URL[] sr;

            if (toTest) {
                sr = UnitTestForSourceQuery.findUnitTests(cp.findOwnerRoot(rule));
            } else {
                sr = UnitTestForSourceQuery.findSources(cp.findOwnerRoot(rule));
            }
            
            for (URL testRoot : sr) {
                FileObject testRootFO = URLMapper.findFileObject(testRoot);

                if (testRootFO != null) {
                    testFile = testRootFO.getFileObject(testFileName);

                    if (testFile != null) {
                        break;
                    }
                }
            }
        }

        return testFile;
    }

}
