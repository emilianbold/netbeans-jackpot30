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
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Hudson;
import hudson.remoting.VirtualChannel;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author lahvac
 */
public class IndexingBuilder extends Builder {

    private final String projectName;
    private final String toolName;
    private final String indexSubDirectory;
    private final String ignorePatterns;
    
    public IndexingBuilder(StaplerRequest req, JSONObject json) throws FormException {
        projectName = json.getString("projectName");
        toolName = json.optString("toolName", IndexingTool.DEFAULT_INDEXING_NAME);
        indexSubDirectory = json.optString("indexSubDirectory", "");
        ignorePatterns = json.optString("ignorePatterns", "");
    }

    @DataBoundConstructor
    public IndexingBuilder(String projectName, String toolName, String indexSubDirectory, String ignorePatterns) {
        this.projectName = projectName;
        this.toolName = toolName;
        this.indexSubDirectory = indexSubDirectory;
        this.ignorePatterns = ignorePatterns != null ? ignorePatterns : "";
    }

    public String getProjectName() {
        return projectName;
    }

    public String getToolName() {
        return toolName;
    }

    public String getIndexSubDirectory() {
        return indexSubDirectory;
    }

    public String getIgnorePatterns() {
        return ignorePatterns;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        boolean success = doIndex(getDescriptor().getCacheDir(), build, launcher, listener);

        return success;
    }

    protected/*tests*/ boolean doIndex(File cacheDir, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        IndexingTool t = findSelectedTool();

        if (t == null) {
            listener.getLogger().println("Cannot find indexing tool: " + toolName);
            return false;
        }

        t = t.forNode(build.getBuiltOn(), listener);

        listener.getLogger().println("Looking for projects in: " + build.getWorkspace().getRemote());

        FilePath base = indexSubDirectory == null || indexSubDirectory.isEmpty() ? build.getWorkspace() : build.getWorkspace().child(indexSubDirectory); //XXX: child also supports absolute paths! absolute paths should not be allowed here (for security)
        RemoteResult res = base.act(new FindProjects(getDescriptor().getProjectMarkers(), getDescriptor().getIgnorePattern(), getIgnorePatterns()));

        listener.getLogger().println("Running: " + toolName + " on projects: " + res);

        String codeName = build.getParent().getName();
        ArgumentListBuilder args = new ArgumentListBuilder();
        FilePath targetZip = build.getBuiltOn().getRootPath().createTempFile(codeName, "zip");

        //XXX: there should be a way to specify Java runtime!
        args.add(new File(t.getHome(), "index.sh")); //XXX
        args.add(codeName);
        args.add(projectName); //XXX
        args.add(targetZip);
        args.add(res.root);
        args.add(res.foundProjects.toArray(new String[0]));

        Proc indexer = launcher.launch().pwd(base)
                                        .cmds(args)
                                        .envs("JPT30_INFO=BUILD_ID=" + build.getNumber())
                                        .stdout(listener)
                                        .start();

        indexer.join();

        InputStream ins = targetZip.read();

        try {
            UploadIndex.uploadIndex(codeName, ins);
        } finally {
            ins.close();
            targetZip.delete();
        }

        return true;
    }

    private void dumpToFile(File target, Set<String> files) throws IOException {
        Writer out = new OutputStreamWriter(new FileOutputStream(target));

        try {
            for (String f : files) {
                out.write(f);
                out.write("\n");
            }
        } finally {
            out.close();
        }
    }

    public IndexingTool findSelectedTool() {
        for (IndexingTool t : getDescriptor().getIndexingTools()) {
            if (toolName.equals(t.getName())) return t;
        }

        return null;
    }

