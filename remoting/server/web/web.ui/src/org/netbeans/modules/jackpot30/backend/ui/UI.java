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

package org.netbeans.modules.jackpot30.backend.ui;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.backend.base.CategoryStorage;
import org.netbeans.modules.jackpot30.backend.base.FreemarkerUtilities;
import org.netbeans.modules.jackpot30.backend.base.WebUtilities;
import static org.netbeans.modules.jackpot30.backend.base.WebUtilities.escapeForQuery;
import org.netbeans.modules.jackpot30.backend.ui.highlighting.ColoringAttributes;
import org.netbeans.modules.jackpot30.backend.ui.highlighting.ColoringAttributes.Coloring;
import org.netbeans.modules.jackpot30.backend.ui.highlighting.SemanticHighlighter;
import org.netbeans.modules.jackpot30.backend.ui.highlighting.TokenList;
import org.netbeans.modules.jackpot30.resolve.api.CompilationInfo;
import org.netbeans.modules.jackpot30.resolve.api.JavaUtils;
import org.netbeans.modules.jackpot30.resolve.api.ResolveService;

/**
 *
 * @author lahvac
 */
@Path("/index/ui")
public class UI {

    private static final String URL_BASE_OVERRIDE = null;
//    private static final String URL_BASE_OVERRIDE = "http://lahoda.info/";

