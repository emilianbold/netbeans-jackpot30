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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Refactoring",
id = "org.netbeans.modules.jackpot30.ide.usages.RemoteUsages")
@ActionRegistration(displayName = "#CTL_RemoteUsages")
@ActionReferences({
    @ActionReference(path = "Menu/Edit", position = 2350)
})
@Messages("CTL_RemoteUsages=Find Remote Usages...")
public final class RemoteUsages implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        JTextComponent comp = EditorRegistry.lastFocusedComponent(); //XXX

        if (comp == null) return;

        FileObject file = NbEditorUtilities.getFileObject(comp.getDocument());
        final int pos = comp.getCaretPosition();

        try {
            final String[] serialized = new String[1];
            
            JavaSource.forFileObject(file).runUserActionTask(new Task<CompilationController>() {
                @Override public void run(CompilationController parameter) throws Exception {
                    parameter.toPhase(JavaSource.Phase.RESOLVED);

                    TreePath tp = parameter.getTreeUtilities().pathFor(pos);
                    Element el = parameter.getTrees().getElement(tp);

                    if (el != null && Common.SUPPORTED_KINDS.contains(el.getKind())) {
                        serialized[0] = serialize(ElementHandle.create(el));
                    }
                }
            }, true);

            if (serialized[0] == null) return ; //XXX: warn user!

            List<FileObject> result = new ArrayList<FileObject>();

            for (RemoteIndex idx : RemoteIndex.loadIndices()) {
                URI resolved = new URI(idx.remote.toExternalForm() + "/usages/search?path=" + WebUtilities.escapeForQuery(idx.remoteSegment) + "&signatures=" + WebUtilities.escapeForQuery(serialized[0]));

                for (String path : WebUtilities.requestStringArrayResponse(resolved)) {
                    File f = new File(idx.folder, path);

                    result.add(FileUtil.toFileObject(f));
                }
            }

            RemoteUsagesWindowTopComponent.openFor(Nodes.constructSemiLogicalView(result));
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
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
}
