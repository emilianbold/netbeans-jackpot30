/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.netbeans.modules.jackpot30.cnd.remote.bridge;

import java.util.ArrayList;
import java.util.Collection;
import org.netbeans.modules.cnd.api.remote.ServerList;
import org.netbeans.modules.jackpot30.remoting.api.FileSystemLister;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.remote.spi.FileSystemProvider;
import org.openide.filesystems.FileSystem;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=FileSystemLister.class)
public class FileSystemListerImpl implements FileSystemLister {

    @Override public Collection<? extends FileSystem> getKnownFileSystems() {
        Collection<FileSystem> fss = new ArrayList<FileSystem>();

        for (ExecutionEnvironment ee : ServerList.getEnvironments()) {
            if (ee.isLocal()) continue;
            
            FileSystem fs = FileSystemProvider.getFileSystem(ee);

            if (fs != null) {
                fss.add(fs);
            }
        }

        return fss;
    }

}
