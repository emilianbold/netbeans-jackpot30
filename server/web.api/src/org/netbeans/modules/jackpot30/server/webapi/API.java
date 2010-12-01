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

package org.netbeans.modules.jackpot30.server.webapi;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.codeviation.pojson.Pojson;
import org.netbeans.modules.jackpot30.impl.examples.Example;
import org.netbeans.modules.jackpot30.impl.examples.Example.Option;
import org.netbeans.modules.jackpot30.impl.examples.LoadExamples;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;
import org.netbeans.modules.jackpot30.impl.indexing.FileBasedIndex;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.server.indexer.StandaloneFinder;

/**
 *
 * @author lahvac
 */
@Path("/")
public class API {

    @GET
    @Path("/find")
    @Produces("text/plain")
    public String find(@QueryParam("path") String segment, @QueryParam("pattern") String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (String candidate : StandaloneFinder.findCandidates(Cache.sourceRootForKey(segment), pattern)) {
            sb.append(candidate);
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/findSpans")
    @Produces("text/plain")
    public String findSpans(@QueryParam("path") String segment, @QueryParam("relativePath") String relativePath, @QueryParam("pattern") String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int o : StandaloneFinder.findCandidateOccurrenceSpans(Cache.sourceRootForKey(segment), relativePath, pattern)) {
            sb.append(o);
            sb.append(":");
        }

        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }

        return sb.toString();
    }

    @GET
    @Path("/list")
    @Produces("text/plain")
    public String list() throws IOException {
        StringBuilder sb = new StringBuilder();

        for (String root : Cache.knownSourceRoots()) {
            URL sourceRoot = Cache.sourceRootForKey(root).toURI().toURL();
            Index index = FileBasedIndex.get(sourceRoot);
            String displayName;

            if (index != null && index.getIndexInfo().displayName != null) {
                displayName = index.getIndexInfo().displayName;
            } else {
                String path = sourceRoot.getPath().replaceFirst("/$", "");
                
                displayName = path.substring(path.lastIndexOf('/') + 1);
            }

            sb.append(root);
            sb.append(":");
            sb.append(displayName);
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/cat")
    @Produces("text/plain")
    public String cat(@QueryParam("path") String segment, @QueryParam("relative") String relative) throws IOException {
        URL sourceRoot = Cache.sourceRootForKey(segment).toURI().toURL();
        Index index = FileBasedIndex.get(sourceRoot);

        if (index == null) {
            throw new IOException("No index");
        }

        CharSequence source = index.getSourceCode(relative);

        if (source == null) {
            throw new IOException("Source code not found");
        }
        
        return source.toString();
    }

    @GET
    @Path("/info")
    @Produces("text/plain")
    public String info(@QueryParam("path") String segment) throws IOException {
        URL sourceRoot = Cache.sourceRootForKey(segment).toURI().toURL();
        Index index = FileBasedIndex.get(sourceRoot);

        if (index == null) {
            throw new IOException("No index");
        }

        return Pojson.save(index.getIndexInfo());
    }

    @GET
    @Path("/examples")
    @Produces("text/plain")
    public String examples() throws IOException {
        List<Map<String, String>> examples = new LinkedList<Map<String, String>>();

        for (Example ex : LoadExamples.loadExamples()) {
            if (ex.getOptions().contains(Option.VERIFY) || ex.getOptions().contains(Option.FIX)) continue;
            Map<String, String> desc = new HashMap<String, String>();

            desc.put("displayName", ex.getDisplayName());
            desc.put("pattern", ex.getCode());

            examples.add(desc);
        }

        return Pojson.save(examples);
    }

    @GET
    @Path("/errors")
    @Produces("text/plain")
    public String errors(@QueryParam("pattern") String pattern) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (Diagnostic<? extends JavaFileObject> d : StandaloneFinder.parseAndReportErrors(pattern)) {
            sb.append(d.getMessage(null));
            sb.append("\n");
        }

        return sb.toString();
    }

    @GET
    @Path("/findDuplicates")
    @Produces("text/plain")
    public String findDuplicates(@QueryParam("path") String segment, @QueryParam("hashes") String hashes) throws IOException {
        Map<String, Map<String, Collection<? extends String>>> hash2Segment2Contains = new HashMap<String, Map<String, Collection<? extends String>>>();
        Collection<String> segments = new LinkedList<String>();

        if (segment != null) segments.add(segment);
        else {
            for (String key : Cache.knownSourceRoots()) {
                segments.add(key);
            }
        }

        Iterable<? extends String> hashesList = Arrays.asList(Pojson.load(String[].class, hashes));

        for (String key : segments) {
            Map<String, Collection<? extends String>> found = StandaloneFinder.containsHash(Cache.sourceRootForKey(key), hashesList);

            for (Entry<String, Collection<? extends String>> e : found.entrySet()) {
                Map<String, Collection<? extends String>> perRoot = hash2Segment2Contains.get(e.getKey());

                if (perRoot == null) {
                    hash2Segment2Contains.put(e.getKey(), perRoot = new HashMap<String, Collection<? extends String>>());
                }

                perRoot.put(key, e.getValue());
            }
        }

        return Pojson.save(hash2Segment2Contains);
    }
}
