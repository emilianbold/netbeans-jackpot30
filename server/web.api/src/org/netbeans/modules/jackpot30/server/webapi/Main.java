package org.netbeans.modules.jackpot30.server.webapi;

import com.sun.grizzly.http.SelectorThread;
import com.sun.jersey.api.container.grizzly.GrizzlyWebContainerFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;

/**
 *
 * @author lahvac
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: java -jar " + Main.class.getProtectionDomain().getCodeSource().getLocation().getPath() + " <cache> [nohup]");
            return ;
        }
        
        Cache.setStandaloneCacheRoot(new File(args[0]));

        final String baseUri = "http://localhost:9998/";
        final Map<String, String> initParams = new HashMap<String, String>();
        initParams.put("com.sun.jersey.config.property.packages",
                "org.netbeans.modules.jackpot30.server.webapi");

        System.out.println("Starting grizzly...");
        SelectorThread threadSelector = GrizzlyWebContainerFactory.create(
                baseUri, initParams);

        if (args.length == 2 && "nohup".equals(args[1])) {
            return ;
        }
        
        System.out.println(String.format(
                "Jersey app started with WADL available at %sapplication.wadl\n" +
                "Hit enter to stop it...", baseUri));
        System.in.read();
        threadSelector.stopEndpoint();
        System.exit(0);
    }

}
