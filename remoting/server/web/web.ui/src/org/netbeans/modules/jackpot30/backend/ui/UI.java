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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
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
@Path("/index/ui")
public class UI {

    @GET
    @Path("/search")
    @Produces("text/html")
    public String searchType(@QueryParam("path") String path, @QueryParam("prefix") String prefix) throws URISyntaxException, IOException, TemplateException {
        Map<String, Object> configurationData = new HashMap<String, Object>();

        configurationData.put("paths", list());
        configurationData.put("selectedPath", path);
        configurationData.put("prefix", prefix);

        if (prefix != null && path != null) {
            URI u = new URI("http://localhost:9998/index/symbol/search?path=" + escapeForQuery(path) + "&prefix=" + escapeForQuery(prefix));
            long queryTime = System.currentTimeMillis();
            @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
            Map<String, List<Map<String, Object>>> symbols = Pojson.load(LinkedHashMap.class, u);
            List<Map<String, Object>> results = new LinkedList<Map<String, Object>>();

            queryTime = System.currentTimeMillis() - queryTime;

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

            Collections.sort(results, new Comparator<Map<String, Object>>() {
                @Override public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                    return ((String) o1.get("simpleName")).compareTo((String) o2.get("simpleName"));
                }
            });

            configurationData.put("results", results);

            Map<String, Object> statistics = new HashMap<String, Object>();

            statistics.put("queryTime", queryTime);

            configurationData.put("statistics", statistics);
        }

        return FreemarkerUtilities.processTemplate("org/netbeans/modules/jackpot30/backend/ui/ui-findType.html", configurationData);
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
}
