/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.refactoring;

import java.awt.CardLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.modules.jackpot30.impl.examples.Example;
import org.netbeans.modules.jackpot30.impl.examples.Example.Option;
import org.netbeans.modules.jackpot30.impl.examples.LoadExamples;
import org.netbeans.modules.jackpot30.impl.refactoring.ExamplesList.DialogDescription;
import org.netbeans.modules.java.hints.jackpot.impl.Utilities;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.java.hints.jackpot.spi.HintDescription;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.openide.util.Union2;

/**
 *
 * @author lahvac
 */
public class FindDuplicatesRefactoringPanel extends javax.swing.JPanel {

    private final Map<String, Collection<HintDescription>> displayName2Hints;
    private final ChangeListener changeListener;
    private final boolean query;
    
    public FindDuplicatesRefactoringPanel(final ChangeListener parent, boolean query) {
        this.changeListener = parent;
        this.query = query;
        
        Set<ClassPath> cps = new HashSet<ClassPath>();

        cps.addAll(GlobalPathRegistry.getDefault().getPaths(ClassPath.BOOT));
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
                stateChanged();
            }
            public void removeUpdate(DocumentEvent e) {
                stateChanged();
            }
            public void changedUpdate(DocumentEvent e) {}
        };
        pattern.getDocument().addDocumentListener(dl);

        if (!query) {
            verify.setVisible(false);
        }

        scopesPanel.setChangeListener(changeListener);
        
        enableDisable();
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
        verify = new javax.swing.JCheckBox();
        patternSelection = new javax.swing.JPanel();
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
        customPatternPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        pattern = new javax.swing.JTextPane();
        jPanel3 = new javax.swing.JPanel();
        recentButton = new javax.swing.JButton();
        examplesButton = new javax.swing.JButton();
        patternTypeSelectionPanel = new javax.swing.JPanel();
        knowPatterns = new javax.swing.JRadioButton();
        customPattern = new javax.swing.JRadioButton();
        jPanel1 = new javax.swing.JPanel();
        scopesPanel = new org.netbeans.modules.jackpot30.impl.refactoring.ScopesPanel();

        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(verify, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.verify.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(18, 0, 0, 0);
        add(verify, gridBagConstraints);

        patternSelection.setLayout(new java.awt.CardLayout());

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
        allHints.setPrototypeCellValue(HINTS_LIST_PROTOTYPE);
        allHints.setVisibleRowCount(24);
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
        selectedHints.setPrototypeCellValue(HINTS_LIST_PROTOTYPE);
        selectedHints.setVisibleRowCount(24);
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

        patternSelection.add(knownPatternsPanel, "knownPatterns");

        customPatternPanel.setLayout(new java.awt.GridBagLayout());

        jScrollPane1.setViewportView(pattern);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        customPatternPanel.add(jScrollPane1, gridBagConstraints);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 5));

        recentButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/jackpot30/impl/resources/recent_icon.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(recentButton, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.recentButton.text")); // NOI18N
        recentButton.setToolTipText(org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.recentButton.toolTipText")); // NOI18N
        recentButton.setBorderPainted(false);
        recentButton.setContentAreaFilled(false);
        recentButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recentButtonActionPerformed(evt);
            }
        });
        jPanel3.add(recentButton);

        examplesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/netbeans/modules/jackpot30/impl/resources/examples_icon.png"))); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(examplesButton, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.examplesButton.text")); // NOI18N
        examplesButton.setToolTipText(org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "BTN_Examples")); // NOI18N
        examplesButton.setBorderPainted(false);
        examplesButton.setContentAreaFilled(false);
        examplesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                examplesButtonActionPerformed(evt);
            }
        });
        jPanel3.add(examplesButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        customPatternPanel.add(jPanel3, gridBagConstraints);

        patternSelection.add(customPatternPanel, "customPattern");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(patternSelection, gridBagConstraints);

        patternTypeSelectionPanel.setLayout(new java.awt.GridBagLayout());

        main.add(knowPatterns);
        knowPatterns.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(knowPatterns, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.knowPatterns.text")); // NOI18N
        knowPatterns.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                knowPatternsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        patternTypeSelectionPanel.add(knowPatterns, gridBagConstraints);

        main.add(customPattern);
        org.openide.awt.Mnemonics.setLocalizedText(customPattern, org.openide.util.NbBundle.getMessage(FindDuplicatesRefactoringPanel.class, "FindDuplicatesRefactoringPanel.customPattern.text")); // NOI18N
        customPattern.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                customPatternActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 24, 0, 0);
        patternTypeSelectionPanel.add(customPattern, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        patternTypeSelectionPanel.add(jPanel1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        add(patternTypeSelectionPanel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(18, 0, 0, 0);
        add(scopesPanel, gridBagConstraints);
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

    private void examplesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_examplesButtonActionPerformed
        Example ex = ExamplesList.chooseExample(LoadExamples.loadExamples(), new ExamplesConvertor(), Example.class, query ? EnumSet.noneOf(Option.class) : EnumSet.of(Option.FIX), query ? EnumSet.of(Option.FIX) : EnumSet.noneOf(Option.class));

        if (ex != null) {
            pattern.setText(ex.getCode());
        }
    }//GEN-LAST:event_examplesButtonActionPerformed

    private void recentButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recentButtonActionPerformed
        String cod = ExamplesList.chooseExample(loadRecent(),
                                                new DialogDescription<String>() {
                                                    public String getDisplayName(String t) {
                                                        return t;
                                                    }
                                                    public String getCode(String t) {
                                                        return t;
                                                    }
                                                    public Set<Option> getOptions(String t) {
                                                        return query ? EnumSet.noneOf(Option.class) : EnumSet.of(Option.FIX);
                                                    }
                                                    public String getCaption() {
                                                        return "Recent patterns";
                                                    }
                                                    public String getHeader() {
                                                        return "Patterns:";
                                                    }
                                                },
                                                String.class,
                                                query ? EnumSet.noneOf(Option.class) : EnumSet.of(Option.FIX), query ? EnumSet.of(Option.FIX) : EnumSet.noneOf(Option.class));

        if (cod != null) {
            pattern.setText(cod);
        }
    }//GEN-LAST:event_recentButtonActionPerformed

    private void stateChanged() {
        if (SwingUtilities.isEventDispatchThread()) {
            changeListener.stateChanged(new ChangeEvent(this));
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    stateChanged();
                }
            });
        }
    }

    private void enableDisable() {
        String toSelect = knowPatterns.isSelected() ? "knownPatterns" : "customPattern";

        ((CardLayout) patternSelection.getLayout()).show(patternSelection, toSelect);
        stateChanged();
    }

    public void setPattern(Union2<String, Iterable<? extends HintDescription>> pattern) {
        if (pattern.hasFirst()) {
            customPattern.setSelected(true);
            this.pattern.setText(pattern.first() != null ? pattern.first() : "");
        } else {
            knowPatterns.setSelected(true);
            
            Set<String> selected = new HashSet<String>();

            for (HintDescription d : pattern.second()) {
                selected.add(d.getMetadata().displayName);
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
        scopesPanel.setScope(scope);
    }

    public Scope getScope() {
        return scopesPanel.getScope();
    }

    public boolean getVerify() {
        return verify.isSelected();
    }

    public void setVerify(boolean verify) {
        this.verify.setSelected(verify);
    }

    void fillInFromSettings() {
        scopesPanel.fillInFromSettings();

        List<String> recent = loadRecent();

        if (!recent.isEmpty()) {
            pattern.setText(recent.get(0));
        }
    }

    void saveScopesCombo() {
        scopesPanel.saveScopesCombo();

        String currentPattern = pattern.getText().trim();
        List<String> recent = loadRecent();

        recent.remove(currentPattern);
        recent.add(0, currentPattern);

        while (recent.size() > MAX_RECENT) {
            recent.remove(recent.size() - 1);
        }

        Preferences prefs = NbPreferences.forModule(FindDuplicatesRefactoringPanel.class);
        Preferences recentPatterns = prefs.node(query ? RECENT_PATTERNS_QUERY : RECENT_PATTERNS_APPLY);
        int i = 0;

        for (String r : recent) {
            recentPatterns.put("pattern_" + i++, r);
        }
    }

    private static final int MAX_RECENT = 50;
    private static final String RECENT_PATTERNS_QUERY = "recentPatternsQuery";
    private static final String RECENT_PATTERNS_APPLY = "recentPatternsApply";

    private List<String> loadRecent() {
        Preferences prefs = NbPreferences.forModule(FindDuplicatesRefactoringPanel.class);

        if (prefs == null) return Collections.emptyList();

        List<String> recent = new LinkedList<String>();
        Preferences recentPatterns = prefs.node(query ? RECENT_PATTERNS_QUERY : RECENT_PATTERNS_APPLY);

        if (recentPatterns != null) {
            try {
                for (String k : recentPatterns.keys()) {
                    if (k.startsWith("pattern_")) {
                        recent.add(recentPatterns.get(k, null));
                    }
                }
            } catch (BackingStoreException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return recent;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addAllHints;
    private javax.swing.JButton addHint;
    private javax.swing.JList allHints;
    private javax.swing.JLabel allHintsLabel;
    private javax.swing.JRadioButton customPattern;
    private javax.swing.JPanel customPatternPanel;
    private javax.swing.JButton examplesButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JRadioButton knowPatterns;
    private javax.swing.JPanel knownPatternsPanel;
    private javax.swing.ButtonGroup main;
    private javax.swing.JTextPane pattern;
    private javax.swing.JPanel patternSelection;
    private javax.swing.JPanel patternTypeSelectionPanel;
    private javax.swing.JButton recentButton;
    private javax.swing.JButton removeAllHints;
    private javax.swing.JButton removeHint;
    private org.netbeans.modules.jackpot30.impl.refactoring.ScopesPanel scopesPanel;
    private javax.swing.JList selectedHints;
    private javax.swing.JLabel selectedHintsLabel;
    private javax.swing.JCheckBox verify;
    // End of variables declaration//GEN-END:variables

    private static final String HINTS_LIST_PROTOTYPE  = "012345678901234567890123456789";

    private static final class ExamplesConvertor implements DialogDescription<Example> {

        public String getDisplayName(Example t) {
            return t.getDisplayName();
        }

        public String getCode(Example t) {
            return t.getCode();
        }

        public Set<Option> getOptions(Example t) {
            return t.getOptions();
        }

        public String getCaption() {
            return "Choose Example";
        }

        public String getHeader() {
            return "Examples:";
        }

    }
}
