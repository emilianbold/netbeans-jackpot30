package org.netbeans.modules.jackpot30.file;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=ClassPathProvider.class, position=50)
public class ClassPathProviderImpl implements ClassPathProvider {

    public ClassPath findClassPath(FileObject file, String type) {
        if ("hint".equals(file.getExt())) {
            if (ClassPath.BOOT.equals(type)) {
                return MethodInvocationContext.computeClassPaths()[0];
            } else if (ClassPath.COMPILE.equals(type)) {
                return MethodInvocationContext.computeClassPaths()[1];
            }
        }

        return null;
    }

}
