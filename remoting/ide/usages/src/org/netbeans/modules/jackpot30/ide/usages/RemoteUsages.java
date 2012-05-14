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
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dialog;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.ui.ElementHeaders;
import org.netbeans.api.java.source.ui.ScanDialog;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.jackpot30.common.api.JavaUtils;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Message;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
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
        final ElementDescription element = findElement(file, pos);

        if (element == null) {
            Message message = new NotifyDescriptor.Message("Cannot find usages of this element", NotifyDescriptor.Message.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notifyLater(message);
            return ;
        }

        final Set<SearchOptions> options = EnumSet.noneOf(SearchOptions.class);
        final JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JPanel dialogContent = constructDialog(element, options, okButton);

        DialogDescriptor dd = new DialogDescriptor(dialogContent, "Remote Find Usages", true, new Object[] {okButton, cancelButton}, okButton, DialogDescriptor.DEFAULT_ALIGN, null, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { }
        });
        final Dialog d = DialogDisplayer.getDefault().createDialog(dd);

        final AtomicBoolean cancel = new AtomicBoolean();

        okButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                okButton.setEnabled(false);
                WORKER.post(new FindUsagesWorker(options.contains(SearchOptions.FROM_BASE) ? element.superMethod : element.element, options, d, cancel));
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                cancel.set(true);
                d.setVisible(false);
            }
        });

        d.setVisible(true);
    }

    private static ElementDescription findElement(final FileObject file, final int pos) {
        final ElementDescription[] handle = new ElementDescription[1];

        final JavaSource js = JavaSource.forFileObject(file);

        ScanDialog.runWhenScanFinished(new Runnable() {
            @Override public void run() {
                try {
                    js.runUserActionTask(new Task<CompilationController>() {
                        @Override public void run(CompilationController parameter) throws Exception {
                            parameter.toPhase(JavaSource.Phase.RESOLVED);

                            TreePath tp = parameter.getTreeUtilities().pathFor(pos);
                            Element el = parameter.getTrees().getElement(tp);

                            if (el != null && JavaUtils.SUPPORTED_KINDS.contains(el.getKind())) {
                                handle[0] = new ElementDescription(parameter, el);
                            }
                        }
                    }, true);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

        }, "Find Remote Usages");

        return handle[0];
    }

    private JPanel constructDialog(ElementDescription toSearch, Set<SearchOptions> options, JButton ok) {
        JPanel searchKind;

        switch (toSearch.element.getKind()) {
            case METHOD: searchKind = new MethodOptions(toSearch, options); break;
            case CLASS:
            case INTERFACE:
            case ANNOTATION_TYPE: searchKind = new ClassOptions(options); break;
            default: searchKind = new JPanel(); break;
        }
        
        final JPanel progress = new JPanel();

        progress.setLayout(new CardLayout());
        progress.add(new JPanel(), "hide");
        progress.add(new JLabel("Querying remote server(s), please wait"), "show");

        ok.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                ((CardLayout) progress.getLayout()).show(progress, "show");
            }
        });

        JPanel result = new JPanel();

        result.setLayout(new BorderLayout());
        result.setBorder(new EmptyBorder(new Insets(12, 12, 12, 12)));

        result.add(new JLabel(toSearch.displayName), BorderLayout.NORTH);
        result.add(searchKind, BorderLayout.CENTER);
        result.add(progress, BorderLayout.SOUTH);

        return result;
    }
    
    public static final class ElementDescription {
        public final ElementHandle<?> element;
        public final String displayName;
        public final ElementHandle<?> superMethod;
        public final String superMethodDisplayName;

        public ElementDescription(CompilationInfo info, Element el) {
            this.displayName = displayNameForElement(el, info);

            if (el.getKind() == ElementKind.METHOD) {
                ExecutableElement base = (ExecutableElement) el;

                while (true) {
                    ExecutableElement current = info.getElementUtilities().getOverriddenMethod(base);

                    if (current == null) break;

                    base = current;
                }

                if (base != el) {
                    superMethod = ElementHandle.create(base);
                    superMethodDisplayName = displayNameForElement(base, info);
                } else {
                    superMethod = null;
                    superMethodDisplayName = null;
                }
            } else {
                superMethod = null;
                superMethodDisplayName = null;
            }

            element = ElementHandle.create(el);
        }

        private String displayNameForElement(Element el, CompilationInfo info) throws UnsupportedOperationException {
            switch (el.getKind()) {
                case METHOD:
                    return "<html>Method <b>" + ElementHeaders.getHeader(el, info, ElementHeaders.NAME + ElementHeaders.PARAMETERS) + "</b> of class <b>" + ElementHeaders.getHeader(el.getEnclosingElement(), info, ElementHeaders.NAME);
                case CONSTRUCTOR:
                    return "<html>Constructor <b>" + ElementHeaders.getHeader(el, info, ElementHeaders.NAME + ElementHeaders.PARAMETERS) + "</b> of class <b>" + ElementHeaders.getHeader(el.getEnclosingElement(), info, ElementHeaders.NAME);
                case CLASS:
                case INTERFACE:
                case ENUM:
                case ANNOTATION_TYPE:
                    return "<html>Type <b>" + ElementHeaders.getHeader(el, info, ElementHeaders.NAME);
                case FIELD:
                case ENUM_CONSTANT:
                    return "<html>Field <b>" + ElementHeaders.getHeader(el, info, ElementHeaders.NAME) + " of class " + ElementHeaders.getHeader(el.getEnclosingElement(), info, ElementHeaders.NAME);
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

    private static class FindUsagesWorker implements Runnable, Cancellable {
        
        private final ElementHandle<?> toSearch;
        private final Set<SearchOptions> options;
        private final Dialog d;
        private final AtomicBoolean cancel;

        public FindUsagesWorker(ElementHandle<?> toSearch, Set<SearchOptions> options, Dialog d, AtomicBoolean cancel) {
            this.toSearch = toSearch;
            this.options = options;
            this.d = d;
            this.cancel = cancel;
        }

        @Override public void run() {
            try {
                final String serialized = JavaUtils.serialize(toSearch);

                Set<FileObject> resultSet = new HashSet<FileObject>();
                List<FileObject> result = new ArrayList<FileObject>();
                Map<RemoteIndex, List<String>> unmappable = new HashMap<RemoteIndex, List<String>>();

                for (RemoteIndex idx : RemoteIndex.loadIndices()) {
                    FileObject localFolder = URLMapper.findFileObject(idx.getLocalFolder());

                    if (options.contains(SearchOptions.USAGES)) {
                        URI resolved = new URI(idx.remote.toExternalForm() + "/usages/search?path=" + WebUtilities.escapeForQuery(idx.remoteSegment) + "&signatures=" + WebUtilities.escapeForQuery(serialized));
                        Collection<? extends String> response = WebUtilities.requestStringArrayResponse(resolved, cancel);

                        if (cancel.get()) return;
                        if (response == null) continue;

                        for (String path : response) {
                            if (path.trim().isEmpty()) continue;
                            FileObject file = localFolder.getFileObject(path);

                            if (file != null) {
                                if (resultSet.add(file)) {
                                    result.add(file);
                                }
                            } else {
                                List<String> um = unmappable.get(idx);

                                if (um == null) {
                                    unmappable.put(idx, um = new ArrayList<String>());
                                }

                                um.add(path);
                            }
                        }
                    }

                    if (options.contains(SearchOptions.SUB)) {
                        URI resolved;
                        if (toSearch.getKind() == ElementKind.METHOD) {
                            resolved = new URI(idx.remote.toExternalForm() + "/implements/search?path=" + WebUtilities.escapeForQuery(idx.remoteSegment) + "&method=" + WebUtilities.escapeForQuery(serialized));
                        } else {
                            resolved = new URI(idx.remote.toExternalForm() + "/implements/search?path=" + WebUtilities.escapeForQuery(idx.remoteSegment) + "&type=" + WebUtilities.escapeForQuery(toSearch.getBinaryName()));
                        }

                        String response = WebUtilities.requestStringResponse(resolved, cancel);

                        if (cancel.get()) return;
                        if (response == null) continue;

                        //XXX:
                        Map<String, List<Map<String, String>>> formattedResponse = Pojson.load(LinkedHashMap.class, response);

                        for (Entry<String, List<Map<String, String>>> e : formattedResponse.entrySet()) {
                            for (Map<String, String> p : e.getValue()) {
                                String path = p.get("file");
                                FileObject file = localFolder.getFileObject(path);

                                if (file != null) {
                                    if (resultSet.add(file)) {
                                        result.add(file);
                                    }
                                } else {
                                    List<String> um = unmappable.get(idx);

                                    if (um == null) {
                                        unmappable.put(idx, um = new ArrayList<String>());
                                    }

                                    um.add(path);
                                }
                            }
                        }
                    }
                }

                final Node view = Nodes.constructSemiLogicalView(result, unmappable, toSearch, options);

                if (!cancel.get()) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            RemoteUsagesWindowTopComponent.openFor(view);
                        }
                    });
                }
            } catch (URISyntaxException ex) {
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

    public enum SearchOptions {
        USAGES,
        SUB,
        FROM_BASE;
    }
}
