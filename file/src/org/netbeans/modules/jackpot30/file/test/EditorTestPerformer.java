package org.netbeans.modules.jackpot30.file.test;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.modules.jackpot30.file.test.TestParser.TestCase;
import org.netbeans.modules.jackpot30.file.test.TestParser.TestResult;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.ParserResultTask;
import org.netbeans.modules.parsing.spi.Scheduler;
import org.netbeans.modules.parsing.spi.SchedulerEvent;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class EditorTestPerformer extends ParserResultTask<TestResult>{

    private static final Logger LOG = Logger.getLogger(EditorTestPerformer.class.getName());
    
    private final AtomicBoolean cancel = new AtomicBoolean();
    
    @Override
    public void run(TestResult result, SchedulerEvent event) {
        TestCase[] tests = result.getTests();
        FileObject file = result.getSnapshot().getSource().getFileObject();
        ClassPath cp = ClassPath.getClassPath(file, ClassPath.SOURCE);
        String resourceName = cp != null ? cp.getResourceName(file) : null;

        if (cp == null) {
            LOG.log(Level.FINE, "cp==null");
            return ;
        }

        //XXX: lookup throws the hint file
        String ruleFileName = resourceName.substring(0, resourceName.length() - ".test".length()) + ".hint";

        FileObject ruleFile = cp.findResource(ruleFileName);

        if (ruleFile == null) {
            LOG.log(Level.FINE, "runFile==null");
            return ;
        }

        Document doc = result.getSnapshot().getSource().getDocument(false);

        if (!(doc instanceof StyledDocument)) {
            return ;
        }

        StyledDocument sdoc = (StyledDocument) doc;
        
        try {
            List<ErrorDescription> errors = new LinkedList<ErrorDescription>();

            for (Entry<TestCase, Collection<String>> e : TestPerformer.performTest(ruleFile, file, tests, cancel).entrySet()) {
                TestCase tc = e.getKey();
                String[] golden = tc.getResults();
                String[] real = e.getValue().toArray(new String[0]);

                if (golden.length != real.length) {
                    int line = NbDocument.findLineNumber(sdoc, tc.getTestCaseStart()) + 1;
                    ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(Severity.ERROR, "Incorrect number of outputs, expected: " + golden.length + ", was: " + real.length, doc, line);

                    errors.add(ed);
                }
                
                for (int cntr = 0; cntr < Math.min(golden.length, real.length); cntr++) {
                    String goldenText = golden[cntr];
                    String realText   = real[cntr];

                    if (!TestPerformer.normalize(goldenText).equals(TestPerformer.normalize(realText))) {
                        int line = NbDocument.findLineNumber(sdoc, tc.getResultsStart()[cntr]);
                        List<Fix> fixes = Collections.<Fix>singletonList(new FixImpl(tc.getResultsStart()[cntr], tc.getResultsEnd()[cntr], sdoc, realText));
                        ErrorDescription ed = ErrorDescriptionFactory.createErrorDescription(Severity.ERROR, "Incorrect output", fixes, doc, line);

                        errors.add(ed);
                    }
                }
            }

            HintsController.setErrors(doc, EditorTestPerformer.class.getName(), errors);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public Class<? extends Scheduler> getSchedulerClass() {
        return Scheduler.EDITOR_SENSITIVE_TASK_SCHEDULER;
    }

    @Override
    public void cancel() {
    }

    public static final class FactoryImpl extends TaskFactory {

        @Override
        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singleton(new EditorTestPerformer());
        }
        
    }

    private static final class FixImpl implements Fix {

        //XXX: Position!!
        private final int start;
        private final int end;
        private final StyledDocument doc;
        private final String text;

        public FixImpl(int start, int end, StyledDocument doc, String text) {
            this.start = start;
            this.end = end;
            this.doc = doc;
            this.text = text;
        }

        public String getText() {
            return "Put actual output into golden section";
        }

        public ChangeInfo implement() throws Exception {
            NbDocument.runAtomic(doc, new Runnable() {
                public void run() {
                    try {
                        doc.remove(start, end - start);
                        doc.insertString(start, text, null);
                    } catch (BadLocationException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            });
            
            return null;
        }
        
    }
}
