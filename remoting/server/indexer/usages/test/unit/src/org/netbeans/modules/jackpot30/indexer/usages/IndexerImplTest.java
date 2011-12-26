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
package org.netbeans.modules.jackpot30.indexer.usages;

import com.sun.source.util.TreePath;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.java.source.indexing.JavaCustomIndexer;
import org.netbeans.modules.java.source.parsing.JavacParser;
import org.netbeans.modules.java.source.parsing.JavacParserFactory;
import org.netbeans.modules.parsing.impl.indexing.CacheFolder;
import org.netbeans.modules.parsing.impl.indexing.MimeTypes;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.filesystems.MIMEResolver;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class IndexerImplTest extends NbTestCase {

    public IndexerImplTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        Set<String> mimeTypes = MimeTypes.getAllMimeTypes();
        if (mimeTypes == null) {
            mimeTypes = new HashSet<String>();
        } else {
            mimeTypes = new HashSet<String>(mimeTypes);
        }

        mimeTypes.add("text/x-java");
        MimeTypes.setAllMimeTypes(mimeTypes);
        
        org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        clearWorkDir();
        CacheFolder.setCacheFolder(FileUtil.toFileObject(getWorkDir()));
        super.setUp();
    }

    public void testMethodSignatures() throws IOException {
        doMethodSignatureTest("package test; public class Test { public void test() {} }", "()V;");
        doMethodSignatureTest("package test; public class Test { public <T extends String> void test(java.util.Map<java.util.List<String>, T> m, boolean p) {} }", "<T:Ljava/lang/String;>(Ljava/util/Map<Ljava/util/List<Ljava/lang/String;>;TT;>;Z)V;");
        doMethodSignatureTest("package test; public class Test <T extends String> { public void test(java.util.Map<java.util.List<String>, T> m, boolean p) {} }", "(Ljava/util/Map<Ljava/util/List<Ljava/lang/String;>;TT;>;Z)V;");
        doMethodSignatureTest("package test; public class Test { public void test() throws java.io.IOException {} }", "()V^Ljava/io/IOException;;");
        doMethodSignatureTest("package test; public class Test { public void test(java.util.List<? extends String> l) {} }", "(Ljava/util/List<+Ljava/lang/String;>;)V;");
    }
    
    protected void doMethodSignatureTest(String code, final String signature) throws IOException {
        FileObject testFile = FileUtil.createData(new File(getWorkDir(), "Test.java"));
        OutputStream out = testFile.getOutputStream();

        try {
            out.write(code.getBytes());
        } finally {
            out.close();
        }

        final boolean[] invoked = new boolean[1];

        JavaSource.forFileObject(testFile).runUserActionTask(new Task<CompilationController>() {
            @Override public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(JavaSource.Phase.RESOLVED);

                ExecutableElement method = ElementFilter.methodsIn(parameter.getTopLevelElements().get(0).getEnclosedElements()).iterator().next();

                assertEquals(signature, IndexerImpl.methodTypeSignature(parameter.getElements(), method));
                invoked[0] = true;
            }
        }, true);

        assertTrue(invoked[0]);
    }

    public void testOverriddenMethods() throws IOException {
        doOverriddenMethodsTest("package test; public class Test { public String toStr|ing() { return null; } }",
                                "METHOD:java.lang.Object:toString:()Ljava/lang/String;");
        doOverriddenMethodsTest("package test; public class Test extends A implements B { public void t|t() { } } class A implements B { public void tt() {} } interface B { public void tt(); }",
                                "METHOD:test.A:tt:()V",
                                "METHOD:test.B:tt:()V");
    }

    protected void doOverriddenMethodsTest(String code, final String... signature) throws IOException {
        final int pos = code.indexOf('|');

        code = code.replace("|", "");

        FileObject testFile = FileUtil.createData(new File(getWorkDir(), "Test.java"));
        OutputStream out = testFile.getOutputStream();

        try {
            out.write(code.getBytes());
        } finally {
            out.close();
        }

        final boolean[] invoked = new boolean[1];

        JavaSource.forFileObject(testFile).runUserActionTask(new Task<CompilationController>() {
            @Override public void run(CompilationController parameter) throws Exception {
                parameter.toPhase(JavaSource.Phase.RESOLVED);

                TreePath selected = parameter.getTreeUtilities().pathFor(pos);
                ExecutableElement method = (ExecutableElement) parameter.getTrees().getElement(selected);
                List<String> result = new ArrayList<String>();

                for (ExecutableElement ee : IndexerImpl.overrides(parameter.getTypes(), parameter.getElements(), method)) {
                    result.add(Common.serialize(ElementHandle.create(ee)));
                }

                assertEquals(Arrays.asList(signature), result);
                invoked[0] = true;
            }
        }, true);

        assertTrue(invoked[0]);
    }

    @ServiceProvider(service=MimeDataProvider.class)
    public static final class JavacParserProvider implements MimeDataProvider {

        private Lookup javaLookup = Lookups.fixed(new JavacParserFactory(), new JavaCustomIndexer.Factory());

        public Lookup getLookup(MimePath mimePath) {
            if (mimePath.getPath().endsWith(JavacParser.MIME_TYPE)) {
                return javaLookup;
            }

            return Lookup.EMPTY;
        }

    }

    @ServiceProvider(service=MIMEResolver.class)
    public static final class JavaMimeResolver extends MIMEResolver {

        public JavaMimeResolver() {
            super(JavacParser.MIME_TYPE);
        }

        @Override
        public String findMIMEType(FileObject fo) {
            if ("java".equals(fo.getExt())) {
                return JavacParser.MIME_TYPE;
            }

            return null;
        }

    }
}
