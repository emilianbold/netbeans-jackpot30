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

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.modules.jackpot30.impl.duplicates.ComputeDuplicates.DuplicateDescription;
import org.netbeans.modules.jackpot30.impl.duplicates.ComputeDuplicates.Span;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class DuplicatesListPanel extends javax.swing.JPanel {
    private final Collection<String> sourceRoots;
    private final Iterator<? extends DuplicateDescription> dupes;

    private int targetCount;

    public DuplicatesListPanel(Collection<String> sourceRoots, final Iterator<? extends DuplicateDescription> dupes) {
        this.sourceRoots = sourceRoots;
        this.dupes = dupes;
        
        initComponents();

        left.setContentType("text/x-java");
        left.putClientProperty(DuplicatesListPanel.class, new OffsetsBag(left.getDocument()));
        
        right.setContentType("text/x-java");
        right.putClientProperty(DuplicatesListPanel.class, new OffsetsBag(right.getDocument()));

        duplicatesList.setModel(new DefaultListModel());
        duplicatesList.setCellRenderer(new DuplicatesRendererImpl());
        duplicatesList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent arg0) {
                DuplicateDescription dd = (DuplicateDescription) duplicatesList.getSelectedValue();
                DefaultComboBoxModel l = new DefaultComboBoxModel();
                DefaultComboBoxModel r = new DefaultComboBoxModel();

                for (Span s : dd.dupes) {
                    l.addElement(s);
                    r.addElement(s);
                }

                leftFileList.setModel(l);
                rightFileList.setModel(r);

                leftFileList.setSelectedIndex(0);
                rightFileList.setSelectedIndex(1);
            }
        });

        leftFileList.setRenderer(new SpanRendererImpl());
        leftFileList.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setSpan(left, (Span) leftFileList.getSelectedItem());
            }
        });
        rightFileList.setRenderer(new SpanRendererImpl());
        rightFileList.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setSpan(right, (Span) rightFileList.getSelectedItem());
            }
        });

        progressLabel.setText("Looking for duplicates...");

        findMore();
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

        jScrollPane1 = new javax.swing.JScrollPane();
        duplicatesList = new javax.swing.JList();
        mainSplit2 = new BalancedSplitPane();
        rightPanel = new javax.swing.JPanel();
        rightFileList = new javax.swing.JComboBox();
        jScrollPane3 = new javax.swing.JScrollPane();
        right = new javax.swing.JEditorPane();
        leftPanel = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        left = new javax.swing.JEditorPane();
        leftFileList = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        progressLabel = new javax.swing.JLabel();
        findMore = new javax.swing.JLabel();

        duplicatesList.setPrototypeCellValue("9999999999999999999999999999999999999999999999999999999999999999999999");
        duplicatesList.setVisibleRowCount(4);
        jScrollPane1.setViewportView(duplicatesList);

        mainSplit2.setDividerLocation(400);

        rightPanel.setLayout(new java.awt.GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.ipadx = 324;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        rightPanel.add(rightFileList, gridBagConstraints);

        jScrollPane3.setViewportView(right);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        rightPanel.add(jScrollPane3, gridBagConstraints);

        mainSplit2.setRightComponent(rightPanel);

        leftPanel.setLayout(new java.awt.GridBagLayout());

        jScrollPane2.setViewportView(left);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        leftPanel.add(jScrollPane2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        leftPanel.add(leftFileList, gridBagConstraints);

        mainSplit2.setLeftComponent(leftPanel);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        progressLabel.setText(org.openide.util.NbBundle.getMessage(DuplicatesListPanel.class, "DuplicatesListPanel.progressLabel.text")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        jPanel1.add(progressLabel, gridBagConstraints);

        findMore.setText(org.openide.util.NbBundle.getMessage(DuplicatesListPanel.class, "DuplicatesListPanel.findMore.text")); // NOI18N
        findMore.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        findMore.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                findMoreMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        jPanel1.add(findMore, gridBagConstraints);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(mainSplit2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 906, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 906, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainSplit2, javax.swing.GroupLayout.DEFAULT_SIZE, 467, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void findMoreMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_findMoreMouseClicked
        findMore();
    }//GEN-LAST:event_findMoreMouseClicked

    private void findMore() {
        targetCount = duplicatesList.getModel().getSize() + 100;
        findMore.setVisible(false);
        WORKER.schedule(0);
    }

    private static String computeCommonPrefix(String origCommonPrefix, FileObject file) {
        String name = FileUtil.getFileDisplayName(file);

        if (origCommonPrefix == null) return name;

        int len = Math.min(origCommonPrefix.length(), name.length());

        for (int cntr = 0; cntr < len; cntr++) {
            if (origCommonPrefix.charAt(cntr) != name.charAt(cntr)) {
                return origCommonPrefix.substring(0, cntr);
            }
        }

        return origCommonPrefix;
    }
    
    private static void setSpan(JEditorPane pane, Span s) {
        try {
            pane.setText(s.file.asText());

            Rectangle top = pane.modelToView(0);
            Rectangle start = pane.modelToView(s.startOff);
            Rectangle end = pane.modelToView(s.endOff);

            if (top != null && start != null && end != null) {
                Rectangle toScroll = start.union(end);

                pane.scrollRectToVisible(top);
                pane.scrollRectToVisible(toScroll);
            }

            OffsetsBag bag = (OffsetsBag) pane.getClientProperty(DuplicatesListPanel.class);

            bag.clear();
            bag.addHighlight(s.startOff, s.endOff, HIGHLIGHT);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static final AttributeSet HIGHLIGHT = AttributesUtilities.createImmutable(StyleConstants.Background, new Color(0xDF, 0xDF, 0xDF, 0xff));

    private final class DuplicatesRendererImpl extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof DuplicateDescription)) return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            DuplicateDescription dd = (DuplicateDescription) value;
            Set<FileObject> files = new LinkedHashSet<FileObject>();
            String commonPrefix = null;

            for (Span s : dd.dupes) {
                commonPrefix = computeCommonPrefix(commonPrefix, s.file);
                files.add(s.file);
            }

            StringBuilder cap = new StringBuilder();

            OUTER: for (FileObject file : files) {
                String name = FileUtil.getFileDisplayName(file);

                if (cap.length() > 0) {
                    cap.append("    ");
                }
                
                for (String sr : sourceRoots) {
                    if (name.startsWith(sr)) {
                        cap.append(name.substring(Math.max(0, sr.lastIndexOf('/') + 1)));
                        continue OUTER;
                    }
                }
            }

            return super.getListCellRendererComponent(list, cap.toString(), index, isSelected, cellHasFocus);
        }
    }

    private final class SpanRendererImpl extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof Span)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
            Span span = (Span) value;

            return super.getListCellRendererComponent(list, FileUtil.getFileDisplayName(span.file), index, isSelected, cellHasFocus);
        }
    }

    public static final class HighlightLayerFactoryImpl implements HighlightsLayerFactory {
        public HighlightsLayer[] createLayers(Context cntxt) {
            OffsetsBag bag = (OffsetsBag) cntxt.getComponent().getClientProperty(DuplicatesListPanel.class);

            if (bag != null) {
                return new HighlightsLayer[] {
                    HighlightsLayer.create(DuplicatesListPanel.class.getName(), ZOrder.CARET_RACK, true, bag)
                };
            }

            return new HighlightsLayer[0];
        }
    }

    @ServiceProvider(service=MimeDataProvider.class)
    public static final class MDPI implements MimeDataProvider {

        private static final Lookup L = Lookups.singleton(new HighlightLayerFactoryImpl());

        public Lookup getLookup(MimePath mp) {
            if (mp.getPath().startsWith("text/x-java")) {
                return L;
            }

            return null;
        }
        
    }

    private static final class BalancedSplitPane extends JSplitPane {

        @Override
        @SuppressWarnings("deprecation")
        public void reshape(int x, int y, int w, int h) {
            super.reshape(x, y, w, h);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    setDividerLocation(0.5);
                }
            });
        }

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList duplicatesList;
    private javax.swing.JLabel findMore;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JEditorPane left;
    private javax.swing.JComboBox leftFileList;
    private javax.swing.JPanel leftPanel;
    private javax.swing.JSplitPane mainSplit2;
    private javax.swing.JLabel progressLabel;
    private javax.swing.JEditorPane right;
    private javax.swing.JComboBox rightFileList;
    private javax.swing.JPanel rightPanel;
    // End of variables declaration//GEN-END:variables

    private static final RequestProcessor DEFAULT_WORKER = new RequestProcessor(DuplicatesListPanel.class.getName(), 1, false, false);
    private final Task WORKER = DEFAULT_WORKER.create(new Runnable() {
        public void run() {
            if (dupes.hasNext()) {
                final DuplicateDescription dd = dupes.next();

                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        ((DefaultListModel)duplicatesList.getModel()).addElement(dd);

                        int size = duplicatesList.getModel().getSize();

                        if (size == 1) {
                            duplicatesList.setSelectedIndex(0);
                        }
                        
                        if (size >= targetCount) {
                            findMore.setVisible(true);
                            progressLabel.setText("Found " + size + " duplicated snippets.");
                        } else {
                            progressLabel.setText("Found " + size + " duplicated snippets and searching...");
                            WORKER.schedule(0);
                        }
                    }
                });
            }
        }
    });

}
