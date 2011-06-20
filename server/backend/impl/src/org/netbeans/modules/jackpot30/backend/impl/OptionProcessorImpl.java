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
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesCustomIndexerImpl;
import org.netbeans.modules.jackpot30.impl.indexing.CustomIndexerImpl;
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

    private final Option CATEGORY_ID = Option.requiredArgument(Option.NO_SHORT_NAME, "category-id");
    private final Option CATEGORY_NAME = Option.requiredArgument(Option.NO_SHORT_NAME, "category-name");
    private final Option CATEGORY_PROJECTS = Option.additionalArguments(Option.NO_SHORT_NAME, "category-projects");
    private final Option CATEGORY_ROOT_DIR = Option.requiredArgument(Option.NO_SHORT_NAME, "category-root-dir");
    private final Option CACHE_TARGET = Option.requiredArgument(Option.NO_SHORT_NAME, "cache-target");
    private final Set<Option> OPTIONS = new HashSet<Option>(Arrays.asList(CATEGORY_ID, CATEGORY_NAME, CATEGORY_PROJECTS, CATEGORY_ROOT_DIR, CACHE_TARGET));
    
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

            try {
                CategoryStorage.setCategoryContent(categoryId, categoryName, getRoots(optionValues.get(CATEGORY_PROJECTS), env));
            } catch (InterruptedException ex) {
                throw (CommandException) new CommandException(0).initCause(ex);
            }
        }

        String cacheTarget = optionValues.get(CACHE_TARGET)[0];
        File cache = FileUtil.normalizeFile(new File(cacheTarget));

        cache.getParentFile().mkdirs();

        if (categoryId == null) {
            env.getErrorStream().println("Error: no category-id specified!");
            return;
        }

        try {
            indexProjects(CategoryStorage.getCategoryContent(categoryId), env);
        } catch (InterruptedException ex) {
            throw (CommandException) new CommandException(0).initCause(ex);
        } catch (IOException ex) {
            throw (CommandException) new CommandException(0).initCause(ex);
        }

        JarOutputStream out = null;
        InputStream segments = null;

        try {
            FileObject cacheFolder = CacheFolder.getCacheFolder();

            out = new JarOutputStream(new FileOutputStream(cache));
            pack(out, cacheFolder, categoryId, new StringBuilder());

            segments = cacheFolder.getFileObject("segments").getInputStream();
            Properties in = new Properties();

            in.load(segments);

            segments.close();//XXX: should be in finally!

            File baseDirFile = new File(optionValues.get(CATEGORY_ROOT_DIR)[0]);
            String baseDir = baseDirFile.toURI().toString();

            Properties outSegments = new Properties();

            for (String segment : in.stringPropertyNames()) {
                String url = in.getProperty(segment);
                String rel = url.startsWith(baseDir) ? "rel:/" + url.substring(baseDir.length()) : url;

                outSegments.setProperty(segment, rel);
            }

            out.putNextEntry(new ZipEntry(categoryId + "/segments"));

            outSegments.store(out, "");
        } catch (IOException ex) {
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

    private Set<FileObject> getRoots(String[] projects, Env env) throws IllegalArgumentException, InterruptedException {
        Set<FileObject> sourceRoots = new HashSet<FileObject>(projects.length * 4 / 3 + 1);

        for (String p : projects) {
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

            try {
                Project prj = ProjectManager.getDefault().findProject(prjFO);

                if (prj == null) {
                    env.getErrorStream().println("Project specified as: " + p + " does not resolve to a project (" + FileUtil.getFileDisplayName(prjFO));
                    continue;
                }

                SourceGroup[] javaSG = ProjectUtils.getSources(prj).getSourceGroups("java");

                if (javaSG.length == 0) {
                    env.getErrorStream().println("Project specified as: " + p + " does not define a java source groups (" + FileUtil.getFileDisplayName(prjFO));
                    continue;
                }

                for (SourceGroup sg : javaSG) {
                    sourceRoots.add(sg.getRootFolder());
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
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
            System.setProperty(CustomIndexerImpl.class.getName() + "-attributed", "true");//force partially attributed Jackpot index
            org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
            ClassPath source = ClassPathSupport.createClassPath(sourceRoots.toArray(new FileObject[0]));

            GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {source});
            SourceUtils.waitScanFinished();

            for (FileObject root : sourceRoots) {
                new DuplicatesCustomIndexerImpl.FactoryImpl().updateIndex(root.getURL(), new AtomicBoolean());
            }
            GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, new ClassPath[] {source});
        }
    }

    private void pack(JarOutputStream target, FileObject index, String name, StringBuilder relPath) throws IOException {
        int len = relPath.length();
        boolean first = relPath.length() == 0;

        if (!first) relPath.append("/");
        relPath.append(name);

        for (FileObject c : index.getChildren()) {
            if (first && c.getNameExt().equals("segments")) continue;
            pack(target, c, c.getNameExt(), relPath);
        }

        if (index.isData()) {
            target.putNextEntry(new ZipEntry(relPath.toString()));

            InputStream in = index.getInputStream();

            try {
                FileUtil.copy(in, target);
            } finally {
                in.close();
            }
        }

        relPath.delete(len, relPath.length());
    }
}
