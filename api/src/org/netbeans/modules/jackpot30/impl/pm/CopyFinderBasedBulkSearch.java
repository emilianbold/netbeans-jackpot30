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

package org.netbeans.modules.jackpot30.impl.pm;

import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.type.TypeMirror;
import org.netbeans.api.java.source.CompilationInfo;

/**
 *
 * @author lahvac
 */
public class CopyFinderBasedBulkSearch extends BulkSearch {

    public CopyFinderBasedBulkSearch() {
        super(false);
    }

    @Override
    public Map<String, Collection<TreePath>> match(CompilationInfo info, Tree tree, BulkPattern pattern, Map<String, Long> timeLog) {
        Map<String, Collection<TreePath>> result = new HashMap<String, Collection<TreePath>>();
        
        for (Entry<Tree, String> e : ((BulkPatternImpl) pattern).pattern2Code.entrySet()) {
            TreePath topLevel = new TreePath(info.getCompilationUnit());
            
            for (TreePath r : CopyFinder.computeDuplicates(info, new TreePath(topLevel, e.getKey()), topLevel, false, new AtomicBoolean(), Collections.<String, TypeMirror>emptyMap()).keySet()) {
                Collection<TreePath> c = result.get(e.getValue());

                if (c == null) {
                    result.put(e.getValue(), c = new LinkedList<TreePath>());
                }

                c.add(r);
            }
        }

        return result;
    }

    @Override
    public boolean matches(CompilationInfo info, Tree tree, BulkPattern pattern) {
        //XXX: performance
        return !match(info, tree, pattern).isEmpty();
    }

    @Override
    public BulkPattern create(Collection<? extends String> code, Collection<? extends Tree> patterns) {
        Map<Tree, String> pattern2Code = new HashMap<Tree, String>();

        Iterator<? extends String> itCode = code.iterator();
        Iterator<? extends Tree>   itPatt = patterns.iterator();

        while (itCode.hasNext() && itPatt.hasNext()) {
            pattern2Code.put(itPatt.next(), itCode.next());
        }

        return new BulkPatternImpl(pattern2Code);
    }

    @Override
    public boolean matches(InputStream encoded, BulkPattern pattern) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void encode(Tree tree, EncodingContext ctx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static final class BulkPatternImpl extends BulkPattern {

        private final Map<Tree, String> pattern2Code;
        
        public BulkPatternImpl(Map<Tree, String> pattern2Code) {
            super(null, null);
            this.pattern2Code = pattern2Code;
        }

    }

}
