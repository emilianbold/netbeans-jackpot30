/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.hudson;

import hudson.Launcher;
import hudson.Proc;
import hudson.model.Hudson;
import hudson.util.ArgumentListBuilder;
import hudson.util.LogTaskListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lahvac
 */
public class StartWebFrontEnd {

    public static boolean disable = false;

    private static Proc frontend;

    public static void ensureStarted() {
        if (disable) return ;
        
        try {
            if (frontend != null && frontend.isAlive()) return;

            IndexingTool[] tools = Hudson.getInstance().getDescriptorByType(IndexingTool.DescriptorImpl.class).getInstallations();

            if (tools.length == 0) return;

            File cacheDir = Hudson.getInstance().getDescriptorByType(IndexingBuilder.DescriptorImpl.class).getCacheDir();

            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            
            LogTaskListener listener = new LogTaskListener(Logger.global, Level.INFO);
            IndexingTool tool = tools[0].forNode(Hudson.getInstance(), listener);

            ArgumentListBuilder args = new ArgumentListBuilder();
            Launcher launcher = new Launcher.LocalLauncher(listener);
            args.add(new File(tool.getHome(), "web.sh")); //XXX
            args.add(cacheDir);

            frontend = launcher.launch().cmds(args)
                                        .stdout(listener)
                                        .start();
        } catch (IOException ex) {
            Logger.getLogger(StartWebFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(StartWebFrontEnd.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
