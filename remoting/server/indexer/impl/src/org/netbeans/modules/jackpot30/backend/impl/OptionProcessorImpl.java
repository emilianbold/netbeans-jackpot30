/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.backend.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.modules.jackpot30.backend.impl.spi.IndexAccessor;
import org.netbeans.modules.jackpot30.backend.impl.spi.StatisticsGenerator;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.support.ant.PropertyUtils;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.LifecycleManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=OptionProcessor.class)
public class OptionProcessorImpl extends OptionProcessor {

    private static final Logger LOG = Logger.getLogger(OptionProcessorImpl.class.getName());
    private final Option CATEGORY_ID = Option.requiredArgument(Option.NO_SHORT_NAME, "category-id");
    private final Option CATEGORY_NAME = Option.requiredArgument(Option.NO_SHORT_NAME, "category-name");
    private final Option CATEGORY_PROJECTS = Option.additionalArguments(Option.NO_SHORT_NAME, "category-projects");
    private final Option CATEGORY_ROOT_DIR = Option.requiredArgument(Option.NO_SHORT_NAME, "category-root-dir");
    private final Option CACHE_TARGET = Option.requiredArgument(Option.NO_SHORT_NAME, "cache-target");
    private final Option INFO = Option.requiredArgument(Option.NO_SHORT_NAME, "info");
    private final Set<Option> OPTIONS = new HashSet<Option>(Arrays.asList(CATEGORY_ID, CATEGORY_NAME, CATEGORY_PROJECTS, CATEGORY_ROOT_DIR, CACHE_TARGET, INFO));
    
    @Override
    protected Set<Option> getOptions() {
        return OPTIONS;
    }

