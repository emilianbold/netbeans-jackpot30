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

package org.netbeans.modules.jackpot30.impl.refactoring;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.openide.util.NbPreferences;
import org.openide.util.Union2;

/**
 *
 * @author lahvac
 */
public class FindDuplicatesRefactoringPanel extends javax.swing.JPanel {

    private final Map<String, Collection<HintDescription>> displayName2Hints;
    private final ChangeListener changeListener;
    
    public FindDuplicatesRefactoringPanel(final ChangeListener parent, boolean allowVerify) {
        Set<ClassPath> cps = new HashSet<ClassPath>();

        //TODO: bootclasspath?
        cps.addAll(GlobalPathRegistry.getDefault().getPaths(ClassPath.COMPILE));
        cps.addAll(GlobalPathRegistry.getDefault().getPaths(ClassPath.SOURCE));
        
        Collection<? extends HintDescription> hints = Utilities.listAllHints(cps);
        
        displayName2Hints = Utilities.sortOutHints(hints, new TreeMap<String, Collection<HintDescription>>());

        initComponents();

        DefaultListModel all = new DefaultListModel();
        DefaultListModel selected = new DefaultListModel();

        for (String dn : displayName2Hints.keySet()) {
            all.addElement(dn);
        }

        allHints.setModel(all);
        selectedHints.setModel(selected);
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                parent.stateChanged(new ChangeEvent(FindDuplicatesRefactoringPanel.this));
            }
            public void removeUpdate(DocumentEvent e) {
                parent.stateChanged(new ChangeEvent(FindDuplicatesRefactoringPanel.this));
            }
            public void changedUpdate(DocumentEvent e) {}
        };
        pattern.getDocument().addDocumentListener(dl);
        ItemListener ilImpl = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                parent.stateChanged(new ChangeEvent(FindDuplicatesRefactoringPanel.this));
                updateFolderSelectionPanel();
            }
        };
        scope.addItemListener(ilImpl);
        verify.addItemListener(ilImpl);
        folderCombo.addItemListener(ilImpl);

        ((JTextField) folderCombo.getEditor().getEditorComponent()).getDocument().addDocumentListener(dl);

        if (!allowVerify) {
            verify.setVisible(false);
        }
        
        DefaultComboBoxModel dcbm = new DefaultComboBoxModel();

        for (Scope s : EnumSet.of(Scope.ALL_OPENED_PROJECTS, Scope.GIVEN_FOLDER)) {
            dcbm.addElement(s);
        }
        
        scope.setModel(dcbm);
        scope.setRenderer(new RendererImpl());

        enableDisable();
        updateFolderSelectionPanel();

        this.changeListener = parent;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        main = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        pattern = new javax.swing.JTextPane();
        scope = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        verify = new javax.swing.JCheckBox();
        knownPatternsPanel = new javax.swing.JPanel();
        allHintsLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        allHints = new javax.swing.JList();
        jScrollPane3 = new javax.swing.JScrollPane();
        selectedHints = new javax.swing.JList();
        selectedHintsLabel = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        addHint = new javax.swing.JButton();
        addAllHints = new javax.swing.JButton();
        removeHint = new javax.swing.JButton();
        removeAllHints = new javax.swing.JButton();
        knowPatterns = new javax.swing.JRadioButton();
        customPattern = new javax.swing.JRadioButton();
        jPanel1 = new javax.swing.JPanel();
        folderLabel = new javax.swing.JLabel();
        folderCombo = new javax.swing.JComboBox();
        folderChooser = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setViewportView(pattern);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 498;
        gridBagConstraints.ipady = 153;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(jScrollPane1, gridBagConstraints);

        scope.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 440;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(scope, gridBagConstraints);

        jLabel2.setLabelFor(scope);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.jLabel2.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(18, 0, 0, 0);
        add(jLabel2, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(verify, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.verify.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(18, 0, 0, 0);
        add(verify, gridBagConstraints);

        knownPatternsPanel.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(allHintsLabel, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.allHintsLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 6);
        knownPatternsPanel.add(allHintsLabel, gridBagConstraints);

        allHints.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane2.setViewportView(allHints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 6);
        knownPatternsPanel.add(jScrollPane2, gridBagConstraints);

        selectedHints.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        jScrollPane3.setViewportView(selectedHints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 0, 0);
        knownPatternsPanel.add(jScrollPane3, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(selectedHintsLabel, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.selectedHintsLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        knownPatternsPanel.add(selectedHintsLabel, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(addHint, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.addHint.text")); // NOI18N
        addHint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addHintActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanel2.add(addHint, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(addAllHints, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.addAllHints.text")); // NOI18N
        addAllHints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAllHintsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        jPanel2.add(addAllHints, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(removeHint, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.removeHint.text")); // NOI18N
        removeHint.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeHintActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 6, 0);
        jPanel2.add(removeHint, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(removeAllHints, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.removeAllHints.text")); // NOI18N
        removeAllHints.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAllHintsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        jPanel2.add(removeAllHints, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 5);
        knownPatternsPanel.add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 288;
        gridBagConstraints.ipady = 149;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        add(knownPatternsPanel, gridBagConstraints);

        main.add(knowPatterns);
        knowPatterns.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(knowPatterns, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.knowPatterns.text")); // NOI18N
        knowPatterns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                knowPatternsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(knowPatterns, gridBagConstraints);

        main.add(customPattern);
        org.openide.awt.Mnemonics.setLocalizedText(customPattern, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.customPattern.text")); // NOI18N
        customPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customPatternActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(18, 0, 0, 0);
        add(customPattern, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(folderLabel, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.folderLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 6);
        jPanel1.add(folderLabel, gridBagConstraints);

        folderCombo.setEditable(true);
        folderCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 6);
        jPanel1.add(folderCombo, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(folderChooser, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.folderChooser.text")); // NOI18N
        folderChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                folderChooserActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(folderChooser, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(6, 24, 0, 0);
        add(jPanel1, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void addHintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addHintActionPerformed
        for (Object selected : allHints.getSelectedValues()) {
            ((DefaultListModel) selectedHints.getModel()).addElement(selected);
            ((DefaultListModel) allHints.getModel()).removeElement(selected);
        }
        changeListener.stateChanged(new ChangeEvent(this));
}//GEN-LAST:event_addHintActionPerformed

    private void addAllHintsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAllHintsActionPerformed
        for (Object o : ((DefaultListModel) allHints.getModel()).toArray()) {
            ((DefaultListModel) selectedHints.getModel()).addElement(o);
        }
        ((DefaultListModel) allHints.getModel()).removeAllElements();
        changeListener.stateChanged(new ChangeEvent(this));
}//GEN-LAST:event_addAllHintsActionPerformed

    private void removeHintActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeHintActionPerformed
        for (Object selected : selectedHints.getSelectedValues()) {
            ((DefaultListModel) allHints.getModel()).addElement(selected);
            ((DefaultListModel) selectedHints.getModel()).removeElement(selected);
        }
        changeListener.stateChanged(new ChangeEvent(this));
}//GEN-LAST:event_removeHintActionPerformed

    private void removeAllHintsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAllHintsActionPerformed
        for (Object o : ((DefaultListModel) selectedHints.getModel()).toArray()) {
            ((DefaultListModel) allHints.getModel()).addElement(o);
        }
        ((DefaultListModel) selectedHints.getModel()).removeAllElements();
        changeListener.stateChanged(new ChangeEvent(this));
}//GEN-LAST:event_removeAllHintsActionPerformed

    private void customPatternActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_customPatternActionPerformed
        enableDisable();
    }//GEN-LAST:event_customPatternActionPerformed

    private void knowPatternsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_knowPatternsActionPerformed
        enableDisable();
    }//GEN-LAST:event_knowPatternsActionPerformed

    private void folderChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_folderChooserActionPerformed
        JFileChooser c = new JFileChooser();

        c.setSelectedFile(new File(getSelectedFolder()));
        c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        c.setMultiSelectionEnabled(false);
        c.setApproveButtonText("Select");

        if (c.showDialog(this, null) == JFileChooser.APPROVE_OPTION) {
            setSelectedFolder(c.getSelectedFile().getAbsolutePath());
        }
    }//GEN-LAST:event_folderChooserActionPerformed

    private void enableDisable() {
        setEnabled(knownPatternsPanel, knowPatterns.isSelected());
        setEnabled(pattern, customPattern.isSelected());
    }

    private static void setEnabled(JComponent c, boolean enabled) {
        c.setEnabled(enabled);

        for (Component cc : c.getComponents()) {
            if (cc instanceof JComponent) {
                setEnabled((JComponent) cc, enabled);
            }
        }
    }
    
    public void setPattern(Union2<String, Iterable<? extends HintDescription>> pattern) {
        if (pattern.hasFirst()) {
            customPattern.setSelected(true);
            this.pattern.setText(pattern.first() != null ? pattern.first() : "");
        } else {
            knowPatterns.setSelected(true);
            
            Set<String> selected = new HashSet<String>();

            for (HintDescription d : pattern.second()) {
                selected.add(d.getDisplayName());
            }

            DefaultListModel allModel = (DefaultListModel) allHints.getModel();
            DefaultListModel selectedModel = (DefaultListModel) selectedHints.getModel();

            allModel.clear();
            selectedModel.clear();
            
            for (String dn : displayName2Hints.keySet()) {
                if (selected.contains(dn)) {
                    selectedModel.addElement(dn);
                } else {
                    allModel.addElement(dn);
                }
            }
        }

        enableDisable();
    }

    public Union2<String, Iterable<? extends HintDescription>> getPattern() {
        if (customPattern.isSelected()) {
            return Union2.createFirst(this.pattern.getText());
        } else {
            List<HintDescription> hints = new LinkedList<HintDescription>();

            for (Object dn : ((DefaultListModel) selectedHints.getModel()).toArray()) {
                hints.addAll(displayName2Hints.get((String) dn));
            }

            return Union2.<String, Iterable<? extends HintDescription>>createSecond(hints);
        }
    }

    public void setScope(Scope scope) {
        this.scope.setSelectedItem(scope);
    }

    public Scope getScope() {
        return (Scope) this.scope.getSelectedItem();
    }

    public boolean getVerify() {
        return verify.isSelected();
    }

    public void setVerify(boolean verify) {
        this.verify.setSelected(verify);
    }

    private void updateFolderSelectionPanel() {
        boolean enabled = scope.getSelectedItem() == Scope.GIVEN_FOLDER;

        folderCombo.setEnabled(enabled);
        folderChooser.setEnabled(enabled);
    }

    public String getSelectedFolder() {
        return ((JTextField) folderCombo.getEditor().getEditorComponent()).getText();
    }

    public void setSelectedFolder(String folder) {
        folderCombo.setSelectedItem(folder);
    }

    private static final String FOLDERS_COMBO_KEY = FindDuplicatesRefactoringPanel.class.getName().replace('.', '/') + "/foldersCombo";

    void initializeFoldersCombo() {
        String folders = NbPreferences.forModule(FindDuplicatesRefactoringPanel.class).get(FOLDERS_COMBO_KEY, "");
        DefaultComboBoxModel dcbm = new DefaultComboBoxModel();

        for (String f : folders.split(";")) { //TODO: escape :
            dcbm.addElement(f);
        }

        folderCombo.setModel(dcbm);
        if (dcbm.getSize() > 0)
            folderCombo.setSelectedIndex(0);
    }

    void saveFoldersCombo() {
        Set<String> data = new LinkedHashSet<String>();

        data.add((String) folderCombo.getSelectedItem());

        for (int cntr = 0; cntr < folderCombo.getModel().getSize(); cntr++) {
            String f = (String) folderCombo.getModel().getElementAt(cntr);

            data.add(f);
        }

        StringBuilder persistent = new StringBuilder();

        for (String d : data) {
            if (persistent.length() > 0) {
                persistent.append(";");
            }

            persistent.append(d);
        }

        NbPreferences.forModule(FindDuplicatesRefactoringPanel.class).put(FOLDERS_COMBO_KEY, persistent.toString());
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addAllHints;
    private javax.swing.JButton addHint;
    private javax.swing.JList allHints;
    private javax.swing.JLabel allHintsLabel;
    private javax.swing.JRadioButton customPattern;
    private javax.swing.JButton folderChooser;
    private javax.swing.JComboBox folderCombo;
    private javax.swing.JLabel folderLabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JRadioButton knowPatterns;
    private javax.swing.JPanel knownPatternsPanel;
    private javax.swing.ButtonGroup main;
    private javax.swing.JTextPane pattern;
    private javax.swing.JButton removeAllHints;
    private javax.swing.JButton removeHint;
    private javax.swing.JComboBox scope;
    private javax.swing.JList selectedHints;
    private javax.swing.JLabel selectedHintsLabel;
    private javax.swing.JCheckBox verify;
    // End of variables declaration//GEN-END:variables

    private static final class RendererImpl extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String displayName;

            if (value instanceof Scope) {
                displayName = SCOPE_DISPLAY_NAMES.get((Scope) value);
            } else {
                displayName = value.toString();
            }
            
            return super.getListCellRendererComponent(list, displayName, index, isSelected, cellHasFocus);
        }

    }

    private static final Map<Scope, String> SCOPE_DISPLAY_NAMES;

    static {
        SCOPE_DISPLAY_NAMES = new EnumMap<Scope, String>(Scope.class);
        SCOPE_DISPLAY_NAMES.put(Scope.ALL_OPENED_PROJECTS, "All Opened Projects");
        SCOPE_DISPLAY_NAMES.put(Scope.ALL_REMOTE_PROJECTS, "All Remote Projects");
        SCOPE_DISPLAY_NAMES.put(Scope.GIVEN_FOLDER, "Selected Folder");
    }
    
}
