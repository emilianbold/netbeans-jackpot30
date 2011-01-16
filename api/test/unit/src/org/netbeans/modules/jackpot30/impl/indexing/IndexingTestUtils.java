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
 * accompanied this content. If applicable, add the following below the
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
 * However, if you add GPL Version 2 content and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new content is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.impl.indexing;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.model.JavacTypes;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.TestUtilities;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesCustomIndexerImpl.FactoryImpl;
import org.netbeans.modules.jackpot30.impl.duplicates.indexing.DuplicatesIndex;
import org.netbeans.modules.jackpot30.impl.indexing.CustomIndexerImpl.CustomIndexerFactoryImpl;
import org.netbeans.modules.jackpot30.impl.indexing.Index.IndexWriter;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

public class IndexingTestUtils {
    
    public static void writeFilesAndWaitForScan(FileObject sourceRoot, File... files) throws Exception {
        for (FileObject c : sourceRoot.getChildren()) {
            c.delete();
        }

        for (File f : files) {
            FileObject fo = FileUtil.createData(sourceRoot, f.filename);
            TestUtilities.copyStringToFile(fo, f.content);
        }

        SourceUtils.waitScanFinished();
    }

    public static void indexFiles(URI root, Index idx, File... files) throws Exception {
        IndexWriter iw = idx.openForWriting();

        try {
            iw.clear();

            for (File f : files) {
                indexFile(root, iw, null, f);
            }
        } finally {
            iw.close();
        }
    }

    public static final class File {
        public final String filename;
        public final String content;
        public final boolean index;

        public File(String filename, String content) {
            this(filename, content, true);
        }

        public File(String filename, String content, boolean index) {
            this.filename = filename;
            this.content = content;
            this.index = index;
        }
    }

    @ServiceProvider(service=MimeDataProvider.class)
    public static final class MimeDataProviderImpl implements MimeDataProvider {

        private static final Lookup INDEXER = Lookups.fixed(new CustomIndexerFactoryImpl(), new FactoryImpl());

        public Lookup getLookup(MimePath mimePath) {
            if ("text/x-java".equals(mimePath.getPath())) {
                return INDEXER;
            }
            return null;
        }

    }

    //copied from StandaloneIndexer:
    private static void indexFile(URI root, IndexWriter w, DuplicatesIndex.IndexWriter dw, File source) throws IOException {
        if (!source.filename.endsWith(".java"))
            return ;

        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        DiagnosticListener<JavaFileObject> devNull = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {}
        };
        StandardJavaFileManager m = tool.getStandardFileManager(devNull, null, null);

        m.setLocation(StandardLocation.CLASS_PATH, Collections.<java.io.File>emptyList());
        m.setLocation(StandardLocation.SOURCE_PATH, Collections.<java.io.File>emptyList());

        JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, devNull, Arrays.asList("-bootclasspath",  bootPath), null, Collections.singleton(new JFOImpl(source.filename, source.content)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze(ct.enter(Collections.singletonList(cut)));

        w.record(root.resolve(source.filename).toURL(), cut, source.index ? new Index.AttributionWrapper(Trees.instance(ct), JavacTypes.instance(ct.getContext())) : null);
    }

    private static final class JFOImpl extends SimpleJavaFileObject {
        private final String content;
        public JFOImpl(String filename, String content) {
            super(URI.create("myfo:/" + filename), JavaFileObject.Kind.SOURCE);
            this.content = content;
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }
    }

}