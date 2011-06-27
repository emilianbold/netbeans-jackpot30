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

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeOp;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;

/**
 *
 * @author lahvac
 */
public class Nodes {

    public static Node constructSemiLogicalView(Iterable<? extends FileObject> filesWithOccurrences) {
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
        }

        projects.remove(null);//XXX!!!XXX

        List<Node> nodes = new LinkedList<Node>();

        for (Project p : projects.keySet()) {
            nodes.add(constructSemiLogicalView(p, projects.get(p)));
        }

        return new AbstractNode(new DirectChildren(nodes));
    }

    private static Node constructSemiLogicalView(final Project p, Iterable<? extends FileObject> files) {
        LogicalViewProvider lvp = p.getLookup().lookup(LogicalViewProvider.class);
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

        Collection<Node> fileNodes = new ArrayList<Node>();

        for (FileObject file : files) {
            Node foundChild = locateChild(view, lvp, file);

            if (foundChild == null) {
                Node n = new AbstractNode(Children.LEAF) {
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

                return n;
            }

            fileNodes.add(foundChild);
        }

        return new Wrapper(view, fileNodes);
    }

    private static Node locateChild(Node parent, LogicalViewProvider lvp, FileObject file) {
        if (lvp != null) {
            return lvp.findPath(parent, file);
        }

        throw new UnsupportedOperationException("Not done yet");
    }

    private static class Wrapper extends FilterNode {

        public Wrapper(Node orig, Collection<? extends Node> fileNodes) {
            super(orig, new WrapperChildren(orig, fileNodes));
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
        private final Collection<? extends Node> fileNodes;

        public WrapperChildren(Node orig, Collection<? extends Node> fileNodes) {
            this.orig = orig;
            this.fileNodes = fileNodes;

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
                for (Node c : fileNodes) {
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
            if (fileNodes.contains(key)) {
                return new Node[] {new FilterNode(key)}; //XXX
            }
            return new Node[] {new Wrapper(key, fileNodes)};
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

}
