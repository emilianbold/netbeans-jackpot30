/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.hudson;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.EditType;
import hudson.tasks.Builder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.netbeans.api.jackpot.hudson.IndexBuilder;
import org.netbeans.api.jackpot.hudson.IndexBuilder.IndexBuilderDescriptor;
import org.netbeans.api.jackpot.hudson.IndexBuilder.IndexingContext;

/**
 *
 * @author lahvac
 */
public final class IndexingBuilder extends Builder {

    private final String projectName;
    private final Set<IndexBuilder> indexers;
    
    public IndexingBuilder(StaplerRequest req, JSONObject json) throws FormException {
        projectName = json.getString("projectName");
        indexers = new HashSet<IndexBuilder>();

        for (IndexBuilderDescriptor desc : IndexBuilder.all()) {
            if (json.containsKey(desc.getJsonSafeClassName())) {
                indexers.add(desc.newInstance(req, json.getJSONObject(desc.getJsonSafeClassName())));
            }
        }
    }

    @DataBoundConstructor
    public IndexingBuilder(String projectName, Set<IndexBuilder> indexers) {
        this.projectName = projectName;
        this.indexers = indexers;
    }

    public String getProjectName() {
        return projectName;
    }

    public Set<IndexBuilder> getIndexers() {
        return indexers;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public boolean isIndexerEnabled(String indexer) {
        for (IndexBuilder b : indexers) {
            if (indexer.equals(b.getDescriptor().getJsonSafeClassName()))
                return true;
        }
        
        return false;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        Set<String> addedFiles = new HashSet<String>();
        Set<String> removedFiles = new HashSet<String>();

        for (Entry e : build.getChangeSet()) {
            for (AffectedFile f : e.getAffectedFiles()) {
                if (f.getEditType() == EditType.DELETE) {
                    removedFiles.add(stripLeadingSlash(f.getPath()));
                } else {
                    addedFiles.add(stripLeadingSlash(f.getPath()));
                }
            }
        }

        boolean success = true;

        for (IndexBuilder indexer : getIndexers()) {
            success &= indexer.index(new IndexingContext(getDescriptor().getCacheDir(), build, launcher, listener, addedFiles, removedFiles));
        }
        //XXX:
        File info = new File(Cache.findCache("jackpot30", 1002).findCacheRoot(build.getWorkspace().toURI().toURL()), "info");
        String jsonContent = readFully(info);
        JSONObject json = JSONObject.fromObject(jsonContent);

        String prjName = projectName;

        if (prjName == null || prjName.isEmpty()) {
            prjName = build.getParent().getDisplayName();
        }

        if (!prjName.equals(json.get("displayName"))) {
            json.put("displayName", prjName);
            write(info, JSONSerializer.toJSON(json).toString());
        }

        return success;
    }

    private static String readFully(File file) throws IOException {
        Reader in = null;
        StringBuilder result = new StringBuilder();

        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

            int read;

            while ((read = in.read()) != (-1)) {
                result.append((char) read);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(IndexBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        return result.toString();
    }

    private static void write(File file, String content) throws IOException {
        Writer out = null;
        StringBuilder result = new StringBuilder();

        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            out.write(content);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Logger.getLogger(IndexBuilder.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private String stripLeadingSlash(String path) {
        if (path.length() > 0 && path.charAt(0) == '/') {
            return path.substring(1);
        }

        return path;
    }
    
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends Descriptor<Builder> {

        private File cacheDir;

        public DescriptorImpl() {
            Cache.setStandaloneCacheRoot(cacheDir = new File(Hudson.getInstance().getRootDir(), "index").getAbsoluteFile());
        }

        public File getCacheDir() {
            return cacheDir;
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new IndexingBuilder(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "Run Indexers";
        }

        public List<? extends IndexBuilderDescriptor> getIndexers() {
            return IndexBuilder.all();
        }
    }

}
