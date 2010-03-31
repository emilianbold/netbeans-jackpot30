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

package org.netbeans.modules.jackpot30.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.extensions.TestSetup;
import junit.framework.Test;
import org.netbeans.junit.NbTestSuite;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class CreateStandaloneJarTest extends HintsAnnotationProcessingTest {

    public CreateStandaloneJarTest(String name) {
        super(name);
    }

    private static File compiler;

    @Override
    protected void reallyRunCompiler(File source, File sourcePath, File sourceOutput) throws IOException, Exception {
        assertNotNull(compiler);
        try {
            Process p = Runtime.getRuntime().exec(new String[] {
                "java",
                "-Xbootclasspath/p:" + compiler.getAbsolutePath(),
                "com.sun.tools.javac.Main",
                source.getAbsolutePath(),
                "-sourcepath",
                sourcePath.getAbsolutePath(),
                "-s",
                sourceOutput.getAbsolutePath(),
                "-source",
                "1.5",
                "-Xjcov" //XXX
            });

            new CopyStream(p.getInputStream(), System.out).start();
            new CopyStream(p.getErrorStream(), System.err).start();

            assertEquals(0, p.waitFor());
        } catch (Throwable t) {
            throw new IOException(t);
        }
    }

    @Override
    public void testCodeAPI() throws Exception {
    }

    private static final class CopyStream extends Thread {
        private final InputStream ins;
        private final OutputStream out;

        public CopyStream(InputStream ins, OutputStream out) {
            this.ins = ins;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                FileUtil.copy(ins, out);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    ins.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

    }

    public static Test suite() {
        NbTestSuite suite = new NbTestSuite();
        
        suite.addTestSuite(CreateStandaloneJarTest.class);

        return new TestSetup(suite) {
            private File hintsList;
            protected void setUp() throws Exception {
                compiler = File.createTempFile("compiler", ".jar");
                hintsList = File.createTempFile("hints", "list");

//                if (!compiler.canRead()) {
                    CreateStandaloneJar.createCompiler(compiler, hintsList);
//                }
            }
            protected void tearDown() {
                compiler.delete();
                hintsList.delete();
                compiler = null;
                hintsList = null;
            }
        };
    }

}