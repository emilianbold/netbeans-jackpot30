package org.netbeans.modules.jackpot30.impl.refactoring;

import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringPluginFactory;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=RefactoringPluginFactory.class)
public class RefactoringPluginFactoryImpl implements RefactoringPluginFactory {

    public RefactoringPlugin createInstance(AbstractRefactoring refactoring) {
        if (refactoring instanceof FindDuplicatesRefactoring) {
            return new FindDuplicatesRefactoringPlugin((FindDuplicatesRefactoring) refactoring);
        }

        if (refactoring instanceof ApplyPatternRefactoring) {
            return new ApplyPatternRefactoringPlugin((ApplyPatternRefactoring) refactoring);
        }
        return null;
    }
    
}
