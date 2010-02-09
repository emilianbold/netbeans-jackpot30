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
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run.RunnerAbortedException;
import hudson.remoting.Channel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 *
 * @author lahvac
 */
public final class BuildWrapperImpl extends BuildWrapper {

    private final Map<String, Boolean> id2Enabled;
    
    public BuildWrapperImpl(StaplerRequest req, JSONObject json) {
        id2Enabled = new HashMap<String, Boolean>();

        for (String id : getDescriptor().getHints()) {
            id2Enabled.put(id, json.getBoolean(id));
        }
    }

    @DataBoundConstructor
    public BuildWrapperImpl(Map<String, Boolean> id2Enabled) {
        this.id2Enabled = id2Enabled;
    }

    public Map<String, Boolean> getId2Enabled() {
        return id2Enabled;
    }

    public boolean isHintEnabled(String id) {
        return id2Enabled.get(id) == Boolean.TRUE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new EnvironmentImpl();
    }

    @Override
    public Launcher decorateLauncher(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException, RunnerAbortedException {
        return new LauncherWrapper(launcher);
    }


    private final class EnvironmentImpl extends Environment {

        @Override
        public void buildEnvVars(Map<String, String> env) {
            super.buildEnvVars(env);
        }

    }

    private final class LauncherWrapper extends Launcher {

        private final Launcher delegate;

        public LauncherWrapper(Launcher delegate) {
            super(delegate.getListener(), delegate.getChannel());//XXX???
            this.delegate = delegate;
        }

        @Override
        public Proc launch(ProcStarter ps) throws IOException {
            if (!ps.cmds().get(0).contains("ant")) {
                return delegate.launch(ps);
            }
            List<String> args = new LinkedList<String>(ps.cmds());
            args.add("-lib");
            args.add(antLibLocation.getAbsolutePath());

            StringBuilder enabled = new StringBuilder();
            boolean first = true;
            
            for (Entry<String, Boolean> e : id2Enabled.entrySet()) {
                if (e.getValue() != Boolean.TRUE) continue;
                if (!first) enabled.append(":");
                enabled.append(e.getKey() /*XXX:*/.replace('_', '.'));
                first = false;
            }

            args.add("-Djackpot30-enabled-hints=" + enabled.toString());
            args.add("-Dbuild.compiler=org.netbeans.modules.jackpot30.compiler.ant.JackpotCompiler");

            boolean[] origMasks = ps.masks();
            boolean[] mask = new boolean[args.size()];

            System.arraycopy(origMasks, 0, mask, 0, origMasks.length);
            
            ProcStarter nue = ps.cmds(args).masks(mask);

            return delegate.launch(nue);
        }

        @Override
        public Channel launchChannel(String[] strings, OutputStream out, FilePath fp, Map<String, String> map) throws IOException, InterruptedException {
            return delegate.launchChannel(strings, out, fp, map);
        }

        @Override
        public void kill(Map<String, String> map) throws IOException, InterruptedException {
            delegate.kill(map);
        }
        
    }

    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new BuildWrapperImpl(req, formData);
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> ap) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Enable Jackpot";
        }

        public String getHintDisplayName(String id) {
            return hints.get(id).displayName;
        }

        public String getCategoryDisplayName(String cat) {
            return Character.toUpperCase(cat.charAt(0)) + cat.substring(1).replace('_', ' ');
        }

        public String getHintDescription(String id) {
            return hints.get(id).description;
        }

        @Override
        public void doHelp(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            System.err.println("doHelp");
            super.doHelp(req, rsp);
        }

        public List<String> getHints() {
            List<String> res = new ArrayList<String>(hints.keySet());

            Collections.sort(res, new Comparator<String>() {
                public int compare(String k1, String k2) {
                    return hints.get(k1).displayName.compareToIgnoreCase(hints.get(k2).displayName);
                }
            });

            return res;
        }

        public List<String> getHintsInCategory(String cat) {
            List<String> res = new ArrayList<String>();

            for (HintDescription d : hints.values()) {
                if (!cat.equals(d.category)) {
                    continue;
                }

                res.add(d.id);
            }

            Collections.sort(res, new Comparator<String>() {
                public int compare(String k1, String k2) {
                    return hints.get(k1).displayName.compareToIgnoreCase(hints.get(k2).displayName);
                }
            });

            return res;
        }
        
        public List<String> getCategories() {
            Set<String> cats = new HashSet<String>();

            for (HintDescription d : hints.values()) {
                cats.add(d.category);
            }

            List<String> res = new ArrayList<String>(cats);

            Collections.sort(res, new Comparator<String>() {
                public int compare(String k1, String k2) {
                    return k1.compareToIgnoreCase(k2);
                }
            });

            return res;
        }

    }

    private static int count;
    
    public static final class HintDescription {
        public final String id;
        public final String category;
        public final String displayName;
        public final String description;

        private HintDescription(Map<String, String> map) {
            this.id = read(map, "id", "no-id-" + count++).replace('.', '_');
            this.category = read(map, "category", "general");
            this.displayName = read(map, "displayName", "No Display Name");
            this.description = read(map, "description", "No Description");
        }

    }

    private static String read(Map<String, String> data, String key, String def) {
        if (data.containsKey(key)) {
            return data.get(key);
        }

        return def;
    }

    private static final File antLibLocation;
    private static final Map<String, HintDescription> hints = new HashMap<String, HintDescription>();

    private static final String[] locationsToTest = new String[] {
        "../../data/compiler",
        "../../compiler",
    };

    static {
        URL loc = BuildWrapperImpl.class.getProtectionDomain().getCodeSource().getLocation();
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
            if (new File(new File(base, t), "compiler.jar").canRead()) {
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

        antLibLocation = locFile;

        File compilerJar = new File(antLibLocation, "compiler.jar");
        
        try {
            Process exec = Runtime.getRuntime().exec(new String[]{"java", "-classpath", compilerJar.getAbsolutePath(), "org.netbeans.modules.jackpot30.compiler.DumpHints"});
            final Reader r = new BufferedReader(new InputStreamReader(exec.getInputStream()));
            final StringBuffer data = new StringBuffer();
            final CountDownLatch l = new CountDownLatch(1);

            new Thread() {
                public void run() {
                    try {
                        int c;

                        while ((c = r.read()) != (-1)) {
                            data.append((char) c);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(BuildWrapperImpl.class.getName()).log(Level.SEVERE, null, ex);
                    } finally {
                        try {
                            r.close();
                        } catch (IOException ex) {
                            Logger.getLogger(BuildWrapperImpl.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        l.countDown();
                    }
                }
            }.start();

            exec.waitFor();
            l.await();

            JSONArray arr = (JSONArray) JSONSerializer.toJSON(data.toString());

            for (int i = 0; i < arr.size(); i++) {
                JSONObject o = arr.getJSONObject(i);

                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) o.toBean(Map.class);

                HintDescription hd = new HintDescription(map);

                hints.put(hd.id, hd);
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(BuildWrapperImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BuildWrapperImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(BuildWrapperImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
