package org.netbeans.modules.jackpot30.impl.refactoring;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.ModificationResult.Difference;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchUtilities;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.java.spi.DiffElement;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.openide.filesystems.FileObject;

public class ApplyPatternRefactoringPlugin implements RefactoringPlugin {

    private final ApplyPatternRefactoring refactoring;

    public ApplyPatternRefactoringPlugin(ApplyPatternRefactoring refactoring) {
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

    public void cancelRequest() {
        //TODO
    }

    public Problem prepare(RefactoringElementsBag refactoringElements) {
        BatchResult candidates = BatchSearch.findOccurrences(refactoring.getPattern(), refactoring.getScope());
        LinkedList<String> problems = new LinkedList<String>();
        Collection<? extends ModificationResult> res = BatchUtilities.applyFixes(candidates, null, /*XXX*/new AtomicBoolean(), problems);

        refactoringElements.registerTransaction(new RetoucheCommit(new LinkedList<ModificationResult>(res)));
        
        for (ModificationResult mr : res) {
            for (FileObject file : mr.getModifiedFileObjects()) {
                for (Difference d : mr.getDifferences(file)) {
                    refactoringElements.add(refactoring, DiffElement.create(d, file, mr));
                }
            }
        }

        Problem current = null;

        for (String problem : problems) {
            Problem p = new Problem(false, problem);

            if (current != null)
                p.setNext(current);
            current = p;
        }

        return current;
    }

}