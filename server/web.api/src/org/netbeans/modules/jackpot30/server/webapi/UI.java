package org.netbeans.modules.jackpot30.server.webapi;

import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.netbeans.modules.jackpot30.impl.WebUtilities;

/**
 *
 * @author lahvac
 */
@Path("/index/ui")
public class UI {

    @GET
    @Produces("text/html")
    public String doUI(@QueryParam("path") String path, @QueryParam("pattern") String pattern) throws URISyntaxException {
        StringBuilder response = new StringBuilder();

        response.append("<html>\n");
        response.append("<body>\n");
        response.append("<form method=\"get\">\n");
        response.append("<label for=\"path\">Project:</label>");
        response.append("<select size=\"1\" name=\"path\">");
        for (String c : WebUtilities.requestStringArrayResponse(new URI("http://localhost:9998/index/list"))) {
            response.append("<option");
            if (c.equals(path)) {
                response.append(" selected");
            }
            response.append(">");
            response.append(c);
            response.append("</option>");
        }
        response.append("</select>");
        response.append("<br>");
        response.append("<label for=\"pattern\">Pattern:</label><br>");
        response.append("<textarea rows=\"10\" cols=\"40\" name=\"pattern\">");
        if (pattern != null) {
            response.append(pattern);
        }
        response.append("</textarea><br>");
        response.append("<input type=\"submit\" name=\"Find Candidates\"/>\n");
        response.append("</form>\n");

        if (pattern != null && path != null) {
            response.append("Found candidates for pattern: " + pattern);
            response.append("<br>");

            URI u = new URI("http", null, "localhost", 9998, "/index/find", "path=" + path + "&pattern=" + pattern, null);
            
            for (String c : WebUtilities.requestStringArrayResponse(u)) {
                URI rel = new URI(null, null, "/index/cat", "path=" + path + "&relative=" + c, null);
                response.append("<a href=\"");
                response.append(rel.toASCIIString());
                response.append("\">");
                response.append(c);
                response.append("</a><br>");
            }
        }
        
        response.append("</body>\n");
        response.append("</html>\n");

        return response.toString();
    }

}
