package org.netbeans.modules.jackpot30.server.webapi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 *
 * @author lahvac
 */
@Path("/ui")
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
        for (String c : requestStringArrayResponse(new URI("http://localhost:9998/list"))) {
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

            URI u = new URI("http", null, "localhost", 9998, "/find", "path=" + path + "&pattern=" + pattern, null);
            
            for (String c : requestStringArrayResponse(u)) {
                response.append(c);
                response.append("<br>");
            }
        }
        
        response.append("</body>\n");
        response.append("</html>\n");

        return response.toString();
    }

    private static List<String> requestStringArrayResponse (URI uri) {
        final ArrayList<String> result = new ArrayList<String> ();
        final URL url;
        try {
            url = uri.toURL();
            final URLConnection urlConnection = url.openConnection ();
            urlConnection.connect ();
            final Object content = urlConnection.getContent ();
//            System.out.println (content);
//            System.out.println (content.getClass ());
            final InputStream inputStream = (InputStream) content;
            final BufferedReader reader = new BufferedReader (new InputStreamReader (inputStream, "ASCII"));
            try {
                for (;;) {
                    String line = reader.readLine ();
                    if (line == null)
                        break;
                    result.add (line);
                }
            } finally {
                reader.close ();
            }
        } catch (IOException e) {
            e.printStackTrace ();  // TODO
        }
        return result;
    }
}
