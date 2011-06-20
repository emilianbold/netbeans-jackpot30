/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2011 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.backend.type.api;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndex.NameKind;
import org.netbeans.api.java.source.ClassIndex.SearchScope;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.jackpot30.backend.base.CategoryStorage;
import org.netbeans.modules.java.source.usages.ClassIndexManager;
import org.netbeans.modules.jumpto.type.GoToTypeAction;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;

/**
 *
 * @author lahvac
 */
@Path("/index/type")
public class API {

    @GET
    @Path("/search")
    @Produces("text/plain")
    public String findType(@QueryParam("path") String segment, @QueryParam("prefix") String prefix, @QueryParam("casesensitive") @DefaultValue("false") boolean casesensitive, @QueryParam("asynchronous") @DefaultValue(value="false") boolean asynchronous) throws IOException {
        assert !asynchronous;

        //copied (and converted to NameKind) from jumpto's GoToTypeAction:
        boolean exact = prefix.endsWith(" "); // NOI18N

        prefix = prefix.trim();

        if ( prefix.length() == 0) {
            return "";
        }

        NameKind nameKind;
        int wildcard = GoToTypeAction.containsWildCard(prefix);

        if (exact) {
            //nameKind = panel.isCaseSensitive() ? SearchType.EXACT_NAME : SearchType.CASE_INSENSITIVE_EXACT_NAME;
            nameKind = NameKind.SIMPLE_NAME;
        }
        else if ((GoToTypeAction.isAllUpper(prefix) && prefix.length() > 1) || GoToTypeAction.isCamelCase(prefix)) {
            nameKind = NameKind.CAMEL_CASE;
        }
        else if (wildcard != -1) {
            nameKind = casesensitive ? NameKind.REGEXP : NameKind.CASE_INSENSITIVE_REGEXP;
        }
        else {
            nameKind = casesensitive ? NameKind.PREFIX : NameKind.CASE_INSENSITIVE_PREFIX;
        }

        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        CategoryStorage srcRoots = CategoryStorage.forId(segment);

        for (URL srcRoot : srcRoots.getCategoryIndexFolders()) {
            if (!"rel".equals(srcRoot.getProtocol())) continue;
            String rootId = srcRoot.getPath().substring(1);
            List<String> currentResult = new ArrayList<String>();

            result.put(rootId, currentResult);

            ClassIndexManager.getDefault().createUsagesQuery(srcRoot, true).isValid();
            ClasspathInfo cpInfo = ClasspathInfo.create(ClassPath.EMPTY, ClassPath.EMPTY, ClassPathSupport.createClassPath(srcRoot));
            Set<ElementHandle<TypeElement>> names = new HashSet<ElementHandle<TypeElement>>(cpInfo.getClassIndex().getDeclaredTypes(prefix, nameKind, EnumSet.of(SearchScope.SOURCE)));

            if (nameKind == NameKind.CAMEL_CASE) {
                names.addAll(cpInfo.getClassIndex().getDeclaredTypes(prefix, NameKind.CASE_INSENSITIVE_PREFIX, EnumSet.of(SearchScope.SOURCE)));
            }

            for (ElementHandle<TypeElement> d : names) {
                currentResult.add(d.getBinaryName());
            }
        }

        return Pojson.save(result);
    }

}
