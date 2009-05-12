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

package org.netbeans.modules.jackpot30.impl;

import com.sun.source.tree.Tree.Kind;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintProvider;
import org.openide.util.Lookup;

/**
 *
 * @author lahvac
 */
public class RulesManager {

    private final Map<Kind, List<HintDescription>> kind2Hints = new HashMap<Kind, List<HintDescription>>();
    private final Map<PatternDescription, List<HintDescription>> pattern2Hint = new HashMap<PatternDescription, List<HintDescription>>();

    private static final RulesManager INSTANCE = new RulesManager();

    public static RulesManager getInstance() {
        return INSTANCE;
    }

    private RulesManager() {
        for (HintProvider p : Lookup.getDefault().lookupAll(HintProvider.class)) {
            for (HintDescription d : p.computeHints()) {
                if (d.getTriggerKind() != null) {
                    List<HintDescription> l = kind2Hints.get(d.getTriggerKind());

                    if (l == null) {
                        kind2Hints.put(d.getTriggerKind(), l = new LinkedList<HintDescription>());
                    }

                    l.add(d);
                }
                if (d.getTriggerPattern() != null) {
                    List<HintDescription> l = pattern2Hint.get(d.getTriggerPattern());

                    if (l == null) {
                        pattern2Hint.put(d.getTriggerPattern(), l = new LinkedList<HintDescription>());
                    }
                    
                    l.add(d);
                }
            }
        }
    }

    public Map<Kind, List<HintDescription>> getKindBasedHints() {
        return kind2Hints;
    }

    public Map<PatternDescription, List<HintDescription>> getPatternBasedHints() {
        return pattern2Hint;
    }

}