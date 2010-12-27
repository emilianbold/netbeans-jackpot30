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

package org.netbeans.modules.jackpot30.hudson;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.scm.SubversionSCM;
import hudson.scm.SubversionSCM.ModuleLocation;
import hudson.util.ArgumentListBuilder;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.jvnet.hudson.test.HudsonHomeLoader;
import org.jvnet.hudson.test.HudsonTestCase;
import static org.junit.Assert.*;
import org.netbeans.api.jackpot.hudson.IndexBuilder;
import org.xml.sax.SAXException;

/**
 *
 * @author lahvac
 */
public class IndexingBuilderTest extends HudsonTestCase {

    public IndexingBuilderTest() {
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        checkoutDir = HudsonHomeLoader.NEW.allocate();
        repositoryDir = HudsonHomeLoader.NEW.allocate();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        //XXX: some of the working directories seems to be kept by the testing infra, workarounding:
        new FilePath(checkoutDir).deleteRecursive();
        new FilePath(repositoryDir).deleteRecursive();
        hudson.getRootPath().deleteRecursive();
    }

    private File checkoutDir;
    private File repositoryDir;

    public void testUpdate() throws Exception {
        //setup svn repository:
        String repositoryURL = repositoryDir.toURI().toURL().toExternalForm().replace("file:/", "file:///");
        runSubversionAdmin("create", repositoryDir.getAbsolutePath());
        runSubversion("checkout", repositoryURL, ".");
        createFile("A.java");
        createFile("B.java");
        runSubversion("add", "A.java", "B.java");
        runSubversion("commit", "-m", "initial");

        FreeStyleProject p = createFreeStyleProject();
        ModuleLocation mod1 = new ModuleLocation(repositoryURL, null);
        SubversionSCM scm = new SubversionSCM(Collections.singletonList(mod1), true, null, "", "", "");
        IndexBuilderImpl indexer = new IndexBuilderImpl();
        p.setScm(scm);
        p.getBuildersList().add(new IndexingBuilder(contextPath, Collections.<IndexBuilder>singleton(indexer)));

        doRunProject(p);

        assertTrue(indexer.called);

        runSubversion("remove", "B.java");
        createFile("C.java");
        runSubversion("add", "C.java");
        runSubversion("commit", "-m", "");

        indexer.called = false;
        doRunProject(p);

        assertTrue(indexer.called);
        assertEquals(Collections.singleton("C.java"), indexer.addedOrModified);
        assertEquals(Collections.singleton("B.java"), indexer.removed);
    }

    public void DISABLEDtestCheckoutIntoSpecifiedDir() throws Exception {
        //setup svn repository:
        String repositoryURL = repositoryDir.toURI().toURL().toExternalForm().replace("file:/", "file:///");
        runSubversionAdmin("create", repositoryDir.getAbsolutePath());
        runSubversion("checkout", repositoryURL, ".");
        createFile("A.java");
        createFile("B.java");
        runSubversion("add", "A.java", "B.java");
        runSubversion("commit", "-m", "initial");

        FreeStyleProject p = createFreeStyleProject();
        ModuleLocation mod1 = new ModuleLocation(repositoryURL, "repo1");
        SubversionSCM scm = new SubversionSCM(Collections.singletonList(mod1), true, null, "", "", "");
        IndexBuilderImpl indexer = new IndexBuilderImpl();
        p.setScm(scm);
        p.getBuildersList().add(new IndexingBuilder(contextPath, Collections.<IndexBuilder>singleton(indexer)));

        doRunProject(p);

        assertTrue(indexer.called);

        runSubversion("remove", "B.java");
        createFile("C.java");
        runSubversion("add", "C.java");
        runSubversion("commit", "-m", "");

        indexer.called = false;
        doRunProject(p);

        assertTrue(indexer.called);
        assertEquals(Arrays.asList(""), indexer.addedOrModified);
        assertEquals(Arrays.asList(""), indexer.removed);
    }

    private void doRunProject(FreeStyleProject p) throws SAXException, IOException, InterruptedException {
        WebClient w = new WebClient();
        w.getPage(p, "build?delay=0sec");

        Thread.sleep(5000);

        while (p.isBuilding()) {
            Thread.sleep(100);
        }
    }

    private void runSubversion(String... args) throws IOException, InterruptedException {
        Launcher.LocalLauncher l = new Launcher.LocalLauncher(new StreamTaskListener(System.err));
        l.launch().cmds(new ArgumentListBuilder().add("svn").add(args))
                  .pwd(checkoutDir)
                  .start()
                  .join();
    }

    private void runSubversionAdmin(String... args) throws IOException, InterruptedException {
        Launcher.LocalLauncher l = new Launcher.LocalLauncher(new StreamTaskListener(System.err));
        l.launch().cmds(new ArgumentListBuilder().add("svnadmin").add(args))
                  .pwd(repositoryDir)
                  .start()
                  .join();
    }

    private void createFile(String relativePath) throws IOException {
        File toCreate = new File(checkoutDir, relativePath.replace('/', File.separatorChar));

        toCreate.getParentFile().mkdirs();

        new FileOutputStream(toCreate).close();
    }

    private static final class IndexBuilderImpl extends IndexBuilder {

        private boolean called;
        private Set<String> addedOrModified;
        private Set<String> removed;

        @Override
        public boolean index(IndexingContext context) throws InterruptedException, IOException {
            addedOrModified = context.addedOrModified;
            removed = context.removed;
            called = true;
            return true;
        }

    }

}