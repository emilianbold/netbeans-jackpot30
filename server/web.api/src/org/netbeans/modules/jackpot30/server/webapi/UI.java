/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

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
