/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.impl.examples;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Set;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.text.BadLocationException;
import org.netbeans.modules.jackpot30.impl.examples.Example.Option;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class ExamplesList<T> extends javax.swing.JPanel {

    private final DialogDescription<T> convertor;
    private final Class<T> dataClass;

    public ExamplesList(Iterable<? extends T> data, DialogDescription<T> convertor, Class<T> dataClass, Set<Option> require, Set<Option> forbidden) {
        this.convertor = convertor;
        this.dataClass = dataClass;
        
        initComponents();

        DefaultListModel listModel = new DefaultListModel();

        for (T t : data) {
            Set<Option> options = convertor.getOptions(t);
            if (!options.containsAll(require)) continue;
            if (!Collections.disjoint(options, forbidden)) continue;
            
            listModel.addElement(t);
        }

        list.setModel(listModel);
        list.setCellRenderer(new ExamplesRenderer());
        list.setSelectedIndex(0);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list = new javax.swing.JList();
        jScrollPane2 = new javax.swing.JScrollPane();
        pattern = new javax.swing.JEditorPane();

        jLabel1.setText(convertor.getHeader());

        list.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                listMouseClicked(evt);
            }
        });
        list.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                listValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(list);

        pattern.setContentType(org.openide.util.NbBundle.getMessage(ExamplesList.class, "ExamplesList.pattern.contentType")); // NOI18N
        jScrollPane2.setViewportView(pattern);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void listMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_listMouseClicked
        if (evt.getClickCount() > 1) {
            desc.setValue(DialogDescriptor.OK_OPTION);
            dialog.setVisible(false);
        }
    }//GEN-LAST:event_listMouseClicked

    private void listValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_listValueChanged
        T ex = dataClass.cast(list.getSelectedValue());

        pattern.setText(convertor.getCode(ex));
        
        try {
            Rectangle rect = pattern.modelToView(0);

            if (rect != null) {
                pattern.scrollRectToVisible(rect);
            }
        } catch (BadLocationException ex1) {
            Exceptions.printStackTrace(ex1);
        }
    }//GEN-LAST:event_listValueChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList list;
    private javax.swing.JEditorPane pattern;
    // End of variables declaration//GEN-END:variables

    private DialogDescriptor desc;
    private Dialog           dialog;

    public void setDialog(DialogDescriptor desc, Dialog dialog) {
        this.desc = desc;
        this.dialog = dialog;
    }

    public T getSelectedExample() {
        return dataClass.cast(list.getSelectedValue());
    }
    
    private class ExamplesRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            value = convertor.getDisplayName(dataClass.cast(value));
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    public static <T> T chooseExample(Iterable<? extends T> examplesList, DialogDescription<T> convertor, Class<T> dataClass, Set<Option> require, Set<Option> forbidden) {
        ExamplesList<T> examples = new ExamplesList<T>(examplesList, convertor, dataClass, require, forbidden);
        DialogDescriptor dd = new DialogDescriptor(examples, convertor.getCaption(), true, DialogDescriptor.OK_CANCEL_OPTION, DialogDescriptor.OK_OPTION, null);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dd);

        examples.setDialog(dd, dialog);
        dialog.setVisible(true);

        if (dd.getValue() == DialogDescriptor.OK_OPTION) {
            return examples.getSelectedExample();
        }

        return null;
    }

    public interface DialogDescription<T> {
        public String getCaption();
        public String getHeader();
        public String getDisplayName(T t);
        public String getCode(T t);
        public Set<Option> getOptions(T t);
    }
}