package org.netbeans.modules.jackpot30.impl.refactoring;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.netbeans.modules.jackpot30.impl.batch.BatchSearch.Scope;
import org.netbeans.modules.refactoring.spi.ui.UI;

public final class FindOccurrencesAction implements ActionListener {

    public void actionPerformed(ActionEvent e) {
        UI.openRefactoringUI(new FindDuplicatesRefactoringUI(null, Scope.ALL_OPENED_PROJECTS));
    }
}
