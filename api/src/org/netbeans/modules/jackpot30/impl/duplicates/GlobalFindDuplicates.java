/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.impl.duplicates;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.jackpot30.impl.duplicates.ComputeDuplicates.DuplicateDescription;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

public final class GlobalFindDuplicates implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        final Collection<DuplicateDescription> dupes = Collections.synchronizedList(new LinkedList<DuplicateDescription>());
        final ProgressHandle handle = ProgressHandleFactory.createHandle("Compute Duplicates");
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Computing Duplicates - Please Wait"), BorderLayout.NORTH);
        panel.add(ProgressHandleFactory.createProgressComponent(handle), BorderLayout.CENTER);
        DialogDescriptor w = new DialogDescriptor(panel, "Computing Duplicates");
        final Dialog d = DialogDisplayer.getDefault().createDialog(w);

        WORKER.post(new Runnable() {
            public void run() {
                handle.start();

                try {
                    dupes.addAll(new ComputeDuplicates().computeDuplicatesForAllOpenedProjects());
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    handle.finish();

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            d.setVisible(false);
                        }
                    });
                }
            }
        });

        d.setVisible(true);

        NotifyDescriptor nd = new NotifyDescriptor.Message(new DuplicatesListPanel(dupes));

        DialogDisplayer.getDefault().notifyLater(nd);
    }

    private static final RequestProcessor WORKER = new RequestProcessor(GlobalFindDuplicates.class.getName(), 1);
    
}
