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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.swing.Icon;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.modules.jackpot30.common.api.JavaUtils;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.netbeans.modules.refactoring.java.WhereUsedElement;
import org.netbeans.modules.refactoring.java.ui.tree.ElementGrip;
import org.netbeans.modules.refactoring.java.ui.tree.RefactoringTreeElement;
import org.netbeans.modules.refactoring.spi.ui.TreeElement;
import org.netbeans.modules.refactoring.spi.ui.TreeElementFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.nodes.Node;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;

public final class RemoteUsages {

    //XXX: handle unmappable result!
    public static List<FileObject> findUsages(ElementHandle<?> toSearch, Set<SearchOptions> options, AtomicBoolean cancel) {
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

                    if (cancel.get()) return Collections.emptyList();
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

                    if (cancel.get()) return Collections.emptyList();
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

            return result;
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
            return Collections.emptyList();
        } finally {
            cancel.set(true);
        }
    }

    public enum SearchOptions {
        USAGES,
        SUB,
        FROM_BASE;
    }

    public static boolean computeOccurrences(FileObject file, final ElementHandle<?> eh, final Set<RemoteUsages.SearchOptions> options, final TreeElement parent, final List<TreeElement> toPopulate) {
        final boolean[] success = new boolean[] {true};

        try {
            JavaSource.forFileObject(file).runUserActionTask(new Task<CompilationController>() {
                @Override public void run(final CompilationController parameter) throws Exception {
                    parameter.toPhase(Phase.RESOLVED);

                    final Element toFind = eh.resolve(parameter);

                    if (toFind == null) {
                        return;
                    }

                    final AtomicBoolean stop = new AtomicBoolean();

                    new CancellableTreePathScanner<Void, Void>(stop) {
                        @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                            handleNode(node.getName(), getCurrentPath());
                            return super.visitIdentifier(node, p);
                        }
                        @Override public Void visitMemberSelect(MemberSelectTree node, Void p) {
                            handleNode(node.getIdentifier(), getCurrentPath());
                            return super.visitMemberSelect(node, p);
                        }
                        @Override public Void visitNewClass(NewClassTree node, Void p) {
                            Name simpleName = null;
                            TreePath name = new TreePath(getCurrentPath(), node.getIdentifier());

                            OUTER: while (true) {
                                switch (name.getLeaf().getKind()) {
                                    case PARAMETERIZED_TYPE: name = new TreePath(name, ((ParameterizedTypeTree) name.getLeaf()).getType()); break;
                                    case MEMBER_SELECT: simpleName = ((MemberSelectTree) name.getLeaf()).getIdentifier(); break OUTER;
                                    case IDENTIFIER: simpleName = ((IdentifierTree) name.getLeaf()).getName(); break OUTER;
                                    default: name = getCurrentPath(); break OUTER;
                                }
                            }

                            handleNode(simpleName, name);
                            return super.visitNewClass(node, p);
                        }
                        private void handleNode(Name simpleName, TreePath toHighlight) {
                            if (!options.contains(RemoteUsages.SearchOptions.USAGES)) return;
                            Element el = parameter.getTrees().getElement(getCurrentPath());

                            if (el == null || el.asType().getKind() == TypeKind.ERROR) {
                                if (toFind.getSimpleName().equals(simpleName)) {
                                    success[0] = false;
                                    stop.set(true);
                                    return; //TODO: correct? what about the second pass?
                                }
                            }
                            if (RemoteUsages.equals(parameter, toFind, el)) {
                                toPopulate.add(new UsageTreeElementImpl(parameter, toHighlight, parent));
                            }
                        }
                        @Override
                        public Void visitMethod(MethodTree node, Void p) {
                            if (options.contains(RemoteUsages.SearchOptions.SUB) && toFind.getKind() == ElementKind.METHOD) {
                                boolean found = false;
                                Element el = parameter.getTrees().getElement(getCurrentPath());

                                if (el != null && el.getKind() == ElementKind.METHOD) {
                                    if (parameter.getElements().overrides((ExecutableElement) el, (ExecutableElement) toFind, (TypeElement) el.getEnclosingElement())) {
                                        toPopulate.add(new UsageTreeElementImpl(parameter, getCurrentPath(), parent));
                                        found = true;
                                    }
                                }

                                if (!found && el != null && el.getSimpleName().contentEquals(toFind.getSimpleName())) {
                                    for (TypeMirror sup : superTypes((TypeElement) el.getEnclosingElement())) {
                                        if (sup.getKind() == TypeKind.ERROR) {
                                            success[0] = false;
                                            stop.set(true);
                                            return null; //TODO: correct? what about the second pass?
                                        }
                                    }
                                }
                            }
                            return super.visitMethod(node, p);
                        }
                        @Override
                        public Void visitClass(ClassTree node, Void p) {
                            if (options.contains(RemoteUsages.SearchOptions.SUB) && (toFind.getKind().isClass() || toFind.getKind().isInterface())) {
                                Element el = parameter.getTrees().getElement(getCurrentPath());
                                boolean wasError = false;

                                for (TypeMirror sup : superTypes((TypeElement) el)) {
                                    if (sup.getKind() == TypeKind.ERROR) {
                                        wasError = true;
                                    } else {
                                        if (toFind.equals(parameter.getTypes().asElement(sup))) {
                                            wasError = false;
                                            toPopulate.add(new UsageTreeElementImpl(parameter, getCurrentPath(), parent));
                                            break;
                                        }
                                    }
                                }

                                if (wasError) {
                                    success[0] = false;
                                    stop.set(true);
                                    return null; //TODO: correct? what about the second pass?
                                }
                            }

                            return super.visitClass(node, p);
                        }
                        private Set<TypeMirror> superTypes(TypeElement type) {
                            Set<TypeMirror> result = new HashSet<TypeMirror>();
                            List<TypeMirror> todo = new LinkedList<TypeMirror>();

                            todo.add(type.asType());

                            while (!todo.isEmpty()) {
                                List<? extends TypeMirror> directSupertypes = parameter.getTypes().directSupertypes(todo.remove(0));

                                todo.addAll(directSupertypes);
                                result.addAll(directSupertypes);
                            }

                            return result;
                        }
                    }.scan(parameter.getCompilationUnit(), null);
                }
            }, true);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return success[0];
    }

    private static boolean equals(CompilationInfo info, Element toFind, Element what) {
        if (toFind == what) return true;
        if (what == null) return false;
        if (toFind.getKind() != what.getKind()) return false;
        if (toFind.getKind() != ElementKind.METHOD) return false;

        return info.getElements().overrides((ExecutableElement) what, (ExecutableElement) toFind, (TypeElement) what.getEnclosingElement());
    }

    private static final class UsageTreeElementImpl implements TreeElement {

        private final WhereUsedElement delegate;
        private final TreeElement parent;

        public UsageTreeElementImpl(CompilationInfo info, TreePath toHighlight, TreeElement parent) {
            delegate = WhereUsedElement.create(info, toHighlight, false);
            this.parent = parent;
        }

        @Override
        public TreeElement getParent(boolean isLogical) {
            return parent;
        }

        @Override
        public Icon getIcon() {
            return delegate.getLookup().lookup(Icon.class);
        }

        @Override
        public String getText(boolean isLogical) {
            return delegate.getDisplayText();
        }

        @Override
        public Object getUserObject() {
            return delegate;
        }

    }
}
