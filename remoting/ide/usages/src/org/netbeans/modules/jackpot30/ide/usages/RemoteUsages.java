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
package org.netbeans.modules.jackpot30.ide.usages;

import com.sun.source.util.TreePath;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

@ActionID(category = "Refactoring",
id = "org.netbeans.modules.jackpot30.ide.usages.RemoteUsages")
@ActionRegistration(displayName = "#CTL_RemoteUsages")
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 2350)
})
@Messages("CTL_RemoteUsages=Find Remote Usages...")
public final class RemoteUsages implements ActionListener {

    private final RequestProcessor WORKER = new RequestProcessor(RemoteUsages.class.getName(), 1, false, false);
    
    public void actionPerformed(ActionEvent e) {
        JTextComponent comp = EditorRegistry.lastFocusedComponent(); //XXX

        if (comp == null) return;

        final FileObject file = NbEditorUtilities.getFileObject(comp.getDocument());
        final int pos = comp.getCaretPosition();

        DialogDescriptor dd = new DialogDescriptor("Querying remote server(s), please wait", "Please Wait", true, new Object[0], null, DialogDescriptor.DEFAULT_ALIGN, null, null);
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);

        WORKER.post(new FindUsagesWorker(file, pos, d));

        d.setVisible(true);
    }

    //XXX:
    public static String serialize(ElementHandle<?> h) {
        StringBuilder result = new StringBuilder();

        result.append(h.getKind());

        try {
            Field signaturesField = ElementHandle.class.getDeclaredField("signatures");

            signaturesField.setAccessible(true);

            String[] signatures = (String[]) signaturesField.get(h);

            for (String sig : signatures) {
                result.append(":");
                result.append(sig);
            }
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchFieldException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }

        return result.toString();
    }

    private static class FindUsagesWorker implements Runnable, Cancellable {

        private final FileObject file;
        private final int pos;
        private final Dialog d;
        private final AtomicBoolean cancel;

        public FindUsagesWorker(FileObject file, int pos, Dialog d) {
            this.file = file;
            this.pos = pos;
            this.d = d;
            this.cancel = new AtomicBoolean();
        }

        @Override public void run() {
            try {
                final ElementHandle<?>[] handle = new ElementHandle<?>[1];
                final String[] serialized = new String[1];

                JavaSource.forFileObject(file).runUserActionTask(new Task<CompilationController>() {
                    @Override public void run(CompilationController parameter) throws Exception {
                        parameter.toPhase(JavaSource.Phase.RESOLVED);

                        TreePath tp = parameter.getTreeUtilities().pathFor(pos);
                        Element el = parameter.getTrees().getElement(tp);

                        if (el != null && Common.SUPPORTED_KINDS.contains(el.getKind())) {
                            serialized[0] = serialize(handle[0] = ElementHandle.create(el));
                        }
                    }
                }, true);

                if (serialized[0] == null) return ; //XXX: warn user!

                List<FileObject> result = new ArrayList<FileObject>();

                for (RemoteIndex idx : RemoteIndex.loadIndices()) {
                    URI resolved = new URI(idx.remote.toExternalForm() + "/usages/search?path=" + WebUtilities.escapeForQuery(idx.remoteSegment) + "&signatures=" + WebUtilities.escapeForQuery(serialized[0]));
                    Collection<? extends String> response = WebUtilities.requestStringArrayResponse(resolved, cancel);

                    if (cancel.get()) return;
                    if (response == null) continue;
                    
                    for (String path : response) {
                        File f = new File(idx.folder, path);

                        result.add(FileUtil.toFileObject(f));
                    }
                }

                final Node view = Nodes.constructSemiLogicalView(result, handle[0]);

                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        RemoteUsagesWindowTopComponent.openFor(view);
                    }
                });
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                cancel.set(true);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override public void run() {
                        d.setVisible(false);
                    }
                });
            }
        }

        @Override public boolean cancel() {
            cancel.set(true);
            return true;
        }
    }
}
