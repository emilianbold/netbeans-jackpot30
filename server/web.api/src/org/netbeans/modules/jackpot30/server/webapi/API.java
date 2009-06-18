package org.netbeans.modules.jackpot30.server.webapi;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.server.indexer.StandaloneFinder;

/**
 *
 * @author lahvac
 */
@Path("/index")
public class API {

    @GET
    @Path("/find")
    @Produces("text/plain")
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
    @Produces("text/plain")
    public String list() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (String root : Cache.knownSourceRoots()) {
            sb.append(root);
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/cat")
    @Produces("text/plain")
    public String cat(@QueryParam("path") String path, @QueryParam("relative") String relative) throws IOException {
        URL sourceRoot = new File(path).toURI().toURL();
        Index index = Index.get(sourceRoot);

        if (index == null) {
            throw new IOException("No index");
        }

        CharSequence source = index.getSourceCode(relative);

        if (source == null) {
            throw new IOException("Source code not found");
        }
        
        return source.toString();
    }
}
