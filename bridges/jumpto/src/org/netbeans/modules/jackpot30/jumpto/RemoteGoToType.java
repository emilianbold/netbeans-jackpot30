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
package org.netbeans.modules.jackpot30.jumpto;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.swing.Icon;
import org.codeviation.pojson.Pojson;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ui.ElementIcons;
import org.netbeans.api.java.source.ui.ElementOpen;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.jackpot30.impl.WebUtilities;
import org.netbeans.modules.jackpot30.impl.indexing.RemoteIndex;
import org.netbeans.modules.java.source.ElementHandleAccessor;
import org.netbeans.spi.jumpto.type.TypeDescriptor;
import org.netbeans.spi.jumpto.type.TypeProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=TypeProvider.class)
public class RemoteGoToType implements TypeProvider {

    @Override
    public String name() {
        return "Jackpot 3.0 Remote Index Type Provider";
    }

    @Override
    public String getDisplayName() {
        return "Jackpot 3.0 Remote Index Type Provider";
    }

    @Override
    public void computeTypeNames(Context context, Result result) {
        for (RemoteIndex ri : RemoteIndex.loadIndices()) {
            try {
                URI resolved = new URI(ri.remote.toExternalForm() + "/type/search?path=" + WebUtilities.escapeForQuery(ri.remoteSegment) + "&prefix=" + WebUtilities.escapeForQuery(context.getText()));
                @SuppressWarnings("unchecked") //XXX: should not trust something got from the network!
                Map<String, List<String>> types = Pojson.load(LinkedHashMap.class, WebUtilities.requestStringResponse(resolved));

                for (Entry<String, List<String>> e : types.entrySet()) {
                    for (String binaryName : e.getValue()) {
                        result.addResult(new RemoteTypeDescriptor(ri, e.getKey(), binaryName));
                    }
                }
            } catch (URISyntaxException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public void cancel() {
    }

    @Override
    public void cleanup() {
    }

    private static final class RemoteTypeDescriptor extends TypeDescriptor {

        private final RemoteIndex origin;
        private final String relativePath;
        private final String binaryName;
        private final AtomicReference<FileObject> file = new AtomicReference<FileObject>();

        public RemoteTypeDescriptor(RemoteIndex origin, String relativePath, String binaryName) {
            this.origin = origin;
            this.relativePath = relativePath;
            this.binaryName = binaryName;
        }

        @Override
        public String getSimpleName() {
            int dollar = binaryName.lastIndexOf("$");
            
            if (dollar >= 0) return binaryName.substring(dollar + 1);
            else {
                int dot = binaryName.lastIndexOf(".");
                
                if (dot >= 0) return binaryName.substring(dot + 1);
                else return binaryName;
            }
        }

        @Override
        public String getOuterName() {
            int dollar = binaryName.lastIndexOf("$");
            int dot = binaryName.lastIndexOf(".");

            if (dollar >= 0 && dot >= 0) return binaryName.substring(dot + 1, dollar).replace("$", ".");
            else return null;
        }

        @Override
        public String getTypeName() {
            if (getOuterName() != null)
                return getSimpleName() + " in " + getOuterName();
            else
                return getSimpleName();
        }

        @Override
        public String getContextName() {
            int dot = binaryName.lastIndexOf(".");

            if (dot >= 0) return " (" + binaryName.substring(0, dot) + ")";
            else return "";
        }

        @Override
        public Icon getIcon() {
            return ElementIcons.getElementIcon(ElementKind.CLASS, EnumSet.noneOf(Modifier.class));
        }

        @Override
        public String getProjectName() {
            FileObject file = getFileObject();

            if (file == null) return null;

            Project prj = FileOwnerQuery.getOwner(file);

            if (prj == null) return null;

            return ProjectUtils.getInformation(prj).getDisplayName();
        }

        @Override
        public Icon getProjectIcon() {
            FileObject file = getFileObject();

            if (file == null) return null;

            Project prj = FileOwnerQuery.getOwner(file);

            if (prj == null) return null;

            return ProjectUtils.getInformation(prj).getIcon();
        }

        @Override
        public FileObject getFileObject() {
            FileObject f = this.file.get();

            if (f == null) {
                String fqn = binaryName;

                if (fqn.contains("$")) {
                    fqn = fqn.substring(0, fqn.indexOf("$"));
                }

                FileObject originFolder = FileUtil.toFileObject(FileUtil.normalizeFile(new File(origin.folder)));

                if (originFolder != null) f = originFolder.getFileObject(relativePath + "/" + fqn.replace('.', '/') + ".java");
                if (f != null) this.file.set(f);
            }

            return f;
        }

        @Override
        public int getOffset() {
            return 0;
        }

        @Override
        public void open() {
            FileObject file = getFileObject();

            if (file == null) return ; //XXX tell to the user

            ClasspathInfo cpInfo = ClasspathInfo.create(file);
            ElementHandle<?> handle = ElementHandleAccessor.INSTANCE.create(ElementKind.CLASS, binaryName);

            ElementOpen.open(cpInfo, handle);
        }

    }
}