    @Override
    protected void process(Env env, Map<Option, String[]> optionValues) throws CommandException {
        String categoryId = null;
        String categoryName = null;

        if (optionValues.containsKey(CATEGORY_ID)) {
            categoryId = optionValues.get(CATEGORY_ID)[0];
        }

        if (optionValues.containsKey(CATEGORY_NAME)) {
            categoryName = optionValues.get(CATEGORY_NAME)[0];
        }

        if (optionValues.containsKey(CATEGORY_PROJECTS)) {
            if (categoryId == null) {
                env.getErrorStream().println("Error: no category-id specified!");
                return;
            }

            if (categoryName == null) {
                env.getErrorStream().println("Warning: no category-name specified.");
                return;
            }
        }

        String cacheTarget = optionValues.get(CACHE_TARGET)[0];
        File cache = FileUtil.normalizeFile(new File(cacheTarget));

        cache.getParentFile().mkdirs();

        if (categoryId == null) {
            env.getErrorStream().println("Error: no category-id specified!");
            return;
        }

        File baseDirFile = new File(optionValues.get(CATEGORY_ROOT_DIR)[0]);
        FileObject baseDir = FileUtil.toFileObject(baseDirFile);
        IndexWriter w = null;

        FileObject cacheFolder = CacheFolder.getCacheFolder();
        FileObject cacheTemp = cacheFolder.getFileObject("index");

        try {
            if (cacheTemp != null) cacheTemp.delete();

            cacheTemp = cacheFolder.createFolder("index");
            w = new IndexWriter(FSDirectory.open(FileUtil.toFile(cacheTemp)), new KeywordAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);

            IndexAccessor.current = new IndexAccessor(w, baseDir);
            Set<FileObject> roots = getRoots(optionValues.get(CATEGORY_PROJECTS), env);

            indexProjects(roots, env);
        } catch (InterruptedException ex) {
            LOG.log(Level.FINE, null, ex);
            throw (CommandException) new CommandException(0).initCause(ex);
        } catch (IOException ex) {
            LOG.log(Level.FINE, null, ex);
            throw (CommandException) new CommandException(0).initCause(ex);
        } finally {
            if (w != null) {
                try {
                    w.optimize(true);
                    w.close(true);
                } catch (CorruptIndexException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        Map<String, Long> statistics = Collections.emptyMap();
        IndexReader r = null;

        try {
            r = IndexReader.open(FSDirectory.open(FileUtil.toFile(cacheTemp)), true);

            statistics = StatisticsGenerator.generateStatistics(r);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        JarOutputStream out = null;
        InputStream segments = null;

        try {
            out = new JarOutputStream(new FileOutputStream(cache));
            pack(out, cacheTemp, "index", new StringBuilder(categoryId));

            segments = cacheFolder.getFileObject("segments").getInputStream();
            Properties in = new Properties();

            in.load(segments);

            segments.close();//XXX: should be in finally!

            String baseDirPath = baseDirFile.toURI().toString();

            Properties outSegments = new Properties();

            for (String segment : in.stringPropertyNames()) {
                String url = in.getProperty(segment);
                String rel;
                
                if (url.startsWith(baseDirPath)) rel = "rel:/" + url.substring(baseDirPath.length());
                else if (url.startsWith("jar:" + baseDirPath)) rel = "jar:rel:/" + url.substring(4 + baseDirPath.length());
                else rel = url;

                outSegments.setProperty(segment, rel);
            }

            out.putNextEntry(new ZipEntry(categoryId + "/segments"));

            outSegments.store(out, "");

            out.putNextEntry(new ZipEntry(categoryId + "/info"));

            out.write("{\n".getBytes("UTF-8"));
            out.write(("\"displayName\": \"" + categoryName + "\"").getBytes("UTF-8"));
            if (optionValues.containsKey(INFO)) {
                for (String infoValue : optionValues.get(INFO)[0].split(";")) {
                    int eqSign = infoValue.indexOf('=');
                    if (eqSign == (-1)) {
                        LOG.log(Level.INFO, "No ''='' sign in: {0}", infoValue);
                        continue;
                    }
                    out.write((",\n\"" + infoValue.substring(0, eqSign) + "\": \"" + infoValue.substring(eqSign + 1) + "\"").getBytes("UTF-8"));
                }
            }
            out.write(",\n \"statistics\" : {\n".getBytes("UTF-8"));
            boolean wasEntry = false;
            for (Entry<String, Long> e : statistics.entrySet()) {
                if (wasEntry) out.write(", \n".getBytes("UTF-8"));
                out.write(("\"" + e.getKey() + "\" : " + e.getValue()).getBytes("UTF-8"));
                wasEntry = true;
            }
            out.write("\n}\n".getBytes("UTF-8"));
            out.write("\n}\n".getBytes("UTF-8"));

            for (FileObject s : cacheFolder.getChildren()) {
                if (!s.isFolder() || !s.getNameExt().startsWith("s") || s.getChildren().length == 0) continue;

                JarOutputStream local = null;
                try {
                    out.putNextEntry(new ZipEntry(categoryId + "/" + s.getNameExt()));

                    local = new JarOutputStream(out);

                    pack(local, s, "", new StringBuilder(""));
                } finally {
                    if (local != null) {
                        local.finish();
                    }
                }
            }
        } catch (IOException ex) {
            LOG.log(Level.FINE, null, ex);
            throw (CommandException) new CommandException(0).initCause(ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    throw (CommandException) new CommandException(0).initCause(ex);
                }
            }

            if (segments != null) {
                try {
                    segments.close();
                } catch (IOException ex) {
                    throw (CommandException) new CommandException(0).initCause(ex);
                }
            }
        }
        
        LifecycleManager.getDefault().exit();
    }

    private Set<FileObject> getRoots(String[] projects, Env env) {
        Set<FileObject> sourceRoots = new HashSet<FileObject>(projects.length * 4 / 3 + 1);

        for (String p : projects) {
            try {
                LOG.log(Level.FINE, "Processing project specified as: {0}", p);
                File f = PropertyUtils.resolveFile(env.getCurrentDirectory(), p);
                File normalized = FileUtil.normalizeFile(f);
                FileObject prjFO = FileUtil.toFileObject(normalized);

                if (prjFO == null) {
                    env.getErrorStream().println("Project location cannot be found: " + p);
                    continue;
                }

                if (!prjFO.isFolder()) {
                    env.getErrorStream().println("Project specified as: " + p + " does not point to a directory (" + FileUtil.getFileDisplayName(prjFO));
                    continue;
                }

                Project prj = ProjectManager.getDefault().findProject(prjFO);

                if (prj == null) {
                    env.getErrorStream().println("Project specified as: " + p + " does not resolve to a project (" + FileUtil.getFileDisplayName(prjFO));
                    continue;
                }

                LOG.log(Level.FINE, "project resolved: {0} ({1})", new Object[] {ProjectUtils.getInformation(prj), prj.getClass()});
                SourceGroup[] javaSG = ProjectUtils.getSources(prj).getSourceGroups("java");

                if (javaSG.length == 0) {
                    env.getErrorStream().println("Project specified as: " + p + " does not define a java source groups (" + FileUtil.getFileDisplayName(prjFO));
                    continue;
                }

                for (SourceGroup sg : javaSG) {
                    LOG.log(Level.FINE, "Found source group: {0}", FileUtil.getFileDisplayName(sg.getRootFolder()));
                    sourceRoots.add(sg.getRootFolder());
                }
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable ex) {
                LOG.log(Level.FINE, null, ex);
                Exceptions.printStackTrace(ex);
                env.getErrorStream().println("Cannot work with project specified as: " + p + " (" + ex.getLocalizedMessage() + ")");
            }
        }

        return sourceRoots;
    }

    private void indexProjects(Set<FileObject> sourceRoots, Env env) throws IOException, InterruptedException {
        if (sourceRoots.isEmpty()) {
            env.getErrorStream().println("Error: There is nothing to index!");
        } else {
            //XXX: to start up the project systems and RepositoryUpdater:
            ((Runnable) OpenProjects.getDefault().openProjects()).run();
            org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
            ClassPath source = ClassPathSupport.createClassPath(sourceRoots.toArray(new FileObject[0]));

            LOG.log(Level.FINE, "Registering as source path: {0}", source.toString());
            GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {source});
            SourceUtils.waitScanFinished();
            GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, new ClassPath[] {source});
        }
    }

    private void pack(JarOutputStream target, FileObject index, String name, StringBuilder relPath) throws IOException {
        int len = relPath.length();
        boolean first = relPath.length() == 0;

        if (!first) relPath.append("/");
        relPath.append(name);

        boolean data = index.isData();

        if (relPath.length() > 0) {
            target.putNextEntry(new ZipEntry(relPath.toString() + (data ? "" : "/")));
        }

        if (data) {
            InputStream in = index.getInputStream();

            try {
                FileUtil.copy(in, target);
            } finally {
                in.close();
            }
        }

        for (FileObject c : index.getChildren()) {
            if (first && c.getNameExt().equals("segments")) continue;
            pack(target, c, c.getNameExt(), relPath);
        }

        relPath.delete(len, relPath.length());
    }

}
