/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.backend.impl.ui;

import java.util.Comparator;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.codeviation.pojson.Pojson;
import org.netbeans.modules.jackpot30.impl.WebUtilities;
import static org.netbeans.modules.jackpot30.impl.WebUtilities.escapeForQuery;

/**
 *
 * @author lahvac
 */
@Path("/index/ui")
public class UI {

    @GET
    @Path("/search")
    @Produces("text/html")
    public String search(@QueryParam("path") String path, @QueryParam("pattern") String pattern) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData = new HashMap<String, Object>();

        configurationData.put("paths", list());
        configurationData.put("selectedPath", path);
        configurationData.put("pattern", pattern);
        configurationData.put("patternEscaped", escapeForQuery(pattern));
        configurationData.put("examples", loadExamples());

        if (pattern != null && path != null) {
            URI u = new URI("http://localhost:9998/index/find?path=" + escapeForQuery(path) + "&pattern=" + escapeForQuery(pattern));
            List<Map<String, Object>> results = new LinkedList<Map<String, Object>>();
            long queryTime = System.currentTimeMillis();
            List<String> candidates = new ArrayList<String>(WebUtilities.requestStringArrayResponse(u));

            queryTime = System.currentTimeMillis() - queryTime;

            Collections.sort(candidates);

            for (String c : candidates) {
                Map<String, Object> found = new HashMap<String, Object>(3);

                found.put("relativePath", c);

                results.add(found);
            }

            configurationData.put("results", results);

            Map<String, Object> statistics = new HashMap<String, Object>();

            statistics.put("files", candidates.size());
            statistics.put("queryTime", queryTime);

            configurationData.put("statistics", statistics);
        }

        return processTemplate("ui-search.html", configurationData);
    }

