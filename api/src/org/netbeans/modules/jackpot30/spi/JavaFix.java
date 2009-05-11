/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.spi;

import com.sun.javadoc.Tag;
import java.io.IOException;
import java.util.regex.Matcher;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.TypeMirrorHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.apisupport.project.NbModuleProject;
import org.netbeans.modules.apisupport.project.ProjectXMLManager;
import org.netbeans.modules.apisupport.project.spi.NbModuleProvider;
import org.netbeans.modules.apisupport.project.ui.customizer.ModuleDependency;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.batch.JavaFixImpl;
import org.netbeans.modules.jackpot30.impl.pm.Pattern;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.filesystems.FileObject;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 *
 * @author Jan Lahoda
 */
public abstract class JavaFix {

    private final TreePathHandle handle;

    protected JavaFix(CompilationInfo info, TreePath tp) {
        this.handle = TreePathHandle.create(tp, info);
    }

    protected abstract String getText();

    protected abstract void performRewrite(WorkingCopy wc, TreePath tp, UpgradeUICallback callback);

    final ChangeInfo process(WorkingCopy wc, UpgradeUICallback callback) throws Exception {
        TreePath tp = handle.resolve(wc);

        if (tp == null) {
            Logger.getLogger(JavaFix.class.getName()).log(Level.SEVERE, "Cannot resolve handle={0}", handle);
            return null;
        }

        performRewrite(wc, tp, callback);

        return null;
    }

    final FileObject getFile() {
        return handle.getFileObject();
    }

