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

package org.netbeans.modules.jackpot30.impl.refactoring;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.netbeans.modules.java.hints.jackpot.impl.MessageImpl;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch;
import org.netbeans.modules.java.hints.jackpot.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.java.hints.jackpot.impl.batch.ProgressHandleWrapper;
import org.netbeans.modules.java.hints.jackpot.spi.HintContext.MessageKind;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;

public class FindDuplicatesRefactoringPlugin extends AbstractApplyHintsRefactoringPlugin {

    private final FindDuplicatesRefactoring refactoring;

    public FindDuplicatesRefactoringPlugin(FindDuplicatesRefactoring refactoring) {
        super(refactoring);
        this.refactoring = refactoring;
    }

    public Problem preCheck() {
        return null;
    }

    public Problem checkParameters() {
        return null;
    }

    public Problem fastCheckParameters() {
        return null;
    }

     public Problem prepare(RefactoringElementsBag refactoringElements) {
        cancel.set(false);

        Collection<MessageImpl> problems = refactoring.isQuery() ? performSearchForPattern(refactoringElements) : performApplyPattern(refactoringElements);
        Problem current = null;

        for (MessageImpl problem : problems) {
            Problem p = new Problem(problem.kind == MessageKind.ERROR, problem.text);

            if (current != null)
                p.setNext(current);
            current = p;
        }

        return current;
    }

    private List<MessageImpl> performSearchForPattern(final RefactoringElementsBag refactoringElements) {
        ProgressHandleWrapper w = new ProgressHandleWrapper(this, 50, 50);
        BatchResult candidates = BatchSearch.findOccurrences(refactoring.getPattern(), refactoring.getScope(), w);
        List<MessageImpl> problems = new LinkedList<MessageImpl>(candidates.problems);

        prepareElements(candidates, w, refactoringElements, refactoring.isVerify(), problems);

        w.finish();

        return problems;
     }

    private Collection<MessageImpl> performApplyPattern(RefactoringElementsBag refactoringElements) {
        return performApplyPattern(refactoring.getPattern(), refactoring.getScope(), refactoringElements);
    }

}