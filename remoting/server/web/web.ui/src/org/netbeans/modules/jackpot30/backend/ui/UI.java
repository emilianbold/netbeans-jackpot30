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

import freemarker.template.TemplateException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.backend.base.FreemarkerUtilities;
import org.netbeans.modules.jackpot30.backend.base.WebUtilities;
import static org.netbeans.modules.jackpot30.backend.base.WebUtilities.escapeForQuery;

/**
 *
 * @author lahvac
 */
@Path("/index/ui")
public class UI {

    private static final String URL_BASE = "http://localhost:9998/index";
//    private static final String URL_BASE = "http://lahoda.info/index";

    @GET
    @Path("/search")
    @Produces("text/html")
    public String searchType(@QueryParam("path") String path, @QueryParam("prefix") String prefix) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData = new HashMap<String, Object>();

        configurationData.put("paths", list());
        configurationData.put("selectedPath", path);
        configurationData.put("prefix", prefix);

        if (prefix != null && path != null) {
            URI u = new URI(URL_BASE + "/symbol/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
            @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
            Map<String, List<Map<String, Object>>> symbols = Pojson.load(LinkedHashMap.class, u);
            List<Map<String, Object>> results = new LinkedList<Map<String, Object>>();

            for (Entry<String, List<Map<String, Object>>> e : symbols.entrySet()) {
                for (Map<String, Object> found : e.getValue()) {
                    found.put("icon", getElementIcon((String) found.get("kind"), (Collection<String>) found.get("modifiers")));
                    if ("METHOD".equals(found.get("kind")) || "CONSTRUCTOR".equals(found.get("kind"))) {
                        found.put("displayName", found.get("simpleName") + decodeMethodSignature((String) found.get("signature")));
                    } else {
                        found.put("displayName", found.get("simpleName"));
                    }

                    results.add(found);
                }
            }

            URI typeSearch = new URI(URL_BASE + "/type/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
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

            configurationData.put("results", results);
        }

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/ui/ui-findType.html", configurationData);
    }

    @GET
    @Path("/show")
    @Produces("text/html")
    public String show(@QueryParam("path") String segment, @QueryParam("relative") String relative, @QueryParam("highlight") @DefaultValue("[]") String highlightSpec) throws URISyntaxException, IOException, TemplateException {
        URI u = new URI(URL_BASE + "/source/cat?path=" + escapeForQuery(segment) + "&relative=" + escapeForQuery(relative));
        String content = WebUtilities.requestStringResponse(u);
        List<Long> highlightSpans = Pojson.load(ArrayList.class, highlightSpec);
        Map<String, Object> configurationData = new HashMap<String, Object>();
        String[] highlights = colorTokens(content, highlightSpans);

        configurationData.put("spans", highlights[0]);
        configurationData.put("categories", highlights[1]);
        configurationData.put("code", translate(content));

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/ui/showCode.html", configurationData);
    }

    static String[] colorTokens(String content, List<Long> highlight) {
        TokenSequence<?> ts = TokenHierarchy.create(content, JavaTokenId.language()).tokenSequence();
        StringBuilder spans = new StringBuilder();
        StringBuilder cats  = new StringBuilder();
        long currentOffset = 0;
        boolean cont = false;

        while (cont || ts.moveNext()) {
            if (spans.length() > 0) spans.append(", ");

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

            spans.append(endOffset - currentOffset);
            String category = ts.token().id().primaryCategory();

            char cat;

            if ("keyword".equals(category)) {
                cat = 'K';
            } else if ("keyword-directive".equals(category)) {
                cat = 'K';
            } else if ("literal".equals(category)) {
                cat = 'K';
            } else if ("whitespace".equals(category)) {
                cat = 'W';
            } else if ("comment".equals(category)) {
                cat = 'C';
            } else if ("character".equals(category)) {
                cat = 'H';
            } else if ("number".equals(category)) {
                cat = 'N';
            } else if ("string".equals(category)) {
                cat = 'S';
            } else {
                cat = 'E';
            }

            if (foundHighlight) cat++;

            cats.append(cat);

            currentOffset = endOffset;
        }

        return new String[] {
            spans.toString(),
            cats.toString()
        };
    }

    //XXX: usages on fields do not work because the field signature in the index contain also the field type
    @GET
    @Path("/usages")
    @Produces("text/html")
    public String usages(@QueryParam("path") String segment, @QueryParam("signatures") final String signatures) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData = usagesSubclassesImpl(segment, signatures, "files", new ComputeSegmentData() {
            @Override public Object compute(String currentSegment) throws URISyntaxException, TemplateException, IOException {
                URI u = new URI(URL_BASE + "/usages/search?path=" + escapeForQuery(currentSegment) + "&signatures=" + escapeForQuery(simplify(signatures)));
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
    public String impl(@QueryParam("path") String segment, @QueryParam("type") String typeSignature, @QueryParam("method") final String methodSignature) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData;

        if (typeSignature != null) {
            final String type = strip(typeSignature, "CLASS:", "INTERFACE:", "ENUM:", "ANNOTATION_TYPE:");
            configurationData = usagesSubclassesImpl(segment, typeSignature, "implementors", new ComputeSegmentData() {
                @Override
                public Object compute(String currentSegment) throws URISyntaxException, TemplateException, IOException {
                    URI u = new URI(URL_BASE + "/implements/search?path=" + escapeForQuery(currentSegment) + "&type=" + escapeForQuery(type));
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
            final String method = methodSignature.substring(0, methodSignature.length() - 1);
            configurationData = usagesSubclassesImpl(segment, methodSignature, "implementors", new ComputeSegmentData() {
                @Override
                public Object compute(String currentSegment) throws URISyntaxException, TemplateException, IOException {
                    URI u = new URI(URL_BASE + "/implements/search?path=" + escapeForQuery(currentSegment) + "&method=" + escapeForQuery(method));
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

    private Map<String, Object> usagesSubclassesImpl(String segment, String elementSignature, String dataKey, ComputeSegmentData computeSegmentData) throws URISyntaxException, TemplateException, IOException {
        List<Map<String, String>> segments2Process = new ArrayList<Map<String, String>>();

        for (Map<String, String> m : list()) {
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

    private static List<Map<String, String>> list() throws URISyntaxException {
        List<Map<String, String>> result = new LinkedList<Map<String, String>>();

        for (String enc : WebUtilities.requestStringArrayResponse(new URI(URL_BASE + "/list"))) {
            Map<String, String> rootDesc = new HashMap<String, String>();
            String[] col = enc.split(":", 2);

            rootDesc.put("segment", col[0]);
            rootDesc.put("displayName", col[1]);
            result.add(rootDesc);
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

    static String simplify(String originalSignature) {
        if (   !originalSignature.startsWith("METHOD:")
            && !originalSignature.startsWith("CONSTRUCTOR:")) return originalSignature;
        StringBuilder target = new StringBuilder(originalSignature.length());
        int b = 0;

        for (char c : originalSignature.toCharArray()) {
            if (c == '<') {
                b++;
            } else if (c == '>') {
                b--;
            } else if (b == '^') {
                return target.toString();
            } else if (b == 0) {
                target.append(c);
            }
        }

        return target.delete(target.length() - 1, target.length()).toString();
    }

    static String strip(String originalSignature, String... prefixesToStrip) {
        for (String strip : prefixesToStrip) {
            if (originalSignature.startsWith(strip)) {
                return originalSignature.substring(strip.length());
            }
        }

        return originalSignature;
    }
}
