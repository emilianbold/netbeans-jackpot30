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

package org.netbeans.modules.hudson.indexerinstaller;

import hudson.cli.CLI;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.modules.hudson.api.HudsonInstance;

import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.WindowManager;

@ActionID(
    category="Tools",
    id="org.netbeans.modules.hudson.indexerinstaller.InstallHudsonExtension"
)
@ActionRegistration(
    displayName="#CTL_InstallHudsonExtension"
)
@ActionReferences({
})
@Messages({"CTL_InstallHudsonExtension=Install Hudson Extension",
        "CTL_InstallingPlugin=Installing Plugin", 
        "CTL_RestartTitle=Plugin Installed", 
        "CTL_Restart=Restart Hudson?",
        "CTL_Failed=Installation Failed"})
public final class InstallHudsonExtension implements ActionListener {
    private final HudsonInstance context;

    public InstallHudsonExtension(HudsonInstance context) {
        this.context = context;
    }

    public void actionPerformed(ActionEvent ev) {
        try {
            final Installer installer = new Installer(context.getUrl());
            URL url = CLI.class.getProtectionDomain().getCodeSource().getLocation();
            String name = url.getPath();
            final String plugin = name.substring(name.indexOf("/"), name.indexOf("ext/hudson-cli.jar")) + "plugin/org.netbeans.modules.jackpot30.hudson.hpi";
            ProgressUtils.showProgressDialogAndRun(new Runnable() {
                @Override
                public void run() {
                    final int exit = installer.installPlugin(plugin);
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            if (exit == -1) {
                                int n = JOptionPane.showConfirmDialog(WindowManager.getDefault().getMainWindow(), Bundle.CTL_Restart(),
                                        Bundle.CTL_RestartTitle(),
                                        JOptionPane.YES_NO_OPTION);
                                if (n==JOptionPane.YES_OPTION) {
                                    installer.restartHudson();
                                }
                            } else {
                                JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), Bundle.CTL_Failed());
                                
                            }
                        }
                        
                    });
                    
                }},Bundle.CTL_InstallingPlugin()
            );
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
        
    }
}
