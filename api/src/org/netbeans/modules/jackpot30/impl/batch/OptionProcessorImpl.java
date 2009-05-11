/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.batch;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
@ServiceProvider(service=OptionProcessor.class)
public class OptionProcessorImpl extends OptionProcessor {

    private static final Logger LOG = Logger.getLogger(OptionProcessorImpl.class.getName());
    
    private static final String APPLY_TRANSFORMATIONS_PROJECT_OPTION = "apply-transformations-project";

//    private static final Option LIST = Option.withoutArgument(Option.NO_SHORT_NAME, "list-hints-transformation");
    private static final Option APPLY_TRANSFORMATIONS = Option.shortDescription(
                                                            Option.requiredArgument(Option.NO_SHORT_NAME, "apply-transformations"),
                                                            "org.netbeans.modules.jackpot30.hints.batch.Bundle",
                                                            "SD_ApplyTransformations");
    private static final Option APPLY_TRANSFORMATIONS_PROJECT = Option.shortDescription(
                                                            Option.additionalArguments(Option.NO_SHORT_NAME, APPLY_TRANSFORMATIONS_PROJECT_OPTION),
                                                            "org.netbeans.modules.jackpot30.hints.batch.Bundle",
                                                            "SD_ApplyTransformationsProject");

    private static final Set<Option> OPTIONS = new HashSet<Option>(Arrays.asList(/*LIST, */APPLY_TRANSFORMATIONS, APPLY_TRANSFORMATIONS_PROJECT));
    
    @Override
    protected Set<Option> getOptions() {
        return OPTIONS;
    }

    @Override
    protected void process(Env env, Map<Option, String[]> optionValues) throws CommandException {
//        if (optionValues.containsKey(LIST)) {
//            env.getOutputStream().println("Supported Hints:");
//            for (TreeRule r : BatchApply.listHints()) {
//                env.getOutputStream().println(r.getDisplayName() + " - " + r.getId());
//            }
//        }

        List<Project> projects = new LinkedList<Project>();
        Map<String, List<ClassPath>> classPaths = new HashMap<String, List<ClassPath>>();
        
        if (optionValues.containsKey(APPLY_TRANSFORMATIONS_PROJECT)) {
            String[] projectNames = optionValues.get(APPLY_TRANSFORMATIONS_PROJECT);

            if (projectNames.length == 0) {
                env.getErrorStream().println("At least one parameter needed for " + APPLY_TRANSFORMATIONS_PROJECT_OPTION);
                throw new CommandException(1);
            }

            OUTER: for (String p : projectNames) {
                File loc = new File(env.getCurrentDirectory(), p);
                FileObject projectFile = FileUtil.toFileObject(loc);

                if (projectFile == null) {
                    env.getErrorStream().println("Ignoring file " + p + " - cannot be found.");
                    continue;
                }

                if (!projectFile.isFolder()) {
                    env.getErrorStream().println("Ignoring file " + p + " - not a folder.");
                    continue;
                }

                Project project = null;
                String  error   = null;

                try {
                    project = ProjectManager.getDefault().findProject(projectFile);
                } catch (IOException ex) {
                    error = ex.getLocalizedMessage();
                } catch (IllegalArgumentException ex) {
                    error = ex.getLocalizedMessage();
                }

                if (project == null) {
                    if (error == null) {
                        env.getErrorStream().println("Ignoring file " + p + " - not a project.");
                    } else {
                        env.getErrorStream().println("Ignoring file " + p + " - not a project (" + error + ").");
                    }

                    continue;
                }

                for (SourceGroup sg : ProjectUtils.getSources(project).getSourceGroups("java")) {
                    FileObject root = sg.getRootFolder();

                    for (String type : Arrays.asList(ClassPath.BOOT, ClassPath.COMPILE, ClassPath.SOURCE)) {
                        if (!handleClassPath(root, type, env, p, classPaths)) {
                            continue OUTER;
                        }
                    }
                }

                projects.add(project);
            }

            for (Entry<String, List<ClassPath>> cps : classPaths.entrySet()) {
                LOG.log(Level.INFO, "registering classpaths: type={0}, classpaths={1}", new Object[] {cps.getKey(), cps.getValue()});
                GlobalPathRegistry.getDefault().register(cps.getKey(), cps.getValue().toArray(new ClassPath[0]));
            }
        } else {
            projects.addAll(Arrays.asList(OpenProjects.getDefault().getOpenProjects()));
        }
        
        try {
            waitScanFinished();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        if (optionValues.containsKey(APPLY_TRANSFORMATIONS)) {
            String hintsArg = optionValues.get(APPLY_TRANSFORMATIONS)[0];
//            String[] hints = hintsArg.split(":");

            Lookup context = Lookups.fixed((Object[]) projects.toArray(new Project[0]));
            String error = BatchApply.applyFixes(context, hintsArg, false);

            if (error != null) {
                env.getErrorStream().println("Cannot apply hints because of: " + error);
            }
        }

        for (Entry<String, List<ClassPath>> cps : classPaths.entrySet()) {
            GlobalPathRegistry.getDefault().unregister(cps.toString(), cps.getValue().toArray(new ClassPath[0]));
        }
    }

    private boolean handleClassPath(FileObject root, String type, Env env, String p, Map<String, List<ClassPath>> classPaths) {
        ClassPath cp = ClassPath.getClassPath(root, type);

        if (cp == null) {
            env.getErrorStream().println("Ignoring file " + p + " - no " + type + " classpath for source group: " + FileUtil.getFileDisplayName(root));
            return false;
        }

        List<ClassPath> cps = classPaths.get(type);

        if (cps == null) {
            classPaths.put(type, cps = new LinkedList<ClassPath>());
        }

        cp = ClassPathSupport.createProxyClassPath(cp);
        
        cps.add(cp);
        
        return true;
    }

    @SuppressWarnings("deprecation")
    private void waitScanFinished() throws InterruptedException {
        SourceUtils.waitScanFinished();
    }

}
