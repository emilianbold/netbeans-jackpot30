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
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.EditType;
import hudson.util.ArgumentListBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.netbeans.api.jackpot.hudson.IndexBuilder;

/**
 *
 * @author lahvac
 */
public class JackpotIndexBuilder extends IndexBuilder {

    private JackpotIndexBuilder(StaplerRequest req, JSONObject formData) {
    }

    @DataBoundConstructor
    public JackpotIndexBuilder() {}

    @Override
    public boolean index(File cacheDir, AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Jackpot 3.0 indexing:");

        Set<String> addedFiles = new HashSet<String>();
        Set<String> removedFiles = new HashSet<String>();

        for (Entry e : build.getChangeSet()) {
            for (AffectedFile f : e.getAffectedFiles()) {
                if (f.getEditType() == EditType.DELETE) {
                    removedFiles.add(f.getPath());
                } else {
                    addedFiles.add(f.getPath());
                }
            }
        }

        File a = File.createTempFile("jck30", "");
        File r = File.createTempFile("jck30", "");

        dumpToFile(a, addedFiles);
        dumpToFile(r, removedFiles);

        ArgumentListBuilder args = new ArgumentListBuilder();

        args.add("java"); //XXX
        args.add("-Xbootclasspath/p:" + indexerLocation.getAbsolutePath() + "/lib/javac-api-nb-7.0-b07.jar");
        args.add("-jar");
        args.add(new File(indexerLocation, "indexer.jar").getAbsolutePath());
        args.add("-store-sources");
        args.add(".");
        args.add(cacheDir);
        args.add(a.getAbsolutePath());
        args.add(r.getAbsolutePath());

        Proc indexer = launcher.launch().pwd(build.getWorkspace())
                                        .cmds(args)
                                        .stdout(listener)
                                        .start();

        indexer.join();

        a.delete();
        r.delete();

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

    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends IndexBuilderDescriptor {

        @Override
        public JackpotIndexBuilder newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new JackpotIndexBuilder(req, formData);
        }

        @Override
        public String getDisplayName() {
            return "Jackpot 3.0 Indexing";
        }

    }


    private static final File indexerLocation;

    private static final String[] locationsToTest = new String[] {
        "../../data/compiler/indexer",
        "../../compiler/indexer",
    };

    private static File computeIndexerLocation() {
        URL loc = IndexingBuilder.class.getProtectionDomain().getCodeSource().getLocation();
        File base;
        if ("jar".equals(loc.getProtocol())) { //NOI18N
            String path = loc.getPath();
            int index = path.indexOf("!/"); //NOI18N

            if (index >= 0) {
                try {
                    String jarPath = path.substring(0, index);
                    if (jarPath.indexOf("file://") > -1 && jarPath.indexOf("file:////") == -1) {  //NOI18N
                        /* Replace because JDK application classloader wrongly recognizes UNC paths. */
                        jarPath = jarPath.replaceFirst("file://", "file:////");  //NOI18N
                    }
                    loc = new URL(jarPath);
                } catch (MalformedURLException mue) {
                    throw new IllegalStateException(mue);
                }
            }
        }
        if ("file".equals(loc.getProtocol())) {
            base = new File(loc.getPath());
        } else {
            throw new IllegalStateException(loc.toExternalForm());
        }

        File locFile = null;

        for (String t : locationsToTest) {
            if (new File(new File(base, t), "indexer.jar").canRead()) {
                try {
                    locFile = new File(base, t).getCanonicalFile();
                } catch (IOException ex) {
                    //XXX: log
                    locFile = new File(base, t).getAbsoluteFile();
                }
                break;
            }
        }

        if (locFile == null) {
            throw new IllegalStateException(base.getAbsolutePath());
        }

        return locFile;
    }

    static {
        indexerLocation = computeIndexerLocation();
    }
}