    public static Fix rewriteFix(CompilationInfo info, final String displayName, TreePath what, final String to, Map<String, TreePath> parameters, Map<String, TypeMirror> constraints) {
        final Map<String, TreePathHandle> params = new HashMap<String, TreePathHandle>();

        for (Entry<String, TreePath> e : parameters.entrySet()) {
            params.put(e.getKey(), TreePathHandle.create(e.getValue(), info));
        }

        final Map<String, TypeMirrorHandle> constraintsHandles = new HashMap<String, TypeMirrorHandle>();

        for (Entry<String, TypeMirror> c : constraints.entrySet()) {
            constraintsHandles.put(c.getKey(), TypeMirrorHandle.create(c.getValue()));
        }

        return toEditorFix(new JavaFix(info, what) {
            @Override
            protected String getText() {
                return displayName;
            }
            @Override
            protected void performRewrite(final WorkingCopy wc, TreePath tp, final UpgradeUICallback callback) {
                final Map<String, TreePath> parameters = new HashMap<String, TreePath>();

                for (Entry<String, TreePathHandle> e : params.entrySet()) {
                    TreePath p = e.getValue().resolve(wc);

                    if (tp == null) {
                        Logger.getLogger(JavaFix.class.getName()).log(Level.SEVERE, "Cannot resolve handle={0}", e.getValue());
                    }

                    parameters.put(e.getKey(), p);
                }

                Map<String, TypeMirror> constraints = new HashMap<String, TypeMirror>();

                for (Entry<String, TypeMirrorHandle> c : constraintsHandles.entrySet()) {
                    constraints.put(c.getKey(), c.getValue().resolve(wc));
                }

                Tree parsed = Pattern.parseAndAttribute(wc, to, constraints, new Scope[1]);

                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitIdentifier(IdentifierTree node, Void p) {
                        TreePath tp = parameters.get(node.getName().toString());

                        if (tp != null) {
                            wc.rewrite(node, tp.getLeaf());
                            return null;
                        }

                        return super.visitIdentifier(node, p);
                    }
                    @Override
                    public Void visitMemberSelect(MemberSelectTree node, Void p) {
                        Element e = wc.getTrees().getElement(getCurrentPath());

                        if (e == null || (e.getKind() == ElementKind.CLASS && ((TypeElement) e).asType().getKind() == TypeKind.ERROR)) {
                            if (node.getExpression().getKind() == Kind.IDENTIFIER) {
                                String name = ((IdentifierTree) node.getExpression()).getName().toString();

                                if (name.startsWith("$") && parameters.get(name) == null) {
                                    //XXX: unbound variable, use identifier instead of member select - may cause problems?
                                    wc.rewrite(node, wc.getTreeMaker().Identifier(node.getIdentifier()));
                                    return null;
                                }
                            }

                            return super.visitMemberSelect(node, p);
                        }

                        //check correct dependency:
                        checkDependency(wc, e, callback);
                        
                        if (Utilities.isPureMemberSelect(node, false)) {
                            wc.rewrite(node, wc.getTreeMaker().QualIdent(e));

                            return null;
                        } else {
                            return super.visitMemberSelect(node, p);
                        }
                    }

                }.scan(new TreePath(new TreePath(tp.getCompilationUnit()), parsed), null);

                wc.rewrite(tp.getLeaf(), parsed);
            }
        });
    }

    private static void checkDependency(WorkingCopy copy, Element e, UpgradeUICallback callback) {
        SpecificationVersion sv = computeSpecVersion(copy, e);

        if (sv == null) {
            return ;
        }

        Project currentProject = FileOwnerQuery.getOwner(copy.getFileObject());

        if (currentProject == null) {
            return ;
        }

        FileObject file = getFile(copy, e);

        if (file == null) {
            return ;
        }

        Project referedProject = FileOwnerQuery.getOwner(file);

        if (referedProject == null || currentProject.getProjectDirectory().equals(referedProject.getProjectDirectory())) {
            return ;
        }

        resolveNbModuleDependencies(currentProject, referedProject, sv, callback);
    }

    private static java.util.regex.Pattern SPEC_VERSION = java.util.regex.Pattern.compile("[0-9]+(\\.[0-9]+)+");
    
    static SpecificationVersion computeSpecVersion(CompilationInfo info, Element el) {
        for (Tag since : info.getElementUtilities().javaDocFor(el).tags("@since")) {
            String text = since.text();

            Matcher m = SPEC_VERSION.matcher(text);

            if (!m.find()) {
                continue;
            }

            return new SpecificationVersion(m.group()/*ver.toString()*/);
        }

        return null;
    }
    
    public static Fix toEditorFix(final JavaFix jf) {
        return new JavaFixImpl(jf);
    }

    private static void resolveNbModuleDependencies(Project currentProject, Project referedProject, SpecificationVersion sv, UpgradeUICallback callback) throws IllegalArgumentException {
        NbModuleProvider currentNbModule = currentProject.getLookup().lookup(NbModuleProvider.class);

        if (currentNbModule == null) {
            return ;
        }

        NbModuleProvider referedNbModule = referedProject.getLookup().lookup(NbModuleProvider.class);

        if (referedNbModule == null) {
            return ;
        }

        try {
            NbModuleProject currentNbModuleProject = currentProject.getLookup().lookup(NbModuleProject.class);

            if (currentNbModuleProject == null) {
                return ;
            }
            
            ProjectXMLManager m = new ProjectXMLManager(currentNbModuleProject);
            ModuleDependency dep = null;

            for (ModuleDependency md : m.getDirectDependencies()) {
                if (referedNbModule.getCodeNameBase().equals(md.getModuleEntry().getCodeNameBase())) {
                    dep = md;
                    break;
                }
            }

            if (dep == null) {
                return ;
            }

            SpecificationVersion currentDep = new SpecificationVersion(dep.getSpecificationVersion());

            if (currentDep == null || currentDep.compareTo(sv) < 0) {
                String upgradeText = NbBundle.getMessage(JavaFix.class,
                                                         "LBL_UpdateDependencyQuestion",
                                                         new Object[] {
                                                            ProjectUtils.getInformation(referedProject).getDisplayName(),
                                                            currentDep.toString()
                                                         });

                if (callback.shouldUpgrade(upgradeText)) {
                    ModuleDependency nue = new ModuleDependency(dep.getModuleEntry(),
                                                                dep.getReleaseVersion(),
                                                                sv.toString(),
                                                                dep.hasCompileDependency(),
                                                                dep.hasImplementationDepedendency());
                    
                    m.editDependency(dep, nue);
                    ProjectManager.getDefault().saveProject(currentProject);
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @SuppressWarnings("deprecation")
    private static FileObject getFile(WorkingCopy copy, Element e) {
        return SourceUtils.getFile(e, copy.getClasspathInfo());
    }

    public interface UpgradeUICallback {
        public boolean shouldUpgrade(String comment);
    }

    static {
        JavaFixImpl.Accessor.INSTANCE = new JavaFixImpl.Accessor() {
            @Override
            public String getText(JavaFix jf) {
                return jf.getText();
            }
            @Override
            public ChangeInfo process(JavaFix jf, WorkingCopy wc, UpgradeUICallback callback) throws Exception {
                return jf.process(wc, callback);
            }
            @Override
            public FileObject getFile(JavaFix jf) {
                return jf.getFile();
            }
        };
    }
    
}
