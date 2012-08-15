/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.jackpot30.ide.usages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Icon;
import org.netbeans.api.fileinfo.NonRecursiveFolder;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.modules.jackpot30.ide.usages.RemoteUsages.SearchOptions;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.refactoring.api.AbstractRefactoring;
import org.netbeans.modules.refactoring.api.Problem;
import org.netbeans.modules.refactoring.api.RefactoringElement;
import org.netbeans.modules.refactoring.api.Scope;
import org.netbeans.modules.refactoring.api.ScopeDescription;
import org.netbeans.modules.refactoring.api.ScopeProvider;
import org.netbeans.modules.refactoring.api.WhereUsedQuery;
import org.netbeans.modules.refactoring.spi.RefactoringElementsBag;
import org.netbeans.modules.refactoring.spi.RefactoringPlugin;
import org.netbeans.modules.refactoring.spi.RefactoringPluginFactory;
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation;
import org.netbeans.modules.refactoring.spi.ui.ExpandableTreeElement;
import org.netbeans.modules.refactoring.spi.ui.TreeElement;
import org.netbeans.modules.refactoring.spi.ui.TreeElementFactory;
import org.netbeans.modules.refactoring.spi.ui.TreeElementFactoryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.text.PositionBounds;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class RemoteWhereUsedQuery implements RefactoringPlugin {

    private final WhereUsedQuery what;

    public RemoteWhereUsedQuery(WhereUsedQuery what) {
        this.what = what;
    }

    @Override
    public Problem preCheck() {
        return null;
    }

    @Override
    public Problem checkParameters() {
        return null;
    }

    @Override
    public Problem fastCheckParameters() {
        return null;
    }

    @Override
    public void cancelRequest() { }

    @Override
    public Problem prepare(RefactoringElementsBag refactoringElements) {
        if (what.getContext().lookup(Scope.class) != REMOTE_SCOPE) return null;
        
        TreePathHandle handle = what.getRefactoringSource().lookup(TreePathHandle.class);
        ElementHandle<?> toSearch = handle.getElementHandle();
        Set<SearchOptions> searchOptions = EnumSet.noneOf(SearchOptions.class);

        for (FileObject found : RemoteUsages.findUsages(toSearch, searchOptions, /*XXX*/new AtomicBoolean())) {
            Impl i = new Impl(found, toSearch, searchOptions);

            refactoringElements.add(what, i);
        }

        return null;
    }

    private static final class Impl extends SimpleRefactoringElementImplementation {

        private final FileObject file;
        private final ElementHandle<?> eh;
        private final Set<SearchOptions> searchOptions;

        public Impl(FileObject file, ElementHandle<?> eh, Set<SearchOptions> searchOptions) {
            this.file = file;
            this.eh = eh;
            this.searchOptions = searchOptions;
        }

        @Override
        public String getText() {
            return file.getNameExt(); //XXX
        }

        @Override
        public String getDisplayText() {
            return getText();
        }

        @Override
        public void performChange() {
            throw new UnsupportedOperationException("Nothing to perform.");
        }

        @Override
        public Lookup getLookup() {
            return Lookups.singleton(this);
        }

        @Override
        public FileObject getParentFile() {
            return file.getParent();
        }

        @Override
        public PositionBounds getPosition() {
            return null;
        }

    }

    @ServiceProvider(service=RefactoringPluginFactory.class)
    public static final class FactoryImpl implements RefactoringPluginFactory {

        @Override
        public RefactoringPlugin createInstance(AbstractRefactoring refactoring) {
            if (refactoring instanceof WhereUsedQuery) {
                return new RemoteWhereUsedQuery((WhereUsedQuery) refactoring);
            }

            return null;
        }

    }

    @ServiceProvider(service=TreeElementFactoryImplementation.class, position=1)
    public static final class TreeFactoryImpl implements TreeElementFactoryImplementation {

        @Override
        public TreeElement getTreeElement(Object o) {
            if (o instanceof RefactoringElement) {
                Impl i = ((RefactoringElement) o).getLookup().lookup(Impl.class);

                if (i != null) {
                    TreeElement fileElement = TreeElementFactory.getTreeElement(i.file);

                    assert fileElement != null;

                    return new TreeElementImpl(i, fileElement);
                }
            }
            return null;
        }

        @Override
        public void cleanUp() {

        }

    }

    private static final class TreeElementImpl implements ExpandableTreeElement {

        private final Impl impl;
        private final TreeElement delegateTo;

        public TreeElementImpl(Impl impl, TreeElement delegateTo) {
            this.impl = impl;
            this.delegateTo = delegateTo;
        }

        @Override
        public TreeElement getParent(boolean isLogical) {
            return delegateTo.getParent(isLogical);
        }

        @Override
        public Icon getIcon() {
            return delegateTo.getIcon();
        }

        @Override
        public String getText(boolean isLogical) {
            return delegateTo.getText(isLogical);
        }

        @Override
        public Object getUserObject() {
            return delegateTo.getUserObject();
        }

        @Override
        public Iterator<TreeElement> iterator() {
            final List<TreeElement> result = new ArrayList<TreeElement>();

            RemoteUsages.computeOccurrences(impl.file, impl.eh, impl.searchOptions, this, result);
            
            return result.iterator();
        }

        @Override
        public int estimateSubNodesCount() {
            return 1;
        }
    }

    private static final Scope REMOTE_SCOPE = Scope.create(Collections.<FileObject>emptyList(), Collections.<NonRecursiveFolder>emptyList(), Collections.<FileObject>emptyList());
    private static final ScopeDescription REMOTE_SCOPE_DESCRIPTION = new ScopeDescription("remote-scope", "Remote Scope", null, "A") {
        @Override public Scope getScope() {
            return REMOTE_SCOPE;
        }
    };

    @ServiceProvider(service=ScopeProvider.class)
    public static final class ScopeProviderImpl implements ScopeProvider {

        @Override
        public Iterable<ScopeDescription> scopesFor(Lookup source) {
            if (RemoteIndex.loadIndices().iterator().hasNext()) {
                return Collections.singletonList(REMOTE_SCOPE_DESCRIPTION);
            } else {
                return Collections.emptyList();
            }
        }

    }

}
