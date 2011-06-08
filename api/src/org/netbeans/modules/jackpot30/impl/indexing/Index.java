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

package org.netbeans.modules.jackpot30.impl.indexing;

import org.netbeans.modules.java.hints.jackpot.impl.pm.BulkSearch.BulkPattern;
import java.util.Map;
import com.sun.source.util.Trees;
import javax.lang.model.util.Types;
import org.netbeans.modules.jackpot30.impl.WebUtilities;
import java.util.ArrayList;
import com.sun.source.tree.CompilationUnitTree;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.source.CompilationInfo;
import org.openide.util.Exceptions;
import static org.netbeans.modules.jackpot30.impl.WebUtilities.escapeForQuery;

/**
 *
 * @author lahvac
 */
public abstract class Index {

    protected Index() {}

    public static Index createWithRemoteIndex(final URL sourceRoot, final String indexURL, final String subIndex) {
        return new Index() {
            @Override
            public IndexWriter openForWriting() throws IOException {
                throw new UnsupportedOperationException();
            }
            @Override
            public Collection<? extends String> findCandidates(BulkPattern pattern) throws IOException {
                try {
                    StringBuilder patterns = new StringBuilder();

                    for (String p : pattern.getPatterns()) {
                        patterns.append(p);
                        patterns.append(";;");
                    }

                    URI u = new URI(indexURL + "?path=" + escapeForQuery(subIndex) + "&pattern=" + escapeForQuery(patterns.toString()));

                    return new ArrayList<String>(WebUtilities.requestStringArrayResponse(u));
                } catch (URISyntaxException ex) {
                    //XXX: better handling?
                    Exceptions.printStackTrace(ex);
                    return Collections.emptyList();
                }
            }
            @Override
            public Map<String, Map<String, Integer>> findCandidatesWithFrequencies(BulkPattern pattern) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            @Override
            public @NonNull IndexInfo getIndexInfo() {
                IndexInfo result = IndexInfo.empty();

                try {
                    URI u = new URI(indexURL + "/info");

                    Pojson.update(result, WebUtilities.requestStringResponse(u));
                } catch (URISyntaxException ex) {
                    //XXX: better handling?
                    Exceptions.printStackTrace(ex);
                }
                
                return result;
            }
            @Override
            public CharSequence getSourceCode(String relativePath) {
                try {
                    URI u = new URI(indexURL + "/cat?path=" + escapeForQuery(subIndex) + "&relative=" + escapeForQuery(relativePath));

                    return WebUtilities.requestStringResponse(u);
                } catch (URISyntaxException ex) {
                    //XXX: better handling?
                    Exceptions.printStackTrace(ex);
                    return "";
                }
            }
        };
    }

    public abstract IndexWriter openForWriting() throws IOException;

    public abstract Collection<? extends String> findCandidates(BulkPattern pattern) throws IOException;
    public abstract Map<String, Map<String, Integer>> findCandidatesWithFrequencies(BulkPattern pattern) throws IOException;

    public abstract CharSequence getSourceCode(String relativePath);

    public abstract @NonNull IndexInfo getIndexInfo();

    public abstract static class IndexWriter {

        protected IndexWriter() throws IOException {}

        public abstract void record(URL source, final CompilationUnitTree cut, @NullAllowed AttributionWrapper attributed) throws IOException;

        public abstract void remove(String relativePath) throws IOException;

        public abstract void clear() throws IOException;
        
        public abstract void close() throws IOException;
        
    }

    public static final class AttributionWrapper {
        public final Trees trees;
        public final Types types;

        public AttributionWrapper(CompilationInfo info) {
            trees = info.getTrees();
            types = info.getTypes();
        }

        public AttributionWrapper(Trees trees, Types types) {
            this.trees = trees;
            this.types = types;
        }

    }

}
