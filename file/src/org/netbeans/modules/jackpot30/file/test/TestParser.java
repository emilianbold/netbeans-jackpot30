package org.netbeans.modules.jackpot30.file.test;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.api.Task;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.ParserFactory;
import org.netbeans.modules.parsing.spi.SourceModificationEvent;

/**
 *
 * @author lahvac
 */
public class TestParser extends Parser {

    private TestResult result;
    
    @Override
    public void parse(Snapshot snapshot, Task task, SourceModificationEvent event) throws ParseException {
        System.err.println("parse");
        result = new TestResult(snapshot, parse(snapshot.getText().toString()));
    }

    @Override
    public Result getResult(Task task) throws ParseException {
        System.err.println("getResult");
        return result;
    }

    @Override
    public void cancel() {
    }

    @Override
    public void addChangeListener(ChangeListener changeListener) {}

    @Override
    public void removeChangeListener(ChangeListener changeListener) {}

    public static final class TestResult extends Result {

        private final TestCase[] tests;

        public TestResult(Snapshot snapshot, TestCase[] tests) {
            super(snapshot);
            this.tests = tests;
        }

        public TestCase[] getTests() {
            return tests;
        }

        @Override
        protected void invalidate() {}
        
    }

    public static final class FactoryImpl extends ParserFactory {

        @Override
        public Parser createParser(Collection<Snapshot> snapshots) {
            System.err.println("create");
            return new TestParser();
        }
        
    }

    public static TestCase[] parse(String tests) {
        //TODO: efficiency?
        List<TestCase> result = new LinkedList<TestCase>();
        int codeIndex = -1;
        int testCaseIndex = -1;
        String lastName = null;
        Matcher m = TEST_CASE_HEADER.matcher(tests);

        while (m.find()) {
            if (testCaseIndex >= 0) {
                result.add(handleTestCase(testCaseIndex, lastName, codeIndex, tests.substring(codeIndex, m.start())));
            }

            codeIndex = m.end();
            testCaseIndex = m.start();
            lastName = m.group(1);
        }

        if (testCaseIndex >= 0) {
            result.add(handleTestCase(testCaseIndex, lastName, codeIndex, tests.substring(codeIndex)));
        }

        return result.toArray(new TestCase[result.size()]);
    }

    private static TestCase handleTestCase(int testCaseIndex, String testName, int codeIndex, String testCase) {
        Matcher m = LEADS_TO_HEADER.matcher(testCase);
        String code = null;
        List<String> results = new LinkedList<String>();
        List<Integer> startIndices = new LinkedList<Integer>();
        List<Integer> endIndices = new LinkedList<Integer>();
        int lastStartIndex = -1;
        int lastIndex = -1;

        while (m.find()) {
            if (code == null) {
                code = testCase.substring(0, m.start());
            } else {
                results.add(testCase.substring(lastIndex, m.start()));
                if (!startIndices.isEmpty()) {
                    endIndices.add(startIndices.get(startIndices.size() - 1));
                }
                startIndices.add(lastIndex);
            }

            lastStartIndex = m.start();
            lastIndex = m.end();
        }

        if (code == null) {
            code = testCase.substring(0, m.start());
        } else {
            results.add(testCase.substring(lastIndex));
            if (!startIndices.isEmpty()) {
                endIndices.add(startIndices.get(startIndices.size() - 1));
            }
            startIndices.add(lastIndex);
        }

        endIndices.add(testCase.length());

        int[] startIndicesArr = new int[startIndices.size()];
        int[] endIndicesArr = new int[endIndices.size()];

        assert startIndicesArr.length == endIndicesArr.length;

        int c = 0;

        for (Integer i : startIndices) {
            startIndicesArr[c++] = codeIndex + i;
        }

        c = 0;
        
        for (Integer i : endIndices) {
            endIndicesArr[c++] = codeIndex + i;
        }
        
        return new TestCase(testName, code, results.toArray(new String[0]), testCaseIndex, codeIndex, startIndicesArr, endIndicesArr);
    }

    private static final Pattern TEST_CASE_HEADER = Pattern.compile("%%TestCase[ \t]+(.*)\n");
    private static final Pattern LEADS_TO_HEADER = Pattern.compile("%%=>\n");

    public static final class TestCase {
        private final String name;
        private final String code;
        private final String[] results;

        private final int testCaseStart;
        private final int codeStart;
        private final int[] resultsStart;
        private final int[] resultsEnd;

        private TestCase(String name, String code, String[] results, int testCaseStart, int codeStart, int[] resultsStart, int[] resultsEnd) {
            this.name = name;
            this.code = code;
            this.results = results;
            this.testCaseStart = testCaseStart;
            this.codeStart = codeStart;
            this.resultsStart = resultsStart;
            this.resultsEnd = resultsEnd;
        }

        public String getCode() {
            return code;
        }

        public int getCodeStart() {
            return codeStart;
        }

        public String getName() {
            return name;
        }

        public String[] getResults() {
            return results;
        }

        public int[] getResultsStart() {
            return resultsStart;
        }

        public int[] getResultsEnd() {
            return resultsEnd;
        }

        public int getTestCaseStart() {
            return testCaseStart;
        }

        @Override
        public String toString() {
            return name + ":" + code + ":" + Arrays.toString(results) + ":" + testCaseStart + ":" + codeStart + ":" + Arrays.toString(resultsStart);
        }

    }
}