    private static void findProjects(File root, Collection<String> result, Pattern markers, Pattern ignore, Pattern perProjectIgnore, StringBuilder relPath) {
        int len = relPath.length();
        boolean first = relPath.length() == 0;

        Matcher m = markers.matcher(relPath);

        if (m.matches()) {
            if (perProjectIgnore == null || !perProjectIgnore.matcher(relPath).matches()) {
                result.add(m.group(1));
            }
        }

        File[] children = root.listFiles();

        if (children != null) {
            Arrays.sort(children, new Comparator<File>() {
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            for (File c : children) {
                if (ignore.matcher(c.getName()).matches()) continue;
                if (!first)
                    relPath.append("/");
                relPath.append(c.getName());
                findProjects(c, result, markers, ignore, perProjectIgnore, relPath);
                relPath.delete(len, relPath.length());
            }
        }
    }

    private static final class RemoteResult implements Serializable {
        private final Collection<String> foundProjects;
        private final String root;
        public RemoteResult(Collection<String> foundProjects, String root) {
            this.foundProjects = foundProjects;
            this.root = root;
        }
    }
    
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends Descriptor<Builder> {

        private File cacheDir;
        private static final String DEFAULT_PROJECT_MARKERS = "(.*)/(nbproject/project.xml|pom.xml)";
        private String projectMarkers = DEFAULT_PROJECT_MARKERS;
        private static final String DEFAULT_IGNORE_PATTERN = "CVS|\\.hg|\\.svn";
        private String ignorePattern = DEFAULT_IGNORE_PATTERN;

        public DescriptorImpl() {
            Cache.setStandaloneCacheRoot(cacheDir = new File(Hudson.getInstance().getRootDir(), "index").getAbsoluteFile());
            load();
        }

        public File getCacheDir() {
            return cacheDir;
        }

        public String getProjectMarkers() {
            return projectMarkers;
        }

        public  void setProjectMarkers(String projectMarkers) {
            this.projectMarkers = projectMarkers;
        }

        public String getIgnorePattern() {
            return ignorePattern;
        }

        public  void setIgnorePattern(String ignorePattern) {
            this.ignorePattern = ignorePattern;
        }

        @Override
        public Builder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new IndexingBuilder(req, formData);
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            cacheDir = new File(json.getString("cacheDir"));
            projectMarkers = json.optString("projectMarkers", DEFAULT_PROJECT_MARKERS);
            ignorePattern = json.optString("ignorePattern", DEFAULT_IGNORE_PATTERN);

            save();
            
            return super.configure(req, json);
        }

        @Override
        public String getDisplayName() {
            return "Run Indexers";
        }

        public List<? extends IndexingTool> getIndexingTools() {
            return Arrays.asList(Hudson.getInstance().getDescriptorByType(IndexingTool.DescriptorImpl.class).getInstallations());
        }

        public boolean hasNonStandardIndexingTool() {
            return getIndexingTools().size() > 1;
        }

        public FormValidation doCheckIndexSubDirectory(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException, InterruptedException {
            FilePath workspace = project.getSomeWorkspace();
            if (workspace == null || !workspace.exists() || value == null || value.isEmpty() || workspace.child(value).isDirectory()) {
                return FormValidation.ok();
            } else {
                return workspace.validateRelativeDirectory(value);
            }
        }

        public FormValidation doCheckIgnorePatterns(@AncestorInPath AbstractProject project, @QueryParameter String ignorePatterns) throws IOException, InterruptedException {
            FilePath workspace = project.getSomeWorkspace();
            if (workspace == null || !workspace.exists() || ignorePatterns == null || ignorePatterns.isEmpty()) {
                return FormValidation.ok();
            } else {
                try {
                    Pattern.compile(ignorePatterns);
                    return FormValidation.ok();
                } catch (PatternSyntaxException ex) {
                    return FormValidation.error("Not a valid regular expression (" + ex.getDescription() + ")");
                }
            }
        }

    }

    private static class FindProjects implements FileCallable<RemoteResult> {
        private final String ignorePattern;
        private final String markers;
        private final String perProjectIgnore;
        public FindProjects(String markers, String ignorePattern, String perProjectIgnore) {
            this.markers = markers;
            this.ignorePattern = ignorePattern;
            this.perProjectIgnore = perProjectIgnore;
        }
        public RemoteResult invoke(File file, VirtualChannel vc) throws IOException, InterruptedException {
            Set<String> projects = new HashSet<String>();

            findProjects(file, projects, Pattern.compile(markers), Pattern.compile(ignorePattern), perProjectIgnore == null || perProjectIgnore.trim().isEmpty() ? null : Pattern.compile(perProjectIgnore), new StringBuilder());

            return new RemoteResult(projects, file.getCanonicalPath()/*XXX: will resolve symlinks!!!*/);
        }
    }

}
