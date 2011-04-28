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
package org.netbeans.modules.jackpot30.impl.indexing.options;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import org.netbeans.modules.jackpot30.impl.indexing.RemoteIndex;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

final class IndexPanel extends javax.swing.JPanel {

    private final IndexOptionsPanelController controller;

    IndexPanel(IndexOptionsPanelController controller) {
        this.controller = controller;
        initComponents();
        indices.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableDisable();
            }
        });
        enableDisable();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        indices = new javax.swing.JTable();
        addButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        editButton = new javax.swing.JButton();

        indices.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(indices);

        org.openide.awt.Mnemonics.setLocalizedText(addButton, org.openide.util.NbBundle.getMessage(IndexPanel.class, "IndexPanel.addButton.text")); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(removeButton, org.openide.util.NbBundle.getMessage(IndexPanel.class, "IndexPanel.removeButton.text")); // NOI18N
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(editButton, org.openide.util.NbBundle.getMessage(IndexPanel.class, "IndexPanel.editButton.text")); // NOI18N
        editButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 281, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(removeButton, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                    .addComponent(addButton, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE)
                    .addComponent(editButton, javax.swing.GroupLayout.DEFAULT_SIZE, 154, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 306, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(addButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(editButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeButton)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        addEditIndex(false);
    }//GEN-LAST:event_addButtonActionPerformed

    private void editButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editButtonActionPerformed
        addEditIndex(true);
    }//GEN-LAST:event_editButtonActionPerformed

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        TableModelImpl model = (TableModelImpl) indices.getModel();

        model.indices.remove(indices.getSelectedRow());
        model.fireTableDataChanged();
    }//GEN-LAST:event_removeButtonActionPerformed

    private void addEditIndex(boolean edit) {
        JButton okButton = new JButton("OK");
        CustomizeRemoteIndex panel = new CustomizeRemoteIndex(okButton);
        DialogDescriptor dd = new DialogDescriptor(panel, edit ? "Edit Index" : "Add Index", true, new Object[] {okButton, DialogDescriptor.CANCEL_OPTION}, okButton, DialogDescriptor.DEFAULT_ALIGN, null, null);

        dd.setClosingOptions(null);
        panel.setNotificationSupport(dd.createNotificationLineSupport());

        TableModelImpl model = (TableModelImpl) indices.getModel();

        if (edit) {
            panel.setIndex(model.indices.get(indices.getSelectedRow()));
        }

        if (DialogDisplayer.getDefault().notify(dd) == okButton) {
            RemoteIndex remoteIndex = panel.getIndex();

            if (edit) {
                int index = model.indices.indexOf(indices.getSelectedRow());

                model.indices.remove(index);
                model.indices.add(index, remoteIndex);
            } else {
                model.indices.add(remoteIndex);
            }

            model.fireTableDataChanged();
        }
    }

    private void enableDisable() {
        if (indices.getSelectedRow() != (-1)) {
            editButton.setEnabled(true);
            removeButton.setEnabled(true);
        } else {
            editButton.setEnabled(false);
            removeButton.setEnabled(false);
        }
    }

    void load() {
        TableModelImpl model = new TableModelImpl();

        for (RemoteIndex idx : RemoteIndex.loadIndices()) {
            model.indices.add(idx);
        }

        indices.setModel(model);
    }

    void store() {
        TableModelImpl model = (TableModelImpl) indices.getModel();
        
        RemoteIndex.saveIndices(model.indices);
    }

    boolean valid() {
        // TODO check whether form is consistent and complete
        return true;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JButton editButton;
    private javax.swing.JTable indices;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables

    private static final class TableModelImpl extends AbstractTableModel {

        private final List<RemoteIndex> indices = new ArrayList<RemoteIndex>();

        public int getRowCount() {
            return indices.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0: return "Local folder";
                case 1: return "Remote URL";
                case 2: return "Remote project";
                default: throw new IllegalStateException();
            }
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            RemoteIndex idx = indices.get(rowIndex);

            switch (columnIndex) {
                case 0: return idx.folder;
                case 1: return idx.remote.toExternalForm();
                case 2: return idx.remoteSegment;
                default: throw new IllegalStateException();
            }
        }
    }
}