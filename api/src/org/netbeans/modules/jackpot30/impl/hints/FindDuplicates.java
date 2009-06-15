package org.netbeans.modules.jackpot30.impl.hints;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Context;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.JavaSource.Priority;
import org.netbeans.api.java.source.JavaSourceTaskFactory;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.UiUtils;
import org.netbeans.api.java.source.support.CaretAwareJavaSourceTaskFactory;
import org.netbeans.api.java.source.support.SelectionAwareJavaSourceTaskFactory;
import org.netbeans.modules.jackpot30.impl.indexing.Index;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer;
import org.netbeans.modules.jackpot30.impl.pm.TreeSerializer.Result;
import org.netbeans.modules.java.hints.introduce.IntroduceHint;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.java.source.builder.TreeFactory;
import org.netbeans.modules.java.source.pretty.ImportAnalysis2;
import org.netbeans.modules.java.source.transform.ImmutableTreeTranslator;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
public class FindDuplicates implements CancellableTask<CompilationInfo> {

    public void run(CompilationInfo info) throws Exception {
        Collection<? extends ErrorDescription> eds = computeErrorDescription(info);

        if (eds == null) {
            eds = Collections.emptyList();
        }

        HintsController.setErrors(info.getFileObject(), FindDuplicates.class.getName(), eds);
    }

    private Collection<? extends ErrorDescription> computeErrorDescription(CompilationInfo info) throws Exception {
        int[] span = SelectionAwareJavaSourceTaskFactory.getLastSelection(info.getFileObject());

        if (span == null) {
            return null;
        }

        TreePath selection = selectionForExpressionHack(info, span[0], span[1]);

        if (selection == null) {
            if (selectionForStatementsHack(info, span[0], span[1], new int[2]) == null) {
                return null;
            }
        }

        Fix f = new FixImpl(info.getFileObject(), span[0], span[1]);
        int caret = CaretAwareJavaSourceTaskFactory.getLastPosition(info.getFileObject());
        ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(Severity.HINT, "Look for Duplicates", Collections.singletonList(f), info.getFileObject(), caret, caret);

        return Collections.singletonList(ed);
    }

    public void cancel() {
        //XXX
    }

