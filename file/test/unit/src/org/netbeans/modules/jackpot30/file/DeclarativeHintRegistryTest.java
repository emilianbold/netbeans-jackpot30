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

package org.netbeans.modules.jackpot30.file;

import org.junit.Test;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import static org.junit.Assert.*;

/**
 *
 * @author Jan Lahoda
 */
public class DeclarativeHintRegistryTest {

    public DeclarativeHintRegistryTest() {
    }

    @Test
    public void testParse() {
        HintDescription h = DeclarativeHintRegistry.parse("\"test\":$1A{java.io.File}.toURL()=>\"fix\":$1B.toURI().toURL()");
        String textRepr = dumpDescription(h);
        
        assertEquals("{$1A.toURL()}{test}=>{$1B.toURI().toURL()}{fix};", textRepr);
    }

    @Test
    public void testParseNoFixName() {
        HintDescription h = DeclarativeHintRegistry.parse("\"test\":$1A{java.io.File}.toURL()=>$1B.toURI().toURL()");
        String textRepr = dumpDescription(h);

        assertEquals("{$1A.toURL()}{test}=>{$1B.toURI().toURL()};", textRepr);
    }

    private static String dumpDescription(HintDescription h) {
        StringBuilder dump = new StringBuilder();

        dump.append("{");
        dump.append(h.getTriggerPattern().getPattern());
        dump.append("}");
        //XXX: test also constraints
        dump.append("{");

        DeclarativeHintsWorker w = (DeclarativeHintsWorker) h.getWorker();

        dump.append(w.getDisplayName());
        dump.append("}");

        for (DeclarativeFix f : w.getFixes()) {
            dump.append("=>{");
            dump.append(f.getPattern());
            dump.append("}");
            if (f.getDisplayName() != null) {
                dump.append("{");
                dump.append(f.getDisplayName());
                dump.append("}");
            }
        }

        dump.append(";");

        return dump.toString();
    }

}