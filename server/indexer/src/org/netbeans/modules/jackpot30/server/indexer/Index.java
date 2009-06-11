package org.netbeans.modules.jackpot30.server.indexer;

import java.io.File;
import java.io.IOException;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;

/**
 *
 * @author lahvac
 */
public class Index {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java -jar " + Index.class.getProtectionDomain().getCodeSource().getLocation().getPath() + " <source-root> <cache>");
            return ;
        }

        long startTime = System.currentTimeMillis();

        Cache.setStandaloneCacheRoot(new File(args[1]));
        StandaloneIndexer.index(new File(args[0]));

        long endTime = System.currentTimeMillis();

        System.out.println("indexing took: " + Utilities.toHumanReadableTime(endTime - startTime));
    }

}
