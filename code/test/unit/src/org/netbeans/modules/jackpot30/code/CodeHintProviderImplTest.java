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

package org.netbeans.modules.jackpot30.code;

import com.sun.source.tree.Tree.Kind;
import java.util.Collection;
import java.util.Iterator;
import org.junit.Test;
import org.netbeans.modules.jackpot30.code.CodeHintProviderImpl.WorkerImpl;
import org.netbeans.modules.jackpot30.code.spi.Constraint;
import org.netbeans.modules.jackpot30.code.spi.Hint;
import org.netbeans.modules.jackpot30.code.spi.TriggerPattern;
import org.netbeans.modules.jackpot30.code.spi.TriggerTreeKind;
import org.netbeans.modules.jackpot30.spi.HintContext;
import static org.junit.Assert.*;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.spi.editor.hints.ErrorDescription;

/**
 *
 * @author lahvac
 */
public class CodeHintProviderImplTest {

    public CodeHintProviderImplTest() {
    }

    @Test
    public void testComputeHints() {
        Collection<? extends HintDescription> hints = new CodeHintProviderImpl().computeHints();

        assertEquals(2, hints.size());

        Iterator<? extends HintDescription> it = hints.iterator();

        assertEquals("null:$1.toURL():public static org.netbeans.spi.editor.hints.ErrorDescription org.netbeans.modules.jackpot30.code.CodeHintProviderImplTest.hintPattern1(org.netbeans.modules.jackpot30.spi.HintContext)", toString(it.next()));
        assertEquals("METHOD_INVOCATION:null:public static org.netbeans.spi.editor.hints.ErrorDescription org.netbeans.modules.jackpot30.code.CodeHintProviderImplTest.hintPattern2(org.netbeans.modules.jackpot30.spi.HintContext)", toString(it.next()));
    }

    private static String toString(HintDescription hd) {
        StringBuilder sb = new StringBuilder();

        sb.append(hd.getTriggerKind());
        sb.append(":");
        
        PatternDescription p = hd.getTriggerPattern();

        sb.append(p != null ? p.getPattern() : "null");
        //TODO: constraints
        sb.append(":");
        sb.append(((WorkerImpl) hd.getWorker()).getMethod().toGenericString());

        return sb.toString();
    }

    @Hint("hintPattern1")
    @TriggerPattern(value="$1.toURL()", constraints=@Constraint(variable="$1", type="java.io.File"))
    public static ErrorDescription hintPattern1(HintContext ctx) {
        return null;
    }

    @Hint("hintPattern2")
    @TriggerTreeKind(Kind.METHOD_INVOCATION)
    public static ErrorDescription hintPattern2(HintContext ctx) {
        return null;
    }

}