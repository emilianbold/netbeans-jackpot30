/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.compiler;

import com.sun.tools.javac.Main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class HintsAnnotationProcessingTestBase extends NbTestCase {

    public HintsAnnotationProcessingTestBase(String name) {
        super(name);
    }

    protected File workDir;
    protected File src;
    protected File sourceOutput;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        clearWorkDir();

        workDir = getWorkDir();
        sourceOutput = new File(workDir, "src-out");
        sourceOutput.mkdirs();
        src = new File(workDir, "src");
        src.mkdirs();
    }

    protected void runCompiler(String... fileContentAndExtraOptions) throws Exception {
        List<String> fileAndContent = new LinkedList<String>();
        List<String> extraOptions = new LinkedList<String>();
        List<String> fileContentAndExtraOptionsList = Arrays.asList(fileContentAndExtraOptions);
        int nullPos = fileContentAndExtraOptionsList.indexOf(null);

        if (nullPos == (-1)) {
            fileAndContent = fileContentAndExtraOptionsList;
            extraOptions = Collections.emptyList();
        } else {
            fileAndContent = fileContentAndExtraOptionsList.subList(0, nullPos);
            extraOptions = fileContentAndExtraOptionsList.subList(nullPos + 1, fileContentAndExtraOptionsList.size());
        }

        assertTrue(fileAndContent.size() % 2 == 0);

        List<String> options = new LinkedList<String>();

        for (int cntr = 0; cntr < fileAndContent.size(); cntr += 2) {
            String file = createAndFill(fileAndContent.get(cntr), fileAndContent.get(cntr + 1)).getAbsolutePath();

            if (file.endsWith(".java")) {
                options.add(file);
            }
        }

        if (!extraOptions.contains("-sourcepath")) {
            options.add("-sourcepath");
            options.add(src.getAbsolutePath());
        }
        
        options.add("-s");
        options.add(sourceOutput.getAbsolutePath());
        options.add("-source");
        options.add("1.5");
        options.add("-Xjcov");
        options.addAll(extraOptions);

        reallyRunCompiler(workDir, options.toArray(new String[0]));
    }

    protected void reallyRunCompiler(File workDir, String... params) throws Exception {
        String javacJar = System.getProperty("test.javacJar");

        if (javacJar == null) {
            String oldUserDir = System.getProperty("user.dir");

            System.setProperty("user.dir", workDir.getAbsolutePath());

            try {
                assertEquals(0, Main.compile(params));
            } finally {
                System.setProperty("user.dir", oldUserDir);
            }
        } else {
            File compiler = new File(javacJar);

            assertTrue(compiler.exists());

            List<String> ll = new LinkedList<String>();

            ll.add("java");
            ll.add("-Xbootclasspath/p:" + compiler.getAbsolutePath());
            ll.add("com.sun.tools.javac.Main");
            ll.addAll(Arrays.asList(params));

            try {
                Process p = Runtime.getRuntime().exec(ll.toArray(new String[0]), null, workDir);

                new CopyStream(p.getInputStream(), System.out).start();
                new CopyStream(p.getErrorStream(), System.err).start();

                assertEquals(0, p.waitFor());
            } catch (Throwable t) {
                throw new IOException(t);
            }
        }
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

    private File createAndFill(String path, String content) throws IOException {
        File source = new File(workDir, path);

        source.getParentFile().mkdirs();
        
        Writer out = new OutputStreamWriter(new FileOutputStream(source));

        out.write(content);

        out.close();

        return source;
    }

    protected static String readFully(File file) throws IOException {
        if (!file.canRead()) return null;
        StringBuilder res = new StringBuilder();
        Reader in = new InputStreamReader(new FileInputStream(file));
        int read;
        
        while ((read = in.read()) != (-1)) {
            res.append((char) read);
        }

        return res.toString();
    }

}
