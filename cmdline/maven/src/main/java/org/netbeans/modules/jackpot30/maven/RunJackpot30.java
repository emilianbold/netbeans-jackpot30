/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.netbeans.modules.jackpot30.cmdline.Main;

/**
 * @goal analyze
 * @author Jan Lahoda
 */
public class RunJackpot30 extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            String sourceLevel = "1.5";
            for (Object o : project.getBuild().getPlugins()) {
                if (!(o instanceof Plugin)) continue;
                Plugin p = (Plugin) o;
                if (!"org.apache.maven.plugins".equals(p.getGroupId())) continue;
                if (!"maven-compiler-plugin".equals(p.getArtifactId())) continue;
                if (p.getConfiguration() instanceof Xpp3Dom) {
                    Xpp3Dom configuration = (Xpp3Dom) p.getConfiguration();
                    Xpp3Dom source = configuration.getChild("source");

                    if (source != null) {
                        sourceLevel = source.getValue();
                    }
                }
            }
            List<String> cmdLine = new ArrayList<String>();
            cmdLine.add("-no-apply");
            cmdLine.add("-sourcepath");
            cmdLine.add(toClassPathString((List<String>) project.getCompileSourceRoots()));
            cmdLine.add("-classpath");
            cmdLine.add(toClassPathString((List<String>) project.getCompileClasspathElements()));
            cmdLine.add("-source");
            cmdLine.add(sourceLevel);

            for (String sr : (List<String>) project.getCompileSourceRoots()) {
                cmdLine.add(sr);
            }

            Main.compile(cmdLine.toArray(new String[0]));
        } catch (IOException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        } catch (DependencyResolutionRequiredException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private static String toClassPathString(List<String> entries) {
        StringBuilder classPath = new StringBuilder();

        for (String root : entries) {
            if (classPath.length() > 0) classPath.append(File.pathSeparatorChar);
            classPath.append(root);
        }

        return classPath.toString();
    }

}