    @GET
    @Path("/search")
    @Produces("text/html")
    public String searchType(@Context UriInfo uriInfo, @QueryParam("path") List<String> paths, @QueryParam("prefix") String prefix) throws URISyntaxException, IOException, TemplateException {
        String urlBase = URL_BASE_OVERRIDE != null ? URL_BASE_OVERRIDE : uriInfo.getBaseUri().toString();
        Map<String, Object> configurationData = new HashMap<String, Object>();
        Map<String,Map<String, String>> pathsList = list(urlBase);

        configurationData.put("paths", pathsList.values());
        configurationData.put("selectedPath", paths);
        configurationData.put("prefix", prefix);

        if (prefix != null && paths != null) {
            List<Map<String, Object>> results = new LinkedList<Map<String, Object>>();

            for (String path : paths) {
                URI u = new URI(urlBase + "index/symbol/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
                @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
                Map<String, List<Map<String, Object>>> symbols = Pojson.load(LinkedHashMap.class, u);
                Map<String, ?> segmentDescription = pathsList.get(path);

                for (Entry<String, List<Map<String, Object>>> e : symbols.entrySet()) {
                    for (Map<String, Object> found : e.getValue()) {
                        found.put("icon", getElementIcon((String) found.get("kind"), (Collection<String>) found.get("modifiers")));
                        if ("METHOD".equals(found.get("kind")) || "CONSTRUCTOR".equals(found.get("kind"))) {
                            found.put("displayName", found.get("simpleName") + decodeMethodSignature((String) found.get("signature")));
                        } else {
                            found.put("displayName", found.get("simpleName"));
                        }

                        found.put("segment", segmentDescription);

                        results.add(found);
                    }
                }

                URI typeSearch = new URI(urlBase + "index/type/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
                @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
                Map<String, List<String>> types = Pojson.load(LinkedHashMap.class, typeSearch);

                for (Entry<String, List<String>> e : types.entrySet()) {
                    for (String fqn : e.getValue()) {
                        Map<String, Object> result = new HashMap<String, Object>();

                        result.put("icon", getElementIcon("CLASS", Collections.<String>emptyList()));
                        result.put("kind", "CLASS");
                        result.put("fqn", fqn);

                        String displayName = fqn;
                        String enclosingFQN = "";

                        if (displayName.lastIndexOf('.') > 0) {
                            displayName = displayName.substring(displayName.lastIndexOf('.') + 1);
                            enclosingFQN = fqn.substring(0, fqn.lastIndexOf('.'));
                        }

                        if (displayName.lastIndexOf('$') > 0) {
                            displayName = displayName.substring(displayName.lastIndexOf('$') + 1);
                            enclosingFQN = fqn.substring(0, fqn.lastIndexOf('$'));
                        }

                        result.put("displayName", displayName);
                        result.put("enclosingFQN", enclosingFQN);

                        if (fqn.contains("$")) {
                            fqn = fqn.substring(0, fqn.indexOf("$"));
                        }

                        result.put("file", e.getKey() + fqn.replace('.', '/') + ".java");

                        result.put("segment", segmentDescription);

                        results.add(result);
                    }
                }

                Collections.sort(results, new Comparator<Map<String, Object>>() {
                    @Override public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                        int r = ((String) o1.get("displayName")).compareTo((String) o2.get("displayName"));

                        if (r == 0) {
                            r = ((String) o1.get("enclosingFQN")).compareTo((String) o2.get("enclosingFQN"));
                        }
                        return r;
                    }
                });

            }

            configurationData.put("results", results);
        }

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/ui/ui-findType.html", configurationData);
    }

    @GET
    @Path("/searchSymbol")
    @Produces("application/javascript")
    public String searchSymbol(@Context UriInfo uriInfo, @QueryParam("path") List<String> paths, @QueryParam("prefix") String prefix) throws URISyntaxException, IOException, TemplateException {
        String urlBase = URL_BASE_OVERRIDE != null ? URL_BASE_OVERRIDE : uriInfo.getBaseUri().toString();
        Map<String, Map<?, ?>> segment2Result = new HashMap<String, Map<?, ?>>();

        if (paths == null) {
            Map<String,Map<String, String>> pathsList = list(urlBase);
            paths = new ArrayList<String>(pathsList.keySet());
        }

        for (String path : paths) {
            URI u = new URI(urlBase + "index/symbol/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
            @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
            Map<String, List<Map<String, Object>>> symbols = Pojson.load(LinkedHashMap.class, u);

            URI typeSearch = new URI(urlBase + "index/type/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
            @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
            Map<String, List<String>> types = Pojson.load(LinkedHashMap.class, typeSearch);

            for (Entry<String, List<String>> e : types.entrySet()) {
                List<Map<String, Object>> thisSourceRootResults = new ArrayList<Map<String, Object>>(e.getValue().size());
                for (String fqn : e.getValue()) {
                    Map<String, Object> result = new HashMap<String, Object>();

                    result.put("kind", "CLASS");
                    result.put("fqn", fqn);

                    String simpleName = fqn;
                    String enclosingFQN = "";

                    if (simpleName.lastIndexOf('.') > 0) {
                        simpleName = simpleName.substring(simpleName.lastIndexOf('.') + 1);
                        enclosingFQN = fqn.substring(0, fqn.lastIndexOf('.'));
                    }

                    if (simpleName.lastIndexOf('$') > 0) {
                        simpleName = simpleName.substring(simpleName.lastIndexOf('$') + 1);
                        enclosingFQN = fqn.substring(0, fqn.lastIndexOf('$'));
                    }

                    result.put("simpleName", simpleName);
                    result.put("enclosingFQN", enclosingFQN);

                    if (fqn.contains("$")) {
                        fqn = fqn.substring(0, fqn.indexOf("$"));
                    }

                    result.put("file", fqn.replace('.', '/') + ".java");

                    thisSourceRootResults.add(result);
                }

                List<Map<String, Object>> putInto = symbols.get(e.getKey());

                if (putInto == null) symbols.put(e.getKey(), thisSourceRootResults);
                else putInto.addAll(thisSourceRootResults);
            }

            segment2Result.put(path, Collections.singletonMap("found", symbols));
        }

        return Pojson.save(segment2Result);
    }

    @GET
    @Path("/show")
    @Produces("text/html")
    public String show(@Context UriInfo uriInfo, @QueryParam("path") String segment, @QueryParam("relative") String relative, @QueryParam("highlight") String highlightSpec, @QueryParam("signature") String signature) throws URISyntaxException, IOException, TemplateException, InterruptedException {
        CategoryStorage category = CategoryStorage.forId(segment);
        String urlBase = URL_BASE_OVERRIDE != null ? URL_BASE_OVERRIDE : uriInfo.getBaseUri().toString();
        URI u = new URI(urlBase + "index/source/cat?path=" + escapeForQuery(segment) + "&relative=" + escapeForQuery(relative));
        String content = WebUtilities.requestStringResponse(u).replace("\r\n", "\n");
        List<Long> highlightSpans = new ArrayList<Long>();
        CompilationInfo info = ResolveService.parse(segment, relative);

        if (signature != null) {
            List<long[]> spans = ResolveService.usages(info, signature);

            for (long[] span : spans) {
                highlightSpans.add(span[0]);
                highlightSpans.add(span[1]);
            }
        }

        if (highlightSpec != null) {
            highlightSpans.addAll(Pojson.load(ArrayList.class, highlightSpec));
        }

        Map<String, Object> configurationData = new HashMap<String, Object>();
        HighlightData highlights = colorTokens(info, segment, relative, highlightSpans);

        configurationData.put("spans", toString(highlights.spans));
        configurationData.put("categories", toString(highlights.categories));
        configurationData.put("code", translate(content));
        configurationData.put("path", segment);
        configurationData.put("category", category.getDisplayName());
        configurationData.put("relative", relative);

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/ui/showCode.html", configurationData);
    }

    @GET
    @Path("/highlightData")
    @Produces("application/javascript")
    public String highlightData(@Context UriInfo uriInfo, @QueryParam("path") String segment, @QueryParam("relative") String relative) throws URISyntaxException, IOException, TemplateException, InterruptedException {
        CompilationInfo info = ResolveService.parse(segment, relative);
        HighlightData highlights = colorTokens(info, segment, relative, Collections.<Long>emptyList());

        return Pojson.save(highlights);
    }

    static HighlightData colorTokens(CompilationInfo info, String segment, String relative, List<Long> highlight) throws IOException, InterruptedException {
        TokenSequence<?> ts = info.getTokenHierarchy().tokenSequence(JavaTokenId.language());
        Map<Token, Coloring> semanticHighlights = SemanticHighlighter.computeHighlights(info, new TokenList(info, ts, new AtomicBoolean()));
        List<Long> spans = new ArrayList<Long>(ts.tokenCount());
        List<String> cats  = new ArrayList<String>(ts.tokenCount());
        long currentOffset = 0;
        boolean cont = false;

        ts.moveStart();

        while (cont || ts.moveNext()) {
            long endOffset = ts.offset() + ts.token().length();
            boolean foundHighlight = false;

            cont = false;
            
            for (int i = 0; i < highlight.size(); i += 2) {
                if (   currentOffset <= highlight.get(i)
                    && endOffset >= highlight.get(i)) {
                    if (currentOffset < highlight.get(i)) {
                        endOffset = highlight.get(i);
                        cont = true;
                    } else  if ((highlight.get(i + 1) + 1) < endOffset) {
                        endOffset = highlight.get(i + 1) + 1;
                        cont = true;
                        foundHighlight = true;
                    } else {
                        foundHighlight = true;
                    }
                } else if (   highlight.get(i) <= currentOffset
                           && (highlight.get(i + 1) + 1) > endOffset) {
                    foundHighlight = true;
                } else if (   currentOffset < (highlight.get(i + 1) + 1)
                           && (highlight.get(i + 1) + 1) < endOffset) {
                    endOffset = highlight.get(i + 1) + 1;
                    cont = true;
                    foundHighlight = true;
                }
            }

            spans.add(endOffset - currentOffset);
            String category = ts.token().id().primaryCategory();

            if ("keyword-directive".equals(category)) {
                category = "keyword";
            }

            if ("literal".equals(category)) {
                category = "keyword";
            }

            if (foundHighlight) {
                if (!category.isEmpty()) category += " ";
                category += "highlight";
            }

            Coloring coloring = semanticHighlights.get(ts.token());

            if (coloring != null) {
                for (ColoringAttributes ca : coloring) {
                    if (!category.isEmpty()) category += " ";
                    category += ca.name().toLowerCase();
                }
            }

            cats.add(category);

            currentOffset = endOffset;
        }

        return new HighlightData(cats, spans);
    }

    private static String toString(List<?> list) {
        StringBuilder result = new StringBuilder();

        for (Object o : list) {
            if (result.length() > 0) result.append(", ");
            result.append(String.valueOf(o));
        }

        return result.toString();
    }

    private static final class HighlightData {
        List<String> categories;
        List<Long> spans;
        public HighlightData(List<String> cats, List<Long> spans) {
            this.categories = cats;
            this.spans = spans;
        }
    }

    //XXX: usages on fields do not work because the field signature in the index contain also the field type
    @GET
    @Path("/usages")
    @Produces("text/html")
    public String usages(@Context UriInfo uriInfo, @QueryParam("path") String segment, @QueryParam("signatures") final String signatures) throws URISyntaxException, IOException, TemplateException {
        final String urlBase = URL_BASE_OVERRIDE != null ? URL_BASE_OVERRIDE : uriInfo.getBaseUri().toString();
        Map<String, Object> configurationData = usagesSubclassesImpl(urlBase, segment, signatures, "files", new ComputeSegmentData() {
            @Override public Object compute(String currentSegment) throws URISyntaxException, TemplateException, IOException {
                URI u = new URI(urlBase + "index/usages/search?path=" + escapeForQuery(currentSegment) + "&signatures=" + escapeForQuery(signatures));
                List<String> files = new ArrayList<String>(WebUtilities.requestStringArrayResponse(u));
                Collections.sort(files);
                return files;
            }
        });

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/ui/usages.html", configurationData);
    }

    @GET
    @Path("/implements")
    @Produces("text/html")
    public String impl(@Context UriInfo uriInfo, @QueryParam("path") String segment, @QueryParam("type") String typeSignature, @QueryParam("method") final String methodSignature) throws URISyntaxException, IOException, TemplateException {
        final String urlBase = URL_BASE_OVERRIDE != null ? URL_BASE_OVERRIDE : uriInfo.getBaseUri().toString();
        Map<String, Object> configurationData;

        if (typeSignature != null) {
            final String type = strip(typeSignature, "CLASS:", "INTERFACE:", "ENUM:", "ANNOTATION_TYPE:");
            configurationData = usagesSubclassesImpl(urlBase, segment, typeSignature, "implementors", new ComputeSegmentData() {
                @Override
                public Object compute(String currentSegment) throws URISyntaxException, TemplateException, IOException {
                    URI u = new URI(urlBase + "index/implements/search?path=" + escapeForQuery(currentSegment) + "&type=" + escapeForQuery(type));
                    Map<String, List<Map<String, String>>> data = Pojson.load(HashMap.class, u);
                    List<Map<String, String>> implementors = new ArrayList<Map<String, String>>();
                    for (Entry<String, List<Map<String, String>>> relpath2ImplementorsE : data.entrySet()) {
                        for (Map<String, String> implementorData : relpath2ImplementorsE.getValue()) {
                            Map<String, String> implementor = new HashMap<String, String>();

                            implementor.put("file", implementorData.get("file"));
                            implementor.put("class", implementorData.get("class"));
                            implementors.add(implementor);
                        }
                    }
                    Collections.sort(implementors, new Comparator<Map<String, String>>() {
                        @Override
                        public int compare(Map<String, String> o1, Map<String, String> o2) {
                            return o1.get("class").compareTo(o2.get("class"));
                        }
                    });
                    return implementors;
                }
            });

            configurationData.put("isSubtypes", true);
        } else {
            configurationData = usagesSubclassesImpl(urlBase, segment, methodSignature, "implementors", new ComputeSegmentData() {
                @Override
                public Object compute(String currentSegment) throws URISyntaxException, TemplateException, IOException {
                    URI u = new URI(urlBase + "index/implements/search?path=" + escapeForQuery(currentSegment) + "&method=" + escapeForQuery(methodSignature));
                    Map<String, List<Map<String, String>>> data = Pojson.load(HashMap.class, u);
                    List<Map<String, String>> implementors = new ArrayList<Map<String, String>>();
                    for (Entry<String, List<Map<String, String>>> relpath2ImplementorsE : data.entrySet()) {
                        for (Map<String, String> implementorData : relpath2ImplementorsE.getValue()) {
                            Map<String, String> implementor = new HashMap<String, String>();

                            implementor.put("file", implementorData.get("file"));
                            implementor.put("class", implementorData.get("enclosingFQN"));
                            implementors.add(implementor);
                        }
                    }
                    Collections.sort(implementors, new Comparator<Map<String, String>>() {
                        @Override
                        public int compare(Map<String, String> o1, Map<String, String> o2) {
                            return o1.get("class").compareTo(o2.get("class"));
                        }
                    });
                    return implementors;
                }
            });

            configurationData.put("isSubtypes", false);
        }

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/ui/implementors.html", configurationData);
    }

    private Map<String, Object> usagesSubclassesImpl(String urlBase, String segment, String elementSignature, String dataKey, ComputeSegmentData computeSegmentData) throws URISyntaxException, TemplateException, IOException {
        List<Map<String, String>> segments2Process = new ArrayList<Map<String, String>>();

        for (Map<String, String> m : list(urlBase).values()) {
            if (segment != null) {
                if (segment.equals(m.get("segment"))) {
                    segments2Process.add(m);
                }
            } else {
                segments2Process.add(m);
            }
        }

        Map<String, Object> configurationData = new HashMap<String, Object>();

        configurationData.put("elementDisplayName", elementDisplayName(elementSignature)); //TODO
        configurationData.put("elementSignature", elementSignature); //TODO

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>(segments2Process.size());

        for (Map<String, String> m : segments2Process) {
            Map<String, Object> rootResults = new HashMap<String, Object>();
            String currentSegment = m.get("segment");

            rootResults.put("rootDisplayName", m.get("displayName"));
            rootResults.put("rootPath", currentSegment);
            rootResults.put(dataKey, computeSegmentData.compute(currentSegment));

            results.add(rootResults);
        }

        configurationData.put("results", results);

        return configurationData;
    }

    private interface ComputeSegmentData {
        public Object compute(String segment) throws URISyntaxException, TemplateException, IOException;
    }

    @GET
    @Path("/searchUsages")
    @Produces("application/javascript")
    public String searchUsages(@Context UriInfo uriInfo, @QueryParam("path") List<String> paths, @QueryParam("signature") final String signature) throws URISyntaxException, IOException, TemplateException {
        String urlBase = URL_BASE_OVERRIDE != null ? URL_BASE_OVERRIDE : uriInfo.getBaseUri().toString();
        Map<String, Map<?, ?>> segment2Result = new HashMap<String, Map<?, ?>>();

        if (paths == null) {
            Map<String,Map<String, String>> pathsList = list(urlBase);
            paths = new ArrayList<String>(pathsList.keySet());
        }

        for (String path : paths) {
            Map<String, Map<String, Object>> usageFile2Flags = new HashMap<String, Map<String, Object>>();
            
            URI usagesURI = new URI(urlBase + "index/usages/search?path=" + escapeForQuery(path) + "&signatures=" + escapeForQuery(signature));
            List<String> files = new ArrayList<String>(WebUtilities.requestStringArrayResponse(usagesURI));

            for (String file : files) {
                Map<String, Object> flags = usageFile2Flags.get(file);

                if (flags == null) {
                    usageFile2Flags.put(file, flags = new HashMap<String, Object>());
                }

                flags.put("usage", "true");
            }

            if (signature.startsWith("CLASS:") || signature.startsWith("INTERFACE:") || signature.startsWith("ENUM:") || signature.startsWith("ANNOTATION_TYPE:")) {
                final String type = strip(signature, "CLASS:", "INTERFACE:", "ENUM:", "ANNOTATION_TYPE:");
                URI u = new URI(urlBase + "index/implements/search?path=" + escapeForQuery(path) + "&type=" + escapeForQuery(type));
                Map<String, List<Map<String, String>>> data = Pojson.load(HashMap.class, u);
                for (Entry<String, List<Map<String, String>>> relpath2ImplementorsE : data.entrySet()) {
                    for (Map<String, String> implementorData : relpath2ImplementorsE.getValue()) {
                        String file = implementorData.get("file");
                        Map<String, Object> flags = usageFile2Flags.get(file);

                        if (flags == null) {
                            usageFile2Flags.put(file, flags = new HashMap<String, Object>());
                        }

                        List<String> implementors = (List<String>) flags.get("subtypes");

                        if (implementors == null) {
                            flags.put("subtypes", implementors = new ArrayList<String>());
                        }

                        implementors.add(implementorData.get("class"));
                    }
                }
            } else if (signature.startsWith("METHOD:")) {
                URI u = new URI(urlBase + "index/implements/search?path=" + escapeForQuery(path) + "&method=" + escapeForQuery(signature));
                Map<String, List<Map<String, String>>> data = Pojson.load(HashMap.class, u);
                for (Entry<String, List<Map<String, String>>> relpath2ImplementorsE : data.entrySet()) {
                    for (Map<String, String> implementorData : relpath2ImplementorsE.getValue()) {
                        String file = implementorData.get("file");
                        Map<String, Object> flags = usageFile2Flags.get(file);

                        if (flags == null) {
                            usageFile2Flags.put(file, flags = new HashMap<String, Object>());
                        }

                        List<String> overridersParents = (List<String>) flags.get("overridersParents");

                        if (overridersParents == null) {
                            flags.put("overridersParents", overridersParents = new ArrayList<String>());
                        }

                        overridersParents.add(implementorData.get("enclosingFQN"));
                    }
                }
            }
            
            segment2Result.put(path, usageFile2Flags);
        }

        return Pojson.save(segment2Result);
    }

    @GET
    @Path("/localUsages")
    @Produces("application/javascript")
    public String localUsages(@Context UriInfo uriInfo, @QueryParam("path") String segment, @QueryParam("relative") String relative, @QueryParam("signature") final String signature, @QueryParam("usages") boolean usages) throws URISyntaxException, IOException, TemplateException, InterruptedException {
        final CompilationInfo info = ResolveService.parse(segment, relative);
        List<long[]> result = new ArrayList<long[]>();
        
        for (long[] span : ResolveService.usages(info, signature)) {
            result.add(new long[] {span[2], span[3]});
        }

        return Pojson.save(result);
    }

    private static String elementDisplayName(String signatures) {
        StringBuilder elementDisplayName = new StringBuilder();
        String[] splitSignature = signatures.split(":");

        if (splitSignature.length == 2) {
            return splitSignature[1];
        }

        elementDisplayName.append(splitSignature[2]);

        if ("METHOD".equals(splitSignature[0])) {
            elementDisplayName.append(decodeMethodSignature(splitSignature[3]));
        }

        elementDisplayName.append(" in ");
        elementDisplayName.append(splitSignature[1]);

        return elementDisplayName.toString();
    }

    private static Map<String, Map<String, String>> list(String urlBase) throws URISyntaxException {
        Map<String, Map<String, String>> result = new LinkedHashMap<String, Map<String, String>>();

        for (String enc : WebUtilities.requestStringArrayResponse(new URI(urlBase + "index/list"))) {
            Map<String, String> rootDesc = new HashMap<String, String>();
            String[] col = enc.split(":", 2);

            rootDesc.put("segment", col[0]);
            rootDesc.put("displayName", col[1]);
            result.put(col[0], rootDesc);
        }

        return result;
    }
    
    //Copied from Icons, NetBeans proper
    private static final String GIF_EXTENSION = ".gif";
    private static final String PNG_EXTENSION = ".png";

    public static String getElementIcon(String elementKind, Collection<String> modifiers ) {
        if ("PACKAGE".equals(elementKind)) {
            return "package" + GIF_EXTENSION;
        } else if ("ENUM".equals(elementKind)) {
            return "enum" + PNG_EXTENSION;
        } else if ("ANNOTATION_TYPE".equals(elementKind)) {
            return "annotation" + PNG_EXTENSION;
        } else if ("CLASS".equals(elementKind)) {
            return "class" + PNG_EXTENSION;
        } else if ("INTERFACE".equals(elementKind)) {
            return "interface"  + PNG_EXTENSION;
	} else if ("FIELD".equals(elementKind)) {
            return getIconName("field", PNG_EXTENSION, modifiers );
	} else if ("ENUM_CONSTANT".equals(elementKind)) {
            return "constant" + PNG_EXTENSION;
	} else if ("CONSTRUCTOR".equals(elementKind)) {
            return getIconName("constructor", PNG_EXTENSION, modifiers );
	} else if (   "INSTANCE_INIT".equals(elementKind)
                   || "STATIC_INIT".equals(elementKind)) {
            return "initializer" + (modifiers.contains("STATIC") ? "Static" : "") + PNG_EXTENSION;
	} else if ("METHOD".equals(elementKind)) {
            return getIconName("method", PNG_EXTENSION, modifiers );
        } else {
            return "";
        }
    }

    // Private Methods ---------------------------------------------------------

    private static String getIconName(String typeName, String extension, Collection<String> modifiers ) {

        StringBuilder fileName = new StringBuilder( typeName );

        if ( modifiers.contains("STATIC") ) {
            fileName.append( "Static" );                        //NOI18N
        }
        if ( modifiers.contains("PUBLIC") ) {
            return fileName.append( "Public" ).append( extension ).toString();      //NOI18N
        }
        if ( modifiers.contains("PROTECTED") ) {
            return fileName.append( "Protected" ).append( extension ).toString();   //NOI18N
        }
        if ( modifiers.contains("PRIVATE") ) {
            return fileName.append( "Private" ).append( extension ).toString();     //NOI18N
        }
        return fileName.append( "Package" ).append( extension ).toString();         //NOI18N

    }

    static String decodeMethodSignature(String signature) {
        assert signature.charAt(0) == '(' || signature.charAt(0) == '<';

        int[] pos = new int[] {1};

        if (signature.charAt(0) == '<') {
            int b = 1;

            while (b > 0) {
                switch (signature.charAt(pos[0]++)) {
                    case '<': b++; break;
                    case '>': b--; break;
                }
            };

            pos[0]++;
        }

        StringBuilder result = new StringBuilder();

        result.append("(");

        while (signature.charAt(pos[0]) != ')') {
            if (result.charAt(result.length() - 1) != '(') {
                result.append(", ");
            }

            result.append(decodeSignatureType(signature, pos));
        }

        result.append(')');

        return result.toString();
    }

    static String decodeSignatureType(String signature, int[] pos) {
        char c = signature.charAt(pos[0]++);
        switch (c) {
            case 'V': return "void";
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'S': return "short";
            case 'I': return "int";
            case 'J': return "long";
            case 'C': return "char";
            case 'F': return "float";
            case 'D': return "double";
            case '[': return decodeSignatureType(signature, pos) + "[]";
            case 'L': {
                int lastSlash = pos[0];
                StringBuilder result = new StringBuilder();

                while (signature.charAt(pos[0]) != ';' && signature.charAt(pos[0]) != '<') {
                    if (signature.charAt(pos[0]) == '/') {
                        lastSlash = pos[0] + 1;
                    }
                    if (signature.charAt(pos[0]) == '$') {
                        lastSlash = pos[0] + 1;
                    }
                    pos[0]++;
                }

                result.append(signature.substring(lastSlash, pos[0]));

                if (signature.charAt(pos[0]++) == '<') {
                    result.append('<');

                    while (signature.charAt(pos[0]) != '>') {
                        if (result.charAt(result.length() - 1) != '<') {
                            result.append(", ");
                        }
                        result.append(decodeSignatureType(signature, pos));
                    }

                    result.append('>');
                    pos[0] += 2;
                }


                return result.toString();
            }
            case 'T': {
                StringBuilder result = new StringBuilder();

                while (signature.charAt(pos[0]) != ';') {
                    result.append(signature.charAt(pos[0]));
                    pos[0]++;
                }

                pos[0]++;
                
                return result.toString();
            }
            case '+': return "? extends " + decodeSignatureType(signature, pos);
            case '-': return "? super " + decodeSignatureType(signature, pos);
            case '*': return "?";
            default: return "unknown";
        }
    }

    private static String[] c = new String[] {"&", "<"}; // NOI18N
    private static String[] tags = new String[] {"&amp;", "&lt;"}; // NOI18N

    private String translate(String input) {
        for (int cntr = 0; cntr < c.length; cntr++) {
            input = input.replaceAll(c[cntr], tags[cntr]);
        }

        return input;
    }

    static String strip(String originalSignature, String... prefixesToStrip) {
        for (String strip : prefixesToStrip) {
            if (originalSignature.startsWith(strip)) {
                return originalSignature.substring(strip.length());
            }
        }

        return originalSignature;
    }

    @GET
    @Path("/target")
//    @Produces("text/html")
    public String target(@QueryParam("path") String segment, @QueryParam("relative") String relative, @QueryParam("position") final long position) throws URISyntaxException, IOException, TemplateException, InterruptedException {
        final CompilationInfo info = ResolveService.parse(segment, relative);
        final boolean[] declaration = new boolean[1];
        final long[] targetPosition = new long[] { -2 };
        final String[] signature = new String[1];
        final ElementKind[] kind = new ElementKind[1];

        new TreePathScanner<Void, Void>() {
            @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                handleUsage();
                return super.visitIdentifier(node, p);
            }
            @Override public Void visitMemberSelect(MemberSelectTree node, Void p) {
                handleUsage();
                return super.visitMemberSelect(node, p);
            }
            private void handleUsage() {
                Element el = info.getTrees().getElement(getCurrentPath());

                if (el == null) return;

                long[] span = ResolveService.nameSpan(info, getCurrentPath());

                if (span[0] <= position && position <= span[1]) {
                    if (JavaUtils.SUPPORTED_KINDS.contains(el.getKind())) {
                        signature[0] = JavaUtils.serialize(ElementHandle.create(el));
                    }

                    TreePath tp = info.getTrees().getPath(el);

                    if (tp != null && tp.getCompilationUnit() == info.getCompilationUnit()) {
                        targetPosition[0] = info.getTrees().getSourcePositions().getStartPosition(tp.getCompilationUnit(), tp.getLeaf());
                    }
                }
            }
            @Override public Void visitClass(ClassTree node, Void p) {
                handleDeclaration();
                return super.visitClass(node, p);
            }
            @Override public Void visitMethod(MethodTree node, Void p) {
                handleDeclaration();
                return super.visitMethod(node, p);
            }
            @Override public Void visitVariable(VariableTree node, Void p) {
                handleDeclaration();
                return super.visitVariable(node, p);
            }
            private void handleDeclaration() {
                Element el = info.getTrees().getElement(getCurrentPath());

                if (el == null) return;

                long[] span = ResolveService.nameSpan(info, getCurrentPath());

                if (span[2] <= position && position <= span[3]) {
                    if (JavaUtils.SUPPORTED_KINDS.contains(el.getKind())) {
                        signature[0] = JavaUtils.serialize(ElementHandle.create(el));
                    }

                    declaration[0] = true;
                    kind[0] = el.getKind();
                }
            }
        }.scan(info.getCompilationUnit(), null);

        Map<String, Object> result = new HashMap<String, Object>();

        if (declaration[0]) {
            if (signature[0] != null) {
                List<Map<String, String>> menu = new ArrayList<Map<String, String>>();

                menu.add(menuMap("Usages in current project", "/index/ui/usages?path=" + escapeForQuery(segment) + "&signatures=" + escapeForQuery(signature[0])));
                menu.add(menuMap("Usages in all known projects", "/index/ui/usages?signatures=" + escapeForQuery(signature[0])));

                switch (kind[0]) {
                    case METHOD:
                        menu.add(menuMap("Overriders in the current project", "/index/ui/implements?path=" + escapeForQuery(segment) + "&method=" + escapeForQuery(signature[0])));
                        menu.add(menuMap("Overriders in all known projects", "/index/ui/implements?method=" + escapeForQuery(signature[0])));
                        break;
                    case CLASS: case INTERFACE: case ENUM: case ANNOTATION_TYPE:
                        menu.add(menuMap("Subtypes in the current project", "/index/ui/implements?path=" + escapeForQuery(segment) + "&type=" + escapeForQuery(signature[0])));
                        menu.add(menuMap("Subtypes in all known projects", "/index/ui/implements?type=" + escapeForQuery(signature[0])));
                        break;
                }
                result.put("menu", menu);
                result.put("signature", signature[0]);
            }
        } else {
            if (targetPosition[0] != (-2)) {
                result.put("position", targetPosition[0]);
            } else if (signature[0] != null) {
                String targetSignature = signature[0];
                String source = ResolveService.resolveSource(segment, relative, targetSignature);

                result.put("signature", targetSignature);

                if (source != null) {
                    result.put("path", segment);
                    result.put("source", source);
                } else {
                    String singleSource = null;
                    String singleSourceSegment = null;
                    boolean multipleSources = false;
                    List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

                    for (Entry<? extends CategoryStorage, ? extends Iterable<? extends String>> e : ResolveService.findSourcesContaining(targetSignature).entrySet()) {
                        Map<String, Object> categoryData = new HashMap<String, Object>();

                        categoryData.put("rootDisplayName", e.getKey().getDisplayName());
                        categoryData.put("rootPath", e.getKey().getId());
                        categoryData.put("files", e.getValue());

                        if (!multipleSources) {
                            Iterator<? extends String> fIt = e.getValue().iterator();

                            if (fIt.hasNext()) {
                                singleSource = fIt.next();
                                singleSourceSegment = e.getKey().getId();
                            }

                            if (fIt.hasNext())
                                multipleSources = true;
                        }

                        results.add(categoryData);
                    }

                    if (singleSource != null && !multipleSources) {
                        //TODO: will automatically jump to the single known target - correct?
                        result.put("path", singleSourceSegment);
                        result.put("source", singleSource);
                    } else if (!results.isEmpty()) {
                        result.put("targets", results);
                    }
                }
            }
        }

        return Pojson.save(result);
    }

    private static Map<String, String> menuMap(String displayName, String url) {
        Map<String, String> result = new HashMap<String, String>();

        result.put("displayName", displayName);
        result.put("url", url);

        return result;
    }

    @GET
    @Path("/declarationSpan")
//    @Produces("text/html")
    public String declarationSpan(@QueryParam("path") String segment, @QueryParam("relative") String relative, @QueryParam("signature") String signature) throws URISyntaxException, IOException, TemplateException, InterruptedException {
        CompilationInfo info = ResolveService.parse(segment, relative);
        long[] span = ResolveService.declarationSpans(info, signature);

        if (span == null) {
            span = new long[] {-1, -1, -1, -1};
        }

        return Pojson.save(span);
    }
}
