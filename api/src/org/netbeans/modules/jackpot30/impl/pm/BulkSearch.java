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

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.jackpot30.impl.Utilities;

/**
 *
 * @author lahvac
 */
public class BulkSearch {

    public static Set<String> match(CompilationInfo info, Tree tree, BulkPattern pattern) {
        Set<String> occurringPatterns = new HashSet<String>();
        StringBuilder ser = new StringBuilder();

        TreeSerializer.serializeText(tree, ser);

        String serialized = ser.toString();

        long s2 = System.currentTimeMillis();
        Matcher m = pattern.toRegexpPattern().matcher(serialized);

//        System.err.println("matcher=" + (System.currentTimeMillis() - s2));

        while (m.find()) {
            for (int cntr = 0; cntr < pattern.groups.length; cntr++) {
                if (m.group(pattern.groups[cntr]) != null) {
                    occurringPatterns.add(pattern.original.get(cntr));
                }
            }
        }

        long e2 = System.currentTimeMillis();

//        System.err.println("match: " + (e2 - s2));
        return occurringPatterns;
    }
    
    public static BulkPattern create(CompilationInfo info, String... code) {
        return create(info, Arrays.asList(code));
    }

    public static BulkPattern create(CompilationInfo info, Collection<? extends String> code) {
        StringBuilder ser = new StringBuilder();
        Tree[] patterns = new Tree[code.size()];
        int i = 0;

        for (String c : code) {
            patterns[i++] = Utilities.parseAndAttribute(info, c, null);
        }

        int[] groups = TreeSerializer.serializePatterns(ser, patterns);

        return BulkPattern.create(new LinkedList<String>(code), ser.toString(), groups);
    }

    public static class BulkPattern {

        private final List<String> original;
        private final String serialized;
        private final Pattern p;
        private final int[] groups;

        private BulkPattern(List<String> original, String serialized, int[] groups) {
            this.original = original;
            this.serialized = serialized;
            this.p = Pattern.compile(serialized);
            this.groups = groups;
        }

        Pattern toRegexpPattern() {
            return p;
        }

        private static BulkPattern create(List<String> original, String patterns, int[] groups) {
            return new BulkPattern(original, patterns, groups);
        }

    }
}
