/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008-2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.ide.usages;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.swing.Action;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.UiUtils;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.actions.OpenAction;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.LineCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOp;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author lahvac
 */
public class Nodes {

    public static Node constructSemiLogicalView(Iterable<? extends FileObject> filesWithOccurrences, ElementHandle<?> eh) {
        Map<Project, Collection<FileObject>> projects = new HashMap<Project, Collection<FileObject>>();

        for (FileObject file : filesWithOccurrences) {
            Project project = FileOwnerQuery.getOwner(file);

            if (project == null) {
                Logger.getLogger(Nodes.class.getName()).log(Level.WARNING, "Cannot find project for: {0}", FileUtil.getFileDisplayName(file));
            }

            Collection<FileObject> projectFiles = projects.get(project);

            if (projectFiles == null) {
                projects.put(project, projectFiles = new ArrayList<FileObject>());
            }

            projectFiles.add(file);

            //XXX: workarounding NbProject's Evaluator, which is too stupid to fire meaningfull property events, which leads to PackageView rebuilding inadvertedly itself due to virtual CONTAINERSHIP change:
            ClassPath.getClassPath(file, ClassPath.COMPILE).getRoots();
        }

        projects.remove(null);//XXX!!!XXX

        List<Node> nodes = new LinkedList<Node>();

        for (Project p : projects.keySet()) {
            nodes.add(constructSemiLogicalView(p, projects.get(p), eh));
        }

        return new AbstractNode(new DirectChildren(nodes));
    }

    private static Node constructSemiLogicalView(final Project p, final Iterable<? extends FileObject> files, ElementHandle<?> eh) {
        final LogicalViewProvider lvp = p.getLookup().lookup(LogicalViewProvider.class);
        final Node view;

        if (lvp != null) {
            view = lvp.createLogicalView();
        } else {
            try {
                view = DataObject.find(p.getProjectDirectory()).getNodeDelegate();
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
                return new AbstractNode(Children.LEAF);
            }
        }

        return new Wrapper(view, new ComputeNodes(files, view, lvp, p), eh);
    }

    private static Node locateChild(Node parent, LogicalViewProvider lvp, FileObject file) {
        if (lvp != null) {
            return lvp.findPath(parent, file);
        }

        throw new UnsupportedOperationException("Not done yet");
    }

    private static class Wrapper extends FilterNode {

        public Wrapper(Node orig, ComputeNodes fileNodes, ElementHandle<?> eh) {
            super(orig, new WrapperChildren(orig, fileNodes, eh));
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[0];
        }

    }

    private static boolean isParent(Node parent, Node child) {
        if (NodeOp.isSon(parent, child)) {
            return true;
        }

        Node p = child.getParentNode();

        if (p == null) {
            return false;
        }

        return isParent(parent, p);
    }

    private static class WrapperChildren extends Children.Keys<Node> {

        private final Node orig;
        private final ComputeNodes fileNodes;
        private final ElementHandle<?> eh;

        public WrapperChildren(Node orig, ComputeNodes fileNodes, ElementHandle<?> eh) {
            this.orig = orig;
            this.fileNodes = fileNodes;
            this.eh = eh;

        }

        @Override
        protected void addNotify() {
            super.addNotify();
            doSetKeys();
        }

        private void doSetKeys() {
            Node[] nodes = orig.getChildren().getNodes(true);
            List<Node> toSet = new LinkedList<Node>();

            OUTER: for (Node n : nodes) {
                for (Node c : fileNodes.compute()) {
                    if (n == c || isParent(n, c)) {
                        toSet.add(n);
                        continue OUTER;
                    }
                }
            }

            setKeys(toSet);
        }

        @Override
        protected Node[] createNodes(Node key) {
            if (fileNodes.compute().contains(key)) {
                FileObject file = key.getLookup().lookup(FileObject.class);
                Children c = file != null ? Children.create(new UsagesChildren(file, eh), true) : Children.LEAF;
                
                return new Node[] {new FilterNode(key, c)}; //XXX
            }
            return new Node[] {new Wrapper(key, fileNodes, eh)};
        }

    }

    private static final class DirectChildren extends Children.Keys<Node> {

        public DirectChildren(Collection<Node> nodes) {
            setKeys(nodes);
        }

        @Override
        protected Node[] createNodes(Node key) {
            return new Node[] {key};
        }
    }

    private static Node noOccurrencesNode() {
        AbstractNode noOccurrences = new AbstractNode(Children.LEAF);

        noOccurrences.setDisplayName("No Occurrences Found");

        return noOccurrences;
    }
    
    private static final class UsagesChildren extends ChildFactory<Node> {

        private final FileObject file;
        private final ElementHandle<?> eh;

        public UsagesChildren(FileObject file, ElementHandle<?> eh) {
            this.file = file;
            this.eh = eh;
        }

        @Override
        protected boolean createKeys(final List<Node> toPopulate) {
            List<Node> result = new ArrayList<Node>();

            if (!computeOccurrences(file, eh, result)) {
                result.clear();

                ClassPath source = ClassPath.getClassPath(file, ClassPath.SOURCE);

                GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {source});

                try {
                    SourceUtils.waitScanFinished();
                    computeOccurrences(file, eh, result);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, new ClassPath[] {source});
                }
            }

            toPopulate.addAll(result);
            
            if (toPopulate.isEmpty()) toPopulate.add(noOccurrencesNode());

