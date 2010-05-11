/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.spi.refactoring;

import java.io.IOException;
import java.util.Collections;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.SourceUtilsTestUtil;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.core.startup.Main;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.JavaDataLoader;
import org.netbeans.modules.java.source.indexing.JavaCustomIndexer;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.RepositoryUpdater;
import org.netbeans.modules.parsing.impl.indexing.Util;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class JackpotBasedRefactoringTestBase extends NbTestCase {

    public JackpotBasedRefactoringTestBase(String name) {
        super(name);
    }

    protected static void writeFilesAndWaitForScan(FileObject sourceRoot, File... files) throws Exception {
        for (FileObject c : sourceRoot.getChildren()) {
            c.delete();
        }

        for (File f : files) {
            FileObject fo = FileUtil.createData(sourceRoot, f.filename);
            TestUtilities.copyStringToFile(fo, f.content);
        }

        SourceUtils.waitScanFinished();
    }

    protected static void assertContent(FileObject sourceRoot, File... files) throws Exception {
        for (File f : files) {
            FileObject fo = sourceRoot.getFileObject(f.filename);

            assertNotNull(f.filename, fo);

            assertEquals(f.content.replaceAll("[ \t\n]+", " "), TestUtilities.copyFileToString(FileUtil.toFile(fo)).replaceAll("[ \t\n]+", " "));
        }
    }

    protected static final class File {
        public final String filename;
        public final String content;

        public File(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }
    }

    protected FileObject src;
    protected Project prj;

    @Override
    protected void setUp() throws Exception {
        SourceUtilsTestUtil.prepareTest(new String[] {"org/netbeans/modules/java/editor/resources/layer.xml"}, new Object[] {
            new ClassPathProvider() {
                public ClassPath findClassPath(FileObject file, String type) {
                    if (src != null && (file == src || FileUtil.isParentOf(src, file))) {
                        if (ClassPath.BOOT.equals(type)) {
                            return ClassPathSupport.createClassPath(System.getProperty("sun.boot.class.path"));
                        }
                        if (ClassPath.COMPILE.equals(type)) {
                            return ClassPathSupport.createClassPath(new FileObject[0]);
                        }
                        if (ClassPath.SOURCE.equals(type)) {
                            return ClassPathSupport.createClassPath(src);
                        }
                    }

                    return null;
                }
            },
            new ProjectFactory() {
            public boolean isProject(FileObject projectDirectory) {
                return src == projectDirectory;
            }
            public Project loadProject(final FileObject projectDirectory, ProjectState state) throws IOException {
                if (!isProject(projectDirectory)) return null;
                return new Project() {
                    public FileObject getProjectDirectory() {
                        return projectDirectory;
                    }
                    public Lookup getLookup() {
                        return Lookup.EMPTY;
                    }
                };
            }
            public void saveProject(Project project) throws IOException, ClassCastException {}
        },
        JavaDataLoader.getLoader(JavaDataLoader.class),
        });
        Main.initializeURLFactory();
        org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        prepareTest();
        org.netbeans.api.project.ui.OpenProjects.getDefault().open(new Project[] {prj = ProjectManager.getDefault().findProject(src)}, false);
        Util.allMimeTypes = Collections.singleton("text/x-java");
        GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, new ClassPath[] {ClassPathSupport.createClassPath(src)});
        RepositoryUpdater.getDefault().start(true);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        org.netbeans.api.project.ui.OpenProjects.getDefault().open(new Project[] {prj}, false);
        prj = null;
    }

    private void prepareTest() throws Exception {
        FileObject workdir = SourceUtilsTestUtil.makeScratchDir(this);

        src = FileUtil.createFolder(workdir, "src");

        FileObject cache = FileUtil.createFolder(workdir, "cache");

        CacheFolder.setCacheFolder(cache);
    }

    @ServiceProvider(service=MimeDataProvider.class)
    public static final class MimeDataProviderImpl implements MimeDataProvider {

        private static final Lookup L = Lookups.singleton(new JavaCustomIndexer.Factory());

        public Lookup getLookup(MimePath mimePath) {
            if ("text/x-java".equals(mimePath.getPath())) {
                return L;
            }

            return null;
        }

    }

}
