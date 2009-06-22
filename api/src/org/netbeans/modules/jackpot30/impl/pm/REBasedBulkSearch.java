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
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer.Result;

/**
 *
 * @author lahvac
 */
public class REBasedBulkSearch extends BulkSearch {

    public REBasedBulkSearch() {
        super(true);
    }

    @Override
    public Map<String, Collection<TreePath>> match(CompilationInfo info, Tree tree, BulkPattern patternIn, Map<String, Long> timeLog) {
        REBasedBulkPattern pattern = (REBasedBulkPattern) patternIn;
        
        if (pattern.original.isEmpty()) {
            return Collections.<String, Collection<TreePath>>emptyMap();
        }
        
        Map<String, Collection<TreePath>> occurringPatterns = new HashMap<String, Collection<TreePath>>();
        Result r = TreeSerializer.serializeText(tree);

        if (timeLog != null) {
            timeLog.put("[C] Jackpot 3.0 Serialized Tree Size", (long) r.encoded.length());
        }
        
        long s2 = System.currentTimeMillis();
        Matcher m = pattern.toRegexpPattern().matcher(r.encoded);
        int start = 0; //XXX: hack to allow matches inside other matches (see testTwoPatterns)
        long patternOccurrences = 0;

//        System.err.println("matcher=" + (System.currentTimeMillis() - s2));

        while (start < r.encoded.length() && m.find(start)) {
            patternOccurrences++;
            for (int cntr = 0; cntr < pattern.groups.length; cntr++) {
                if (m.group(pattern.groups[cntr]) != null) {
                    String patt = pattern.original.get(cntr);
                    Collection<TreePath> occurrences = occurringPatterns.get(patt);

                    if (occurrences == null) {
                        occurringPatterns.put(patt, occurrences = new LinkedList<TreePath>());
                    }

                    List<TreePath> paths = r.serializedEnd2Tree.get(m.end());

                    if (pattern.patternsWithUnrolledBlocks.contains(cntr)) {
                        //HACK: see BulkSearchTest.testNoExponentialTimeComplexity
                        List<TreePath> updated = new LinkedList<TreePath>();

                        for (TreePath tp : paths) {
                            if (tp.getParentPath().getLeaf().getKind() == Kind.BLOCK) {
                                updated.add(tp.getParentPath());
                            } else {
                                //see BulkSearchTest.testMultiStatementVariablesAndBlocks4
                                updated.add(tp);
                            }
                        }

                        paths = updated;
                    }

                    occurrences.addAll(paths);
                }
            }

            start = m.start() + 1;
        }

        long e2 = System.currentTimeMillis();

        if (timeLog != null) {
            timeLog.put("[C] Jackpot 3.0 Pattern Occurrences", patternOccurrences);
        }

//        System.err.println("match: " + (e2 - s2));
        return occurringPatterns;
    }

    @Override
    public boolean matches(String encoded, BulkPattern patternIn) {
        REBasedBulkPattern pattern = (REBasedBulkPattern) patternIn;

        return pattern.toRegexpPattern().matcher(encoded).find();
    }

    @Override
    public BulkPattern create(Collection<? extends String> code, Collection<? extends Tree> patterns) {
        Result r = TreeSerializer.serializePatterns(patterns.toArray(new Tree[0]));

        return REBasedBulkPattern.create(new LinkedList<String>(code), r);
    }

    public static class REBasedBulkPattern extends BulkPattern {

        private final List<String> original;
        private final String serialized;
        private final Pattern p;
        private final int[] groups;
        private final Set<Integer> patternsWithUnrolledBlocks;

        private REBasedBulkPattern(List<String> original, Result r) {
            super(r.identifiers, r.treeKinds);
            this.original = original;
            this.serialized = r.encoded;
            this.p = Pattern.compile(serialized);
            this.groups = r.groups;
            this.patternsWithUnrolledBlocks = r.patternsWithUnrolledBlocks;
        }

        Pattern toRegexpPattern() {
            return p;
        }

        private static REBasedBulkPattern create(List<String> original, Result r) {
            return new REBasedBulkPattern(original, r);
        }

    }
}