            return true;
        }

        @Override
        protected Node createNodeForKey(Node key) {
            return key;
        }

    }

    static boolean computeOccurrences(FileObject file, final ElementHandle<?> eh, final List<Node> toPopulate) {
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
                            handleNode(node.getName(), getCurrentPath().getLeaf());
                            return super.visitIdentifier(node, p);
                        }
                        @Override public Void visitMemberSelect(MemberSelectTree node, Void p) {
                            handleNode(node.getIdentifier(), getCurrentPath().getLeaf());
                            return super.visitMemberSelect(node, p);
                        }
                        @Override public Void visitNewClass(NewClassTree node, Void p) {
                            Name simpleName = null;
                            Tree name = node.getIdentifier();

                            OUTER: while (true) {
                                switch (name.getKind()) {
                                    case PARAMETERIZED_TYPE: name = ((ParameterizedTypeTree) name).getType(); break;
                                    case MEMBER_SELECT: simpleName = ((MemberSelectTree) name).getIdentifier(); break OUTER;
                                    case IDENTIFIER: simpleName = ((IdentifierTree) name).getName(); break OUTER;
                                    default: name = node; break OUTER;
                                }
                            }

                            handleNode(simpleName, name);
                            return super.visitNewClass(node, p);
                        }
                        private void handleNode(Name simpleName, Tree toHighlight) {
                            Element el = parameter.getTrees().getElement(getCurrentPath());

                            if (el == null || el.asType().getKind() == TypeKind.ERROR) {
                                if (toFind.getSimpleName().equals(simpleName)) {
                                    success[0] = false;
                                    stop.set(true);
                                    return; //TODO: should stop the computation altogether
                                }
                            }
                            if (Nodes.equals(parameter, toFind, el)) {
                                toPopulate.add(new OccurrenceNode(parameter, toHighlight));
                            }
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

    private static final class OccurrenceNode extends AbstractNode {
        private final FileObject file;
        private final int pos;
        private final String htmlDisplayName;

        public OccurrenceNode(CompilationInfo info, Tree occurrence) {
            this(info, occurrence, new InstanceContent());
        }

        private OccurrenceNode(CompilationInfo info, Tree occurrence, InstanceContent content) {
            super(Children.LEAF, new AbstractLookup(content));

            int[] span;

            switch (occurrence.getKind()) {
                case MEMBER_SELECT: span = info.getTreeUtilities().findNameSpan((MemberSelectTree) occurrence); break;
                default:
                    SourcePositions sp = info.getTrees().getSourcePositions();

                    span = new int[] {(int) sp.getStartPosition(info.getCompilationUnit(), occurrence),
                                      (int) sp.getEndPosition(info.getCompilationUnit(), occurrence)};
                    break;
            }

            long startLine = info.getCompilationUnit().getLineMap().getLineNumber(span[0]);
            long startLineStart = info.getCompilationUnit().getLineMap().getStartPosition(startLine);

            String dn;

            try {
                DataObject od = DataObject.find(info.getFileObject());
                LineCookie lc = od.getLookup().lookup(LineCookie.class);
                Line l = lc.getLineSet().getCurrent((int) startLine - 1);
                od.getLookup().lookup(EditorCookie.class).openDocument();
                String line = l.getText();
                int endOnLine = (int) Math.min(line.length(), span[1] - startLineStart);

                dn = translate(line.substring(0, (int) (span[0] - startLineStart))) + "<b>" + translate(line.substring((int) (span[0] - startLineStart), endOnLine)) + "</b>" + translate(line.substring(endOnLine));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                dn = "Occurrence";
            }

            this.htmlDisplayName = dn;
            this.file = info.getFileObject();
            this.pos = span[0];
            
            content.add(new OpenCookie() {
                @Override public void open() {
                    UiUtils.open(file, pos);
                }
            });
        }

        @Override
        public String getHtmlDisplayName() {
            return htmlDisplayName;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[] {
                OpenAction.get(OpenAction.class)
            };
        }

        @Override
        public Action getPreferredAction() {
            return OpenAction.get(OpenAction.class);
        }

    }

    private static String[] c = new String[] {"&", "<", ">", "\n", "\""}; // NOI18N
    private static String[] tags = new String[] {"&amp;", "&lt;", "&gt;", "<br>", "&quot;"}; // NOI18N

    private static String translate(String input) {
        for (int cntr = 0; cntr < c.length; cntr++) {
            input = input.replaceAll(c[cntr], tags[cntr]);
        }

        return input;
    }

    private static class ComputeNodes  {

        private final Iterable<? extends FileObject> files;
        private final Node view;
        private final LogicalViewProvider lvp;
        private final Project p;

        public ComputeNodes(Iterable<? extends FileObject> files, Node view, LogicalViewProvider lvp, Project p) {
            this.files = files;
            this.view = view;
            this.lvp = lvp;
            this.p = p;
        }
        
        private Collection<Node> result;

        public synchronized Collection<Node> compute() {
            if (result != null) return result;

            Collection<Node> fileNodes = new ArrayList<Node>();

            for (FileObject file : files) {
                Node foundChild = locateChild(view, lvp, file);

                if (foundChild == null) {
                    foundChild = new AbstractNode(Children.LEAF) {
                        @Override
                        public Image getIcon(int type) {
                            return ImageUtilities.icon2Image(ProjectUtils.getInformation(p).getIcon());
                        }
                        @Override
                        public Image getOpenedIcon(int type) {
                            return getIcon(type);
                        }
                        @Override
                        public String getHtmlDisplayName() {
                            return view.getHtmlDisplayName() != null ? NbBundle.getMessage(Nodes.class, "ERR_ProjectNotSupported", view.getHtmlDisplayName()) : null;
                        }
                        @Override
                        public String getDisplayName() {
                            return NbBundle.getMessage(Nodes.class, "ERR_ProjectNotSupported", view.getDisplayName());
                        }
                    };
                }

                fileNodes.add(foundChild);
            }

            return result = fileNodes;
        }
    }
}
