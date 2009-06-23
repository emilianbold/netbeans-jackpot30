package org.netbeans.modules.jackpot30.impl.refactoring;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import org.netbeans.api.java.source.ModificationResult;
import org.netbeans.api.java.source.ModificationResult.Difference;
import org.netbeans.modules.jackpot30.impl.batch.BatchApply;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.BatchResult;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Resource;
import org.netbeans.modules.jackpot30.impl.batch.JavaFixImpl;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.java.spi.DiffElement;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
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
        Map<FileObject, Collection<ErrorDescription>> file2eds = new HashMap<FileObject, Collection<ErrorDescription>>();
        BatchResult candidates = BatchSearch.findOccurrences(refactoring.getPattern(), refactoring.getScope());

        for (Iterable<? extends Resource> it :candidates.projectId2Resources.values()) {
            for (Resource r : it) {
                List<ErrorDescription> eds = new LinkedList<ErrorDescription>();

                Iterable<? extends ErrorDescription> current = r.getVerifiedSpans();

                if (current == null) {
                    //XXX: warn?
                    continue;
                }

                for (ErrorDescription ed : current) {
                    eds.add(ed);
                }
                
                if (!eds.isEmpty()) {
                    file2eds.put(r.getResolvedFile(), eds);
                }
            }
        }

        Map<FileObject, List<JavaFix>> file2Fixes = new HashMap<FileObject, List<JavaFix>>();

        for (final Entry<FileObject, Collection<ErrorDescription>> e : file2eds.entrySet()) {
            LinkedList<JavaFix> fixes = new LinkedList<JavaFix>();
            
            file2Fixes.put(e.getKey(), fixes);
            
            for (ErrorDescription ed : e.getValue()) {
                if (!ed.getFixes().isComputed()) {
                    throw new IllegalStateException();//TODO: should be problem
                }

                if (ed.getFixes().getFixes().size() != 1) {
                    throw new IllegalStateException();//TODO: should be problem
                }

                Fix f = ed.getFixes().getFixes().get(0);

                if (!(f instanceof JavaFixImpl)) {
                    throw new IllegalStateException();//TODO: should be problem
                }


                fixes.add(((JavaFixImpl) f).jf);
            }
        }

        List<ModificationResult> res = BatchApply.performFastFixes(file2Fixes, null, new AtomicBoolean());

        for (ModificationResult mr : res) {
            for (FileObject file : mr.getModifiedFileObjects()) {
                for (Difference d : mr.getDifferences(file)) {
                    refactoringElements.add(refactoring, DiffElement.create(d, file, mr));
                }
            }
        }
                                    
        return null;
    }

}