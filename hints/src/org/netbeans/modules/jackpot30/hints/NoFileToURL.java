/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javahints;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.modules.javahints.epi.ErrorDescriptionFactory;
import org.netbeans.modules.javahints.epi.HintContext;
import org.netbeans.modules.javahints.epi.JavaFix;
import org.netbeans.modules.javahints.epi.Pattern;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;

/**
 *
 * @author Jan Lahoda
 */
public class NoFileToURL extends AbstractHint {

    private final AtomicBoolean cancel = new AtomicBoolean();
    
    public NoFileToURL() {
        super(false, false, HintSeverity.ERROR);
    }
    
    @Override
    public String getDescription() {
        return "Warns about usages of java.io.File.toURL";
    }

    public Set<Kind> getTreeKinds() {
        return EnumSet.of(Kind.METHOD_INVOCATION);
    }

    public List<ErrorDescription> run(CompilationInfo info, TreePath treePath) {
        if (true) {
            return null;
        }
        cancel.set(false);

        Map<String, TreePath> vars = Pattern.matchesPattern(info, "$1{java.io.File}.toURL()", treePath, cancel);

        if (vars == null) {
            return null;
        }
        
        Fix f = JavaFix.rewriteFix(info, "Rewrite to .toURI().toURL()", treePath, "$1.toURI().toURL()", vars);
        ErrorDescription w = ErrorDescriptionFactory.forName(HintContext.create(info, this), treePath, "Use of java.io.File.toURL()", f);

        if (w == null) {
            return null;
        }
        
        return Collections.singletonList(w);
    }

    public String getId() {
        return NoFileToURL.class.getName();
    }

    public String getDisplayName() {
        return "File.toURL()";
    }

    public void cancel() {
        cancel.set(true);
    }

}
