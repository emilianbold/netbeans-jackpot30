package org.netbeans.modules.jackpot30.server.webapi;

import java.io.File;
import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.server.indexer.StandaloneFinder;

/**
 *
 * @author lahvac
 */
@Path("/index")
public class API {

    @GET
    @Path("/find")
    public String find(@QueryParam("path") String path, @QueryParam("pattern") String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (String candidate : StandaloneFinder.findCandidates(new File(path), pattern)) {
            sb.append(candidate);
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/list")
    public String list() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (String root : Cache.knownSourceRoots()) {
            sb.append(root);
            sb.append("\n");
        }

        return sb.toString();
    }
}
