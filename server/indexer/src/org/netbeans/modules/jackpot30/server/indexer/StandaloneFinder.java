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

package org.netbeans.modules.jackpot30.server.indexer;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch;
import org.netbeans.modules.jackpot30.impl.pm.BulkSearch.BulkPattern;

/**
 *
 * @author lahvac
 */
public class StandaloneFinder {

    public static Collection<? extends String> findCandidates(File sourceRoot, String pattern) throws IOException {
        BulkPattern bulkPattern = preparePattern(pattern);
        
        return Index.get(sourceRoot.toURI().toURL()).findCandidates(bulkPattern);
    }

    public static int[] findCandidateOccurrenceSpans(File sourceRoot, String relativePath, String pattern) throws IOException {
        BulkPattern bulkPattern = preparePattern(pattern);
        CharSequence source = Index.get(sourceRoot.toURI().toURL()).getSourceCode(relativePath);
        JavacTaskImpl jti = prepareJavacTaskImpl();
        CompilationUnitTree cut = jti.parse(new JFOImpl(source)).iterator().next();
        Collection<TreePath> paths = new LinkedList<TreePath>();
        
        for (Collection<TreePath> c : BulkSearch.getDefault().match(null, new TreePath(cut), bulkPattern).values()) {
            paths.addAll(c);
        }

        Trees t = Trees.instance(jti);
        int[] result = new int[2 * paths.size()];
        int i = 0;

        for (TreePath tp : paths) {
            result[i++] = (int) t.getSourcePositions().getStartPosition(cut, tp.getLeaf());
            result[i++] = (int) t.getSourcePositions().getEndPosition(cut, tp.getLeaf());
        }

        return result;
    }

    private static BulkPattern preparePattern(String pattern) {
        Collection<String> patterns = new LinkedList<String>();
        Collection<Tree> trees = new LinkedList<Tree>();

        for (String s : pattern.split(";;")) {
            s = s.trim();
            if (s.isEmpty()) {
                continue;
            }
            patterns.add(s);
            trees.add(Utilities.parseAndAttribute(prepareJavacTaskImpl(), s));
        }

        return BulkSearch.getDefault().create(patterns, trees);
    }

    private static JavacTaskImpl prepareJavacTaskImpl() {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();

        assert tool != null;

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, Arrays.asList("-bootclasspath",  bootPath, "-Xjcov"), null, Collections.<JavaFileObject>emptyList());
        
        return ct;
    }

    private static final class JFOImpl extends SimpleJavaFileObject {
        private final CharSequence code;
        public JFOImpl(CharSequence code) {
            super(URI.create(""), Kind.SOURCE);
            this.code = code;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return code;
        }
    }
}