    private static Collection<? extends FileObject> computeDuplicateCandidates(CompilationInfo info, int start, int end) {
        Tree generalized = resolveAndGeneralizePattern(info, start, end);

        if (generalized == null) {
            return Collections.emptyList();
        }

        Result pattern = TreeSerializer.serializePatterns(generalized);
        List<FileObject> result = new LinkedList<FileObject>();

        for (FileObject src : GlobalPathRegistry.getDefault().getSourceRoots()) {
            try {
                Index i = Index.get(src.getURL());

                if (i == null) {
                    continue;
                }

                for (String candidate : i.findCandidates(pattern)) {
                    FileObject f = src.getFileObject(candidate);

                    result.add(f);
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        return result;
    }

    static Tree resolveAndGeneralizePattern(CompilationInfo info, int start, int end) {
        TreePath selection = selectionForExpressionHack(info, start, end);

        if (selection == null) {
            int[] statementSpan = new int[2];
            TreePathHandle statementSelection = selectionForStatementsHack(info, start, end, statementSpan);

            if (statementSelection == null) {
                return null;
            }

            selection = statementSelection.resolve(info);

            if (selection == null) {
                return null; //XXX
            }

            return generalizePattern(info, selection, statementSpan);
        } else {
            return generalizePattern(info, selection, null);
        }
    }

    private static void showDuplicates(final Collection<? extends FileObject> candidates) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DefaultListModel m = new DefaultListModel();

                for (FileObject f : candidates) {
                    m.addElement(f);
                }

                final JList l = new JList(m);
                      DialogDescriptor dd = new DialogDescriptor(new JScrollPane(l), "Possible Duplicates");
                final Dialog d = DialogDisplayer.getDefault().createDialog(dd);

                l.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            FileObject f = (FileObject) l.getSelectedValue();

                            if (f != null) {
                                UiUtils.open(f, 0);
                            }
                            
                            d.setVisible(false);
                        }
                    }
                });

                l.setCellRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                        assert value instanceof FileObject;

                        String displayName = FileUtil.getFileDisplayName((FileObject) value);

                        return super.getListCellRendererComponent(list, displayName, index, isSelected, cellHasFocus);
                    }
                });

                d.setVisible(true);
            }
        });
    }
    
    private static final class FixImpl implements Fix {

        private final FileObject file;
        private final int start;
        private final int end;

        public FixImpl(FileObject file, int start, int end) {
            this.file = file;
            this.start = start;
            this.end = end;
        }

        public String getText() {
            return "Look for Duplicates in Opened Projects";
        }

        public ChangeInfo implement() throws Exception {
            JavaSource.forFileObject(file).runUserActionTask(new Task<CompilationController>() {
                public void run(CompilationController cc) throws Exception {
                    cc.toPhase(Phase.RESOLVED);

                    showDuplicates(computeDuplicateCandidates(cc, start, end));
                }
            }, true);
            
            return null;
        }

    }

    static TreePath selectionForExpressionHack(CompilationInfo info, int start, int end) {
        //XXX: reflection
        //XXX: the IntroduceHint.validateSelection ignores expression of type void!
        try {
            Method m = IntroduceHint.class.getDeclaredMethod("validateSelection", CompilationInfo.class, int.class, int.class);

            m.setAccessible(true);

            return (TreePath) m.invoke(null, info, start, end);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        return null;
    }

    static TreePathHandle selectionForStatementsHack(CompilationInfo info, int start, int end, int[] outSpan)  {
        //XXX: reflection
        try {
            Method m = IntroduceHint.class.getDeclaredMethod("validateSelectionForIntroduceMethod", CompilationInfo.class, int.class, int.class, int[].class);

            m.setAccessible(true);

            return (TreePathHandle) m.invoke(null, info, start, end, outSpan);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (InvocationTargetException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchMethodException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }

        return null;
    }

    static Tree generalizePattern(CompilationInfo info, TreePath original, int[] statementSpan) {
        JavacTaskImpl jti = JavaSourceAccessor.getINSTANCE().getJavacTask(info);
        Context c = jti.getContext();
        TreeFactory make = TreeFactory.instance(c);
        GeneralizePattern gp = new GeneralizePattern(info, make);

        gp.scan(original, null);

        GeneralizePatternITT itt = new GeneralizePatternITT(gp.tree2Variable);

        //TODO: workaround, ImmutableTreeTranslator needs a CompilationUnitTree (rewriteChildren(BlockTree))
        //but sometimes no CompilationUnitTree (e.g. during BatchApply):
        CompilationUnitTree cut = TreeFactory.instance(c).CompilationUnit(null, Collections.<ImportTree>emptyList(), Collections.<Tree>emptyList(), null);

        itt.attach(c, new NoImports(c), cut, null);

        Tree translated = itt.translate(original.getLeaf());

        if (statementSpan != null) {
            assert translated.getKind() == Kind.BLOCK;

            List<StatementTree> newStatements = new LinkedList<StatementTree>();
            BlockTree block = (BlockTree) translated;

            if (statementSpan[0] != statementSpan[1]) {
                newStatements.add(make.ExpressionStatement(make.Identifier("$s0$")));
                newStatements.addAll(block.getStatements().subList(statementSpan[0], statementSpan[1] + 1));
                newStatements.add(make.ExpressionStatement(make.Identifier("$s1$")));

                translated = make.Block(newStatements, block.isStatic());
            } else {
                translated = block.getStatements().get(statementSpan[0]);
            }
        }

        System.err.println("translated: " + translated);
        
        return translated;
    }

    private static final class GeneralizePattern extends TreePathScanner<Void, Void> {

        private final Map<Tree, Tree> tree2Variable = new HashMap<Tree, Tree>();
        private final Map<Element, String> element2Variable = new HashMap<Element, String>();
        private final CompilationInfo info;
        private final TreeFactory make;

        private int currentVariableIndex = 0;

        public GeneralizePattern(CompilationInfo info, TreeFactory make) {
            this.info = info;
            this.make = make;
        }

        private @NonNull String getVariable(@NonNull Element el) {
            String var = element2Variable.get(el);

            if (var == null) {
                element2Variable.put(el, var = "$" + currentVariableIndex++);
            }

            return var;
        }

        private boolean shouldBeGeneralized(@NonNull Element el) {
            if (el.getModifiers().contains(Modifier.PRIVATE)) {
                return true;
            }

            switch (el.getKind()) {
                case LOCAL_VARIABLE:
                case EXCEPTION_PARAMETER:
                case PARAMETER:
                    return true;
            }

            return false;
        }
        
        @Override
        public Void visitIdentifier(IdentifierTree node, Void p) {
            Element e = info.getTrees().getElement(getCurrentPath());

            if (e != null && shouldBeGeneralized(e)) {
                tree2Variable.put(node, make.Identifier(getVariable(e)));
            }
            
            return super.visitIdentifier(node, p);
        }

        @Override
        public Void visitVariable(VariableTree node, Void p) {
            Element e = info.getTrees().getElement(getCurrentPath());

            if (e != null && shouldBeGeneralized(e)) {
                VariableTree nue = make.Variable(node.getModifiers(), getVariable(e), node.getType(), node.getInitializer());
                
                tree2Variable.put(node, nue);
            }
            
            return super.visitVariable(node, p);
        }

    }

    private static final class GeneralizePatternITT extends ImmutableTreeTranslator {

        private final Map<Tree, Tree> tree2Variable;

        public GeneralizePatternITT(Map<Tree, Tree> tree2Variable) {
            this.tree2Variable = tree2Variable;
        }

        @Override
        public Tree translate(Tree tree) {
            Tree var = tree2Variable.remove(tree);

            if (var != null) {
                return super.translate(var);
            }
            
            return super.translate(tree);
        }

    }

    private static final class NoImports extends ImportAnalysis2 {

        public NoImports(Context env) {
            super(env);
        }

        @Override
        public void classEntered(ClassTree clazz) {}

        @Override
        public void classLeft() {}

        @Override
        public ExpressionTree resolveImport(MemberSelectTree orig, Element element) {
            return orig;
        }

        @Override
        public void setCompilationUnit(CompilationUnitTree cut) {}

        private List<? extends ImportTree> imports;
        
        @Override
        public void setImports(List<? extends ImportTree> importsToAdd) {
            this.imports = importsToAdd;
        }

        @Override
        public List<? extends ImportTree> getImports() {
            return this.imports;
        }

        @Override
        public void setPackage(ExpressionTree packageNameTree) {}

    }

    @ServiceProvider(service=JavaSourceTaskFactory.class)
    public static final class FactoryImpl extends SelectionAwareJavaSourceTaskFactory {

        public FactoryImpl() {
            super(Phase.RESOLVED, Priority.LOW);
        }

        @Override
        protected CancellableTask<CompilationInfo> createTask(FileObject file) {
            return new FindDuplicates();
        }
        
    }

}
