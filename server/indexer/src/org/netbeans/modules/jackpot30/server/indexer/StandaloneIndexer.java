/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.server.indexer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesIndex;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.indexing.Index.IndexWriter;

/**
 *
 * @author lahvac
 */
public class StandaloneIndexer {

    public static void index(File root, boolean duplicatesIndex) throws IOException {
        IndexWriter w = Index.get(root.toURI().toURL()).openForWriting();
        DuplicatesIndex.IndexWriter dw = duplicatesIndex ? DuplicatesIndex.get(root.toURI().toURL()).openForWriting() : null;

        try {
            new StandaloneIndexer().doIndex(w, dw, root);
        } finally {
            w.close();
        }
    }

    private void doIndex(IndexWriter w, DuplicatesIndex.IndexWriter dw, File fileOrDir) throws IOException {
        if (fileOrDir.isDirectory()) {
            for (File f : fileOrDir.listFiles()) {
                doIndex(w, dw, f);
            }
        } else {
            indexFile(w, dw, fileOrDir);
        }
    }

    private void indexFile(IndexWriter w, DuplicatesIndex.IndexWriter dw, File source) throws IOException {
        if (!source.getName().endsWith(".java"))
            return ;
        
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        StandardJavaFileManager m = tool.getStandardFileManager(null, null, null);

        m.setLocation(StandardLocation.CLASS_PATH, Collections.<File>emptyList());
        m.setLocation(StandardLocation.SOURCE_PATH, Collections.<File>emptyList());
        
        Iterable<? extends JavaFileObject> fos = m.getJavaFileObjects(source);
        DiagnosticListener<JavaFileObject> devNull = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {}
        };
        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, devNull, Arrays.asList("-bootclasspath",  bootPath), null, fos);
        CompilationUnitTree cut = ct.parse().iterator().next();

        w.record(ct, source.toURI().toURL(), cut);

        if (dw != null) {
            ct.analyze(ct.enter(Collections.singletonList(cut)));

            dw.record(ct, source.toURI().toURL(), cut);
        }
    }
    
}