//    @GET
//    @Path("/searchCategorized")
//    @Produces("text/html")
//    public String searchCategorized(@QueryParam("path") String path, @QueryParam("pattern") String pattern) throws URISyntaxException, IOException, TemplateException {
//        Map<String, Object> configurationData = new HashMap<String, Object>();
//
//        configurationData.put("paths", list());
//        configurationData.put("selectedPath", path);
//        configurationData.put("pattern", pattern);
//        configurationData.put("patternEscaped", escapeForQuery(pattern));
//        configurationData.put("examples", loadExamples());
//
//        if (pattern != null && path != null) {
//            Result queryResult = new DoQuery().doQuery(path, pattern, new Cancel() {
//                                                           public boolean isCancelled() {
//                                                               return false;
//                                                           }
//                                                       });
//
//            configurationData.put("result", queryResult.result);
//        }
//
//        return processTemplate("ui-search-categorized.html", configurationData);
//    }

    @GET
    @Path("/show")
    @Produces("text/html")
    public String show(@QueryParam("path") String path, @QueryParam("relative") String relativePath, @QueryParam("pattern") String pattern) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData = new HashMap<String, Object>();
        List<Map<String, String>> occurrences = new LinkedList<Map<String, String>>();

        configurationData.put("occurrences", occurrences);

        URI codeURL = new URI("http://localhost:9998/index/cat?path=" + escapeForQuery(path) + "&relative=" + escapeForQuery(relativePath));
        String code = WebUtilities.requestStringResponse(codeURL);

        if (pattern != null) {
            URI spansURL = new URI("http://localhost:9998/index/findSpans?path=" + escapeForQuery(path) + "&relativePath=" + escapeForQuery(relativePath) + "&pattern=" + escapeForQuery(pattern));
            int currentCodePos = 0;
            for (int[] span : parseSpans(WebUtilities.requestStringResponse(spansURL))) { //XXX: sorted!
                Map<String, String> occ = new HashMap<String, String>();
                occ.put("prefix", WebUtilities.escapeForHTMLElement(code.substring(currentCodePos, span[0])));
                occ.put("occurrence", WebUtilities.escapeForHTMLElement(code.substring(span[0], span[1])));
                occurrences.add(occ);
                currentCodePos = span[1];
            }

            configurationData.put("suffix", WebUtilities.escapeForHTMLElement(code.substring(currentCodePos, code.length())));
        } else {
            configurationData.put("suffix", WebUtilities.escapeForHTMLElement(code));
        }

        return processTemplate("ui-cat.html", configurationData);
    }
    
    @GET
    @Path("/snippet")
    @Produces("text/html")
    public String snippet(@QueryParam("path") String path, @QueryParam("relative") String relativePath, @QueryParam("pattern") String pattern) throws URISyntaxException, IOException, TemplateException {
        List<Map<String, String>> snippets = new LinkedList<Map<String, String>>();

        URI codeURL = new URI("http://localhost:9998/index/cat?path=" + escapeForQuery(path) + "&relative=" + escapeForQuery(relativePath));
        String code = WebUtilities.requestStringResponse(codeURL);
        URI spansURL = new URI("http://localhost:9998/index/findSpans?path=" + escapeForQuery(path) + "&relativePath=" + escapeForQuery(relativePath) + "&pattern=" + escapeForQuery(pattern));

        for (int[] span : parseSpans(WebUtilities.requestStringResponse(spansURL))) {
            snippets.add(prepareSnippet(code, span));
        }

        return processTemplate("ui-snippet.html", Collections.<String, Object>singletonMap("snippets", snippets));
    }

    @GET
    @Path("/apply")
    @Produces("text/html")
    public Response apply(@QueryParam("path") String path, @QueryParam("pattern") String pattern, @QueryParam("preview") @DefaultValue("") String preview, @QueryParam("download") @DefaultValue("") String download) throws URISyntaxException, IOException, TemplateException {
        if (!download.isEmpty()) {
            if (pattern != null && path != null) {
                URI u = new URI("http://localhost:9998/index/apply?path=" + escapeForQuery(path) + "&pattern=" + escapeForQuery(pattern));

                return Response.temporaryRedirect(u).header("meta", "Content-Disposition: download; filename=\"patch.diff\"").build();
            }
        }

        Map<String, Object> configurationData = new HashMap<String, Object>();

        configurationData.put("paths", list());
        configurationData.put("selectedPath", path);
        configurationData.put("pattern", pattern);
        configurationData.put("patternEscaped", escapeForQuery(pattern));
        configurationData.put("examples", loadExamples());

        if (pattern != null && path != null) {
            URI u = new URI("http://localhost:9998/index/apply?path=" + escapeForQuery(path) + "&pattern=" + escapeForQuery(pattern));
            long queryTime = System.currentTimeMillis();
            String diff = WebUtilities.requestStringResponse(u);

            queryTime = System.currentTimeMillis() - queryTime;

            configurationData.put("diff", diff);

            StringBuilder sb = new StringBuilder();

            for (String l : diff.split("\n")) {
                sb.append("<span");

                for (Entry<String, String> e : prefix2SpanName.entrySet()) {
                    if (l.startsWith(e.getKey())) {
                        sb.append(" class='" + e.getValue() + "'");
                        break;
                    }
                }

                sb.append(">");
                sb.append(l);
                sb.append("</span>\n");
            }

            configurationData.put("result", sb.toString());
        }

        return Response.ok(processTemplate("ui-apply.html", configurationData), "text/html").build();
    }

    @GET
    @Path("/searchType")
    @Produces("text/html")
    public String searchType(@QueryParam("path") String path, @QueryParam("prefix") String prefix) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData = new HashMap<String, Object>();

        configurationData.put("paths", list());
        configurationData.put("selectedPath", path);
        configurationData.put("prefix", prefix);

        if (prefix != null && path != null) {
            URI u = new URI("http://localhost:9998/index/findType?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
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

                    found.put("relativePath", e.getKey() + "/" + fqn.replace('.', '/') + ".java");

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

        return processTemplate("ui-findType.html", configurationData);
    }

    private static final Map<String, String> prefix2SpanName = new LinkedHashMap<String, String>();

    static {
        prefix2SpanName.put("-", "diff-removed");
        prefix2SpanName.put("+", "diff-added");
        prefix2SpanName.put("@@", "diff-hunk");
        prefix2SpanName.put("Index:", "diff-index");
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
    
    private static Iterable<int[]> parseSpans(String from) {
        if (from.isEmpty()) {
            return Collections.emptyList();
        }
        String[] split = from.split(":");
        List<int[]> result = new LinkedList<int[]>();

        for (int i = 0; i < split.length; i += 2) {
            result.add(new int[] {
                Integer.parseInt(split[i + 0].trim()),
                Integer.parseInt(split[i + 1].trim())
            });
        }

        return result;
    }

    private static final int DESIRED_CONTEXT = 2;

    private static Map<String, String> prepareSnippet(String code, int[] span) {
        int grandStart = span[0];
        int firstLineStart = grandStart = lineStart(code, grandStart);

        while (grandStart > 0 && contextLength(code.substring(grandStart, firstLineStart)) < DESIRED_CONTEXT)
            grandStart = lineStart(code, grandStart - 1);

        int grandEnd = span[1];
        int firstLineEnd = grandEnd = lineEnd(code, grandEnd);
        
        while (grandEnd < code.length() - 1 && contextLength(code.substring(firstLineEnd, grandEnd)) < DESIRED_CONTEXT)
            grandEnd = lineEnd(code, grandEnd + 1);

        Map<String, String> result = new HashMap<String, String>();
        
        result.put("prefix", WebUtilities.escapeForHTMLElement(code.substring(grandStart, span[0])));
        result.put("occurrence", WebUtilities.escapeForHTMLElement(code.substring(span[0], span[1])));
        result.put("suffix", WebUtilities.escapeForHTMLElement(code.substring(span[1], grandEnd)));

        return result;
    }

    private static int lineStart(String code, int o) {
        while (o > 0 && code.charAt(o) != '\n') {
            o--;
        }

        return o;
    }

    private static int lineEnd(String code, int o) {
        while (o < code.length() - 1 && code.charAt(o) != '\n') {
            o++;
        }

        return o;
    }

    private static int contextLength(String in) {
        return in.replaceAll("\n[ \t]*\n", "\n").trim().split("\n").length;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> loadExamples() throws IOException, URISyntaxException {
        return Pojson.load(LinkedList.class, new URI("http://localhost:9998/index/examples"));
    }

    private static String processTemplate(String template, Map<String, Object> configurationData) throws TemplateException, IOException {
        Configuration conf = new Configuration();

        conf.setTemplateLoader(new TemplateLoaderImpl());

        Template templ = conf.getTemplate(template);
        StringWriter out = new StringWriter();

        templ.process(configurationData, out);

        return out.toString();
    }

    private static final class TemplateLoaderImpl implements TemplateLoader {

        public Object findTemplateSource(String name) throws IOException {
            return TemplateLoaderImpl.class.getResourceAsStream(name);
        }

        public long getLastModified(Object templateSource) {
            return 0L;
        }

        public Reader getReader(Object templateSource, String encoding) throws IOException {
            InputStream in = (InputStream) templateSource;

            return new InputStreamReader(in);
        }

        public void closeTemplateSource(Object templateSource) throws IOException {
        }
    }

}