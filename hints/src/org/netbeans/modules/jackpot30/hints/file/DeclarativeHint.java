/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.hints.file;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Jan Lahoda
 */
public class DeclarativeHint {

    private final String pattern;
    private final String displayName;
    private final List<DeclarativeFix> fixes;

    private DeclarativeHint(String pattern, String displayName, List<DeclarativeFix> fixes) {
        this.pattern = pattern;
        this.displayName = displayName;
        this.fixes = fixes;
    }

    public String getPattern() {
        return pattern;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<DeclarativeFix> getFixes() {
        return fixes;
    }

    public static DeclarativeHint parse(String spec) {
        List<String> split = Arrays.asList(spec.split("=>"));

        assert split.size() >= 1;

        List<DeclarativeFix> fixes = new LinkedList<DeclarativeFix>();

        for (String s : split.subList(1, split.size())) {
            fixes.add(DeclarativeFix.parse(s));
        }

        String[] s = splitNameAndPatter(split.get(0));

        return new DeclarativeHint(s[1], s[0], fixes);
    }

    private static String[] splitNameAndPatter(String spec) {
        spec = spec.trim();

        String[] s = spec.split("\"");

        return new String[] {
            s[1],
            s[2].substring(1)
        };
    }
    
    public static final class DeclarativeFix {
        
        private final String pattern;
        private final String displayName;

        private DeclarativeFix(String pattern, String displayName) {
            this.pattern = pattern;
            this.displayName = displayName;
        }

        public String getPattern() {
            return pattern;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static DeclarativeFix parse(String spec) {
            String[] s = splitNameAndPatter(spec);

            return new DeclarativeFix(s[1], s[0]);
        }
    }
    
}
