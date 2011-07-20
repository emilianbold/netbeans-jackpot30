/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010-2011 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010-2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.remotingapi.options;

import java.awt.Component;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.openide.NotificationLineSupport;
import org.openide.util.RequestProcessor;

/**
 *
 * @author lahvac
 */
public class CustomizeRemoteIndex extends javax.swing.JPanel {

    private final JButton okButton;

    public CustomizeRemoteIndex(JButton okButton) {
        this.okButton = okButton;
        initComponents();
        DocumentListener updateErrorsListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateErrors();
            }
            public void removeUpdate(DocumentEvent e) {
                updateErrors();
            }
            public void changedUpdate(DocumentEvent e) {}
        };
        folder.getDocument().addDocumentListener(updateErrorsListener);
        indexURL.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                indexURLUpdated();
            }
            public void removeUpdate(DocumentEvent e) {
                indexURLUpdated();
            }
            public void changedUpdate(DocumentEvent e) {
            }
        });
        indexInfo.setFont(UIManager.getFont("Label.font"));
        indexInfo.setBackground(UIManager.getColor("Label.background"));
        indexInfo.setDisabledTextColor(UIManager.getColor("Label.foreground"));
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        folderPanel = new javax.swing.JPanel();
        folderLabel = new javax.swing.JLabel();
        folderChooser = new javax.swing.JButton();
        folder = new javax.swing.JTextField();
        indexInfo = new javax.swing.JTextArea();
        remoteIndexPanel = new javax.swing.JPanel();
        indexURL = new javax.swing.JTextField();
        indexURLLabel = new javax.swing.JLabel();
        subIndex = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();

        setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setLayout(new java.awt.GridBagLayout());

        folderPanel.setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(folderLabel, org.openide.util.NbBundle.getMessage(CustomizeRemoteIndex.class, "CustomizeRemoteIndex.folderLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 6);
        folderPanel.add(folderLabel, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(folderChooser, org.openide.util.NbBundle.getMessage(CustomizeRemoteIndex.class, "CustomizeRemoteIndex.folderChooser.text")); // NOI18N
        folderChooser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                folderChooserActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        folderPanel.add(folderChooser, gridBagConstraints);

        folder.setText(org.openide.util.NbBundle.getMessage(CustomizeRemoteIndex.class, "CustomizeRemoteIndex.folder.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        folderPanel.add(folder, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        add(folderPanel, gridBagConstraints);

        indexInfo.setColumns(20);
        indexInfo.setEditable(false);
        indexInfo.setRows(5);
        indexInfo.setBorder(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(indexInfo, gridBagConstraints);

        indexURL.setColumns(40);
        indexURL.setText(org.openide.util.NbBundle.getMessage(CustomizeRemoteIndex.class, "CustomizeRemoteIndex.indexURL.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(indexURLLabel, org.openide.util.NbBundle.getMessage(CustomizeRemoteIndex.class, "CustomizeRemoteIndex.indexURLLabel.text")); // NOI18N

        subIndex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                subIndexActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CustomizeRemoteIndex.class, "CustomizeRemoteIndex.jLabel1.text")); // NOI18N

        javax.swing.GroupLayout remoteIndexPanelLayout = new javax.swing.GroupLayout(remoteIndexPanel);
        remoteIndexPanel.setLayout(remoteIndexPanelLayout);
        remoteIndexPanelLayout.setHorizontalGroup(
            remoteIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(remoteIndexPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(remoteIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(remoteIndexPanelLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(subIndex, 0, 589, Short.MAX_VALUE))
                    .addGroup(remoteIndexPanelLayout.createSequentialGroup()
                        .addComponent(indexURLLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(indexURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        remoteIndexPanelLayout.setVerticalGroup(
            remoteIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(remoteIndexPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(remoteIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(indexURLLabel)
                    .addComponent(indexURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(remoteIndexPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(subIndex, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        add(remoteIndexPanel, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void folderChooserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_folderChooserActionPerformed
        showFileChooser(folder);
}//GEN-LAST:event_folderChooserActionPerformed

    private void subIndexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_subIndexActionPerformed
        subindexSelectionUpdated();
    }//GEN-LAST:event_subIndexActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JTextField folder;
    private javax.swing.JButton folderChooser;
    private javax.swing.JLabel folderLabel;
    private javax.swing.JPanel folderPanel;
    private javax.swing.JTextArea indexInfo;
    private javax.swing.JTextField indexURL;
    private javax.swing.JLabel indexURLLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel remoteIndexPanel;
    private javax.swing.JComboBox subIndex;
    // End of variables declaration//GEN-END:variables

    private void showFileChooser(JTextField folder) {
        JFileChooser c = new JFileChooser();

        c.setSelectedFile(new File(folder.getText()));
        c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        c.setMultiSelectionEnabled(false);
        c.setApproveButtonText("Select");

        if (c.showDialog(this, null) == JFileChooser.APPROVE_OPTION) {
            folder.setText(c.getSelectedFile().getAbsolutePath());
        }
    }

    public void setIndex(RemoteIndex index) {
        folder.setText(index.folder);
        indexURL.setText(index.remote.toExternalForm());
        subIndex.setSelectedItem(index.remoteSegment);
    }

    public RemoteIndex getIndex() {
        try {
            return RemoteIndex.create(folder.getText(), new URL(indexURL.getText()), (String) subIndex.getSelectedItem());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private NotificationLineSupport notificationSupport;

    public void setNotificationSupport(NotificationLineSupport notificationSupport) {
        this.notificationSupport = notificationSupport;
    }

    private void updateErrors() {
        notificationSupport.clearMessages();
        
        File folderFile = new File(folder.getText());

        if (!folderFile.exists()) {
            notificationSupport.setErrorMessage("Specified directory does not exist.");
            okButton.setEnabled(false);
            return;
        }

        if (!folderFile.isDirectory()) {
            notificationSupport.setErrorMessage("Specified directory is not directory.");
            okButton.setEnabled(false);
            return ;
        }

        if (checkingIndexURL.get()) {
            notificationSupport.setInformationMessage("Checking index URL");
            okButton.setEnabled(false);
            return;
        }

        String urlError = checkingIndexURLError.get();

        if (urlError != null) {
            notificationSupport.setErrorMessage(urlError);
            okButton.setEnabled(false);
            return;
        }
        
        okButton.setEnabled(true);
    }

    private final AtomicBoolean checkingIndexURL = new AtomicBoolean();
    private final AtomicReference<String> checkingIndexURLContentCopy = new AtomicReference<String>();
    private final AtomicReference<String> checkingIndexURLError = new AtomicReference<String>();

    private void indexURLUpdated() {
        checkingIndexURLContentCopy.set(indexURL.getText());
        urlCheckerTask.cancel();
        urlCheckerTask.schedule(50);
    }

    private static final RequestProcessor WORKER = new RequestProcessor(CustomizeRemoteIndex.class.getName(), 1, false, false);
    private final RequestProcessor.Task urlCheckerTask = WORKER.create(new Runnable() {

        public void run() {
            checkingIndexURL.set(true);
            checkingIndexURLError.set(null);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateErrors();
                }
            });

            String urlText = checkingIndexURLContentCopy.get();
            Collection<? extends String> subindices = null;

            try {
                URL url = new URL(urlText);

                if (!url.getPath().endsWith("/"))
                    url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "/" + (url.getQuery() != null ? "?" + url.getQuery() : ""));
                
                subindices = WebUtilities.requestStringArrayResponse(url.toURI().resolve("list"), new AtomicBoolean());

                if (subindices.isEmpty()) {
                   checkingIndexURLError.set("Not an index.");
                }
            } catch (URISyntaxException ex) {
                checkingIndexURLError.set(ex.getLocalizedMessage());
            } catch (MalformedURLException ex) {
                checkingIndexURLError.set(ex.getLocalizedMessage());
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t) {//#6541019
                checkingIndexURLError.set("Invalid URL");
            }
            
            checkingIndexURL.set(false);

            final Collection<? extends String> subindicesFinal = subindices;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateErrors();

                    if (subindicesFinal == null || subindicesFinal.isEmpty()) return;

                    DefaultComboBoxModel model = (DefaultComboBoxModel) subIndex.getModel();
                    String selected = (String) model.getSelectedItem();

                    model.removeAllElements();

                    boolean containsSelection = false;
                    Map<String, String> displayNames = new HashMap<String, String>();

                    for (String subindex : subindicesFinal) {
                        String[] subindexSplit = subindex.split(":", 2);
                        if (subindexSplit[0].equals(selected)) containsSelection = true;
                        model.addElement(subindexSplit[0]);
                        displayNames.put(subindexSplit[0], subindexSplit[1]);
                    }

                    if (containsSelection) {
                        model.setSelectedItem(selected);
                    }

                    subindexSelectionUpdated();
                    subIndex.setRenderer(new RendererImpl(displayNames));
                }
            });
        }
    });

    private final AtomicReference<String> indexInfoURLContentCopy = new AtomicReference<String>();
    private final AtomicReference<String> indexInfoSubIndexCopy = new AtomicReference<String>();
    private void subindexSelectionUpdated() {
        indexInfoURLContentCopy.set(indexURL.getText());
        indexInfoSubIndexCopy.set((String) subIndex.getSelectedItem());
        indexInfoTask.cancel();
        indexInfoTask.schedule(50);
    }

    private final RequestProcessor.Task indexInfoTask = WORKER.create(new Runnable() {

        public void run() {
            //XXX: the index currently does not provide the info anyway...
//            String urlText = indexInfoURLContentCopy.get();
//            String subIndex = indexInfoSubIndexCopy.get();
//            IndexInfo info = null;
//
//            try {
//                URL url = new URL(urlText);
//
//                if (!url.getPath().endsWith("/"))
//                    url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "/" + (url.getQuery() != null ? "?" + url.getQuery() : ""));
//
//                String indexInfoText = WebUtilities.requestStringResponse(url.toURI().resolve("info?path=" + WebUtilities.escapeForQuery(subIndex)));
//                info = IndexInfo.empty();
//
//                if (indexInfoText != null)
//                    Pojson.update(info, indexInfoText);
//            } catch (URISyntaxException ex) {
//                Logger.getLogger(CustomizeRemoteIndex.class.getName()).log(Level.FINE, null, ex);
//            } catch (MalformedURLException ex) {
//                Logger.getLogger(CustomizeRemoteIndex.class.getName()).log(Level.FINE, null, ex);
//            }
//
//            final IndexInfo infoFinal = info;
//
//            SwingUtilities.invokeLater(new Runnable() {
//                public void run() {
//                    if (infoFinal != null) {
//                        indexInfo.setText(toDisplayText(infoFinal));
//                    } else {
//                        indexInfo.setText("");
//                    }
//                }
//            });
        }
    });

//    private static String toDisplayText(IndexInfo info) {
//        StringBuilder sb = new StringBuilder();
//
//        if (info.sourceLocation != null) {
//            sb.append("Source Location: ").append(info.sourceLocation).append("\n");
//        }
//        if (info.lastUpdate >= 0) {
//            sb.append("Last Update:\t").append(DateFormat.getDateTimeInstance().format(new Date(info.lastUpdate))).append("\n");
//        }
//        if (info.totalFiles >= 0) {
//            sb.append("Indexed Files:\t").append(info.totalFiles).append("\n");
//        }
//
//        return sb.toString();
//    }

    private static final class RendererImpl extends DefaultListCellRenderer {
        private final Map<String, String> displayNames;
        public RendererImpl(Map<String, String> displayNames) {
            this.displayNames = displayNames;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            return super.getListCellRendererComponent(list, displayNames.get(value), index, isSelected, cellHasFocus);
        }

    }
}
