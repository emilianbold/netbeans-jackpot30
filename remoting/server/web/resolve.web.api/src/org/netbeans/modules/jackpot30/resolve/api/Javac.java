/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.resolve.api;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.netbeans.modules.jackpot30.backend.base.SourceRoot;

/**
 *
 * @author lahvac
 */
public class Javac {
    private static final Map<SourceRoot, Reference<Javac>> category2Javac = new WeakHashMap<SourceRoot, Reference<Javac>>();

    static synchronized Javac get(SourceRoot sourceRoot) {
        Reference<Javac> javacSR = category2Javac.get(sourceRoot);
        Javac javac = javacSR != null ? javacSR.get() : null;

        if (javac == null) {
            category2Javac.put(sourceRoot, new SoftReference<Javac>(javac = new Javac(sourceRoot)));
        }

        return javac;
    }

    private final SourceRoot sourceRoot;
    private final JavacTaskImpl javacTask;
    private final Map<String, CompilationInfo> path2CUT = new HashMap<String, CompilationInfo>();

    private Javac(SourceRoot sourceRoot) {
        this.sourceRoot = sourceRoot;
        FMImpl fm = new FMImpl(sourceRoot.getClassPath());
        this.javacTask = (JavacTaskImpl) ToolProvider.getSystemJavaCompiler().getTask(null, fm, null, Arrays.asList("-Xjcov", "-proc:none"), null, Collections.<JavaFileObject>emptyList());
    }


    public CompilationInfo parse(String relativePath) throws IOException, InterruptedException {
        CompilationInfo result = path2CUT.get(relativePath);

        if (result == null) {
            String content = org.netbeans.modules.jackpot30.source.api.API.readFileContent(sourceRoot.getCategory(), relativePath);
            Iterable<? extends CompilationUnitTree> cuts = javacTask.parse(new FileObjectImpl(relativePath, content));
            CompilationUnitTree cut = cuts.iterator().next();

            javacTask.analyze(javacTask.enter(Collections.singletonList(cut)));

            path2CUT.put(relativePath, result = new CompilationInfo(this, cut));
        }

        return result;
    }

    public JavacTask getTask() {
        return javacTask;
    }

    private static class FileObjectImpl extends SimpleJavaFileObject {
        private String text;
        public FileObjectImpl(String relativePath, String text) {
            super(URI.create("myfo:/" + relativePath), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

    private static final class FMImpl implements JavaFileManager {

        private final Collection<org.openide.filesystems.FileObject> classpath;
        private final Map<org.openide.filesystems.FileObject, JFOImpl> files = new HashMap<org.openide.filesystems.FileObject, JFOImpl>();

        public FMImpl(Collection<org.openide.filesystems.FileObject> classpath) {
            this.classpath = classpath;
        }

        @Override
        public ClassLoader getClassLoader(Location location) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
            assert !recurse;
            if (!kinds.contains(Kind.CLASS) || location != StandardLocation.CLASS_PATH) return Collections.emptyList();

            String dir = packageName.replace('.', '/');
            List<JavaFileObject> result = new ArrayList<JavaFileObject>();

            for (org.openide.filesystems.FileObject root : classpath) {
                org.openide.filesystems.FileObject dirFO = root.getFileObject(dir);

                if (dirFO != null) {
                    for (org.openide.filesystems.FileObject  f : dirFO.getChildren()) {
                        if (!f.isData()) continue;

                        JFOImpl jfo = files.get(f);

                        if (jfo == null) {
                            files.put(f, jfo = new JFOImpl(f, packageName + "." + f.getName()));
                        }

                        result.add(jfo);
                    }
                }
            }

            return result;
        }

        @Override
        public String inferBinaryName(Location location, JavaFileObject file) {
            return ((JFOImpl) file).binaryName;
        }

        @Override
        public boolean isSameFile(FileObject a, FileObject b) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean handleOption(String current, Iterator<String> remaining) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean hasLocation(Location location) {
            return StandardLocation.CLASS_PATH == location;
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void flush() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void close() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int isSupportedOption(String option) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static final class JFOImpl implements JavaFileObject {

        private final org.openide.filesystems.FileObject file;
        private final String binaryName;

        public JFOImpl(org.openide.filesystems.FileObject file, String binaryName) {
            this.file = file;
            this.binaryName = binaryName;
        }

        @Override
        public Kind getKind() {
            return Kind.CLASS;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public NestingKind getNestingKind() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Modifier getAccessLevel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public URI toUri() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getName() {
            return file.getName();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            return file.getInputStream();
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Writer openWriter() throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public long getLastModified() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean delete() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
