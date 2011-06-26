/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2009-2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.backend.type.ui;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.codeviation.pojson.Pojson;
import org.netbeans.modules.jackpot30.backend.base.FreemarkerUtilities;
import org.netbeans.modules.jackpot30.backend.base.WebUtilities;
import static org.netbeans.modules.jackpot30.backend.base.WebUtilities.escapeForQuery;

/**
 *
 * @author lahvac
 */
@Path("/index/type")
public class UI {

    @GET
    @Path("/search/ui")
    @Produces("text/html")
    public String searchType(@QueryParam("path") String path, @QueryParam("prefix") String prefix) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData = new HashMap<String, Object>();

        configurationData.put("paths", list());
        configurationData.put("selectedPath", path);
        configurationData.put("prefix", prefix);

        if (prefix != null && path != null) {
            URI u = new URI("http://localhost:9998/index/type/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
            long queryTime = System.currentTimeMillis();
            @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
            Map<String, List<String>> types = Pojson.load(LinkedHashMap.class, u);
            List<Map<String, Object>> results = new LinkedList<Map<String, Object>>();

            queryTime = System.currentTimeMillis() - queryTime;

            for (Entry<String, List<String>> e : types.entrySet()) {
                for (String fqn : e.getValue()) {
                    Map<String, Object> found = new HashMap<String, Object>(3);

                    found.put("fqn", fqn);

                    if (fqn.contains("$")) {
                        fqn = fqn.substring(0, fqn.indexOf("$"));
                    }

                    found.put("relativePath", e.getKey() + /*"/" + */fqn.replace('.', '/') + ".java");

                    results.add(found);
                }
            }

            Collections.sort(results, new Comparator<Map<String, Object>>() {
                @Override public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                    return ((String) o1.get("fqn")).compareTo((String) o2.get("fqn"));
                }
            });
            
            configurationData.put("results", results);

            Map<String, Object> statistics = new HashMap<String, Object>();

            statistics.put("queryTime", queryTime);

            configurationData.put("statistics", statistics);
        }

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/type/ui/ui-findType.html", configurationData);
    }

    private static List<Map<String, String>> list() throws URISyntaxException {
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();

        for (String enc : WebUtilities.requestStringArrayResponse(new URI("http://localhost:9998/index/list"))) {
            Map<String, String> rootDesc = new HashMap<String, String>();
            String[] col = enc.split(":", 2);

            rootDesc.put("segment", col[0]);
            rootDesc.put("displayName", col[1]);
            result.add(rootDesc);
        }

        return result;
    }
    
}
