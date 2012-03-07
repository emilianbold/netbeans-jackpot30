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
package org.netbeans.modules.jackpot30.refactoring;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 *
 * @author lahvac
 */
public class GenerateScriptPanel extends javax.swing.JPanel {

    private final boolean supported;

    public GenerateScriptPanel(boolean supported) {
        this.supported = supported;
        initComponents();
        loadDefaults();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        generate = new javax.swing.JCheckBox();
        makePrivate = new javax.swing.JCheckBox();
        deprecate = new javax.swing.JCheckBox();

        generate.setText(org.openide.util.NbBundle.getMessage(GenerateScriptPanel.class, "GenerateScriptPanel.generate.text")); // NOI18N
        generate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateActionPerformed(evt);
            }
        });

        makePrivate.setText(org.openide.util.NbBundle.getMessage(GenerateScriptPanel.class, "GenerateScriptPanel.makePrivate.text")); // NOI18N

        deprecate.setText(org.openide.util.NbBundle.getMessage(GenerateScriptPanel.class, "GenerateScriptPanel.deprecate.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(deprecate)
                            .addComponent(makePrivate)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(generate)))
                .addContainerGap(79, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(generate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(makePrivate)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deprecate)
                .addContainerGap(223, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void generateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_generateActionPerformed
        updateState();
    }//GEN-LAST:event_generateActionPerformed

    private static final String KEY_DO_GENERATE_SCRIPT = "generate-script";
    private static final boolean DEF_DO_GENERATE_SCRIPT = true;
    private static final String KEY_MAKE_PRIVATE = "make-private";
    private static final boolean DEF_MAKE_PRIVATE = true;
    private static final String KEY_DEPRECATE = "deprecate";
    private static final boolean DEF_DEPRECATE = false;
    
    private Preferences getPreferencesStorage() {
        return NbPreferences.forModule(GenerateScriptPanel.class).node(GenerateScriptPanel.class.getSimpleName());
    }

    private void loadDefaults() {
        if (supported) {
            Preferences p = getPreferencesStorage();
            generate.setSelected(p.getBoolean(KEY_DO_GENERATE_SCRIPT, DEF_DO_GENERATE_SCRIPT));
            makePrivate.setSelected(p.getBoolean(KEY_MAKE_PRIVATE, DEF_MAKE_PRIVATE));
            deprecate.setSelected(p.getBoolean(KEY_DEPRECATE, DEF_DEPRECATE));
        } else {
            generate.setSelected(false);
            makePrivate.setSelected(false);
            deprecate.setSelected(false);
        }
        
        updateState();
    }

    public void saveDefaults() {
        Preferences p = getPreferencesStorage();
        boolean doGenerate = generate.isSelected();

        p.putBoolean(KEY_DO_GENERATE_SCRIPT, doGenerate);

        if (doGenerate) {
            p.putBoolean(KEY_MAKE_PRIVATE, makePrivate.isSelected());
            p.putBoolean(KEY_DEPRECATE, deprecate.isSelected());
        }
    }

    private void updateState() {
        if (supported) {
            generate.setEnabled(true);
            makePrivate.setEnabled(generate.isSelected());
            deprecate.setEnabled(generate.isSelected());
        } else {
            generate.setEnabled(false);
            makePrivate.setEnabled(false);
            deprecate.setEnabled(false);
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox deprecate;
    private javax.swing.JCheckBox generate;
    private javax.swing.JCheckBox makePrivate;
    // End of variables declaration//GEN-END:variables

    public ExtraData getData() {
        return new ExtraData(generate.isSelected(), makePrivate.isSelected(), deprecate.isSelected());
    }
}