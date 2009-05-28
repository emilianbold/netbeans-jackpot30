package org.netbeans.modules.jackpot30.file;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.parsing.api.Embedding;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.EmbeddingProvider;
import org.netbeans.modules.parsing.spi.SchedulerTask;
import org.netbeans.modules.parsing.spi.TaskFactory;

/**
 *
 * @author lahvac
 */
public class EmbeddingProviderImpl extends EmbeddingProvider {

    @Override
    public List<Embedding> getEmbeddings(Snapshot snapshot) {
        int index = 0;
        List<Embedding> result = new LinkedList<Embedding>();
        TokenSequence<DeclarativeHintTokenId> ts = snapshot.getTokenHierarchy().tokenSequence(DeclarativeHintTokenId.language());

        result.add(snapshot.create(GLOBAL_PATTERN_PREFIX, "text/x-java"));

        for (HintTextDescription hint : new DeclarativeHintsParser().parse(ts).hints) {
            result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART1.replaceAll("\\{0\\}", "" + (index++)), "text/x-java"));

            StringBuilder builder = new StringBuilder();
            boolean first = true;

            for (Entry<String, int[]> e : hint.variables2Constraints.entrySet()) {
                if (!first) {
                    result.add(snapshot.create(", ", "text/x-java"));
                    builder.append(", ");
                }
                
                Embedding e1 = snapshot.create(e.getValue()[0], e.getValue()[1] - e.getValue()[0], "text/x-java");
                Embedding e2 = snapshot.create(" " + e.getKey(), "text/x-java");

                result.add(Embedding.create(Arrays.asList(e1, e2)));

                builder.append(snapshot.getText().subSequence(e.getValue()[0], e.getValue()[1]).toString());
                builder.append(" " + e.getKey());
                
                first = false;
            }

            result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART2, "text/x-java"));
            result.add(snapshot.create(hint.textStart, hint.textEnd - hint.textStart, "text/x-java"));
            result.add(snapshot.create(SNIPPET_PATTERN_SUFFIX, "text/x-java"));

            for (int[] fixes : hint.fixes) {
                result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART1.replaceAll("\\{0\\}", "" + (index++)), "text/x-java"));
                result.add(snapshot.create(builder.toString(), "text/x-java"));
                result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART2, "text/x-java"));
                result.add(snapshot.create(fixes[0], fixes[1] - fixes[0], "text/x-java"));
                result.add(snapshot.create(SNIPPET_PATTERN_SUFFIX, "text/x-java"));
            }
        }

        result.add(snapshot.create(GLOBAL_PATTERN_SUFFIX, "text/x-java"));

        return Collections.singletonList(Embedding.create(result));
    }
    
    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void cancel() {}

    private static final String GLOBAL_PATTERN_PREFIX = "package $; class $ {\n";
    private static final String GLOBAL_PATTERN_SUFFIX = "\n}\n";
    private static final String SNIPPET_PATTERN_PREFIX_PART1 = "private void ${0}(";
    private static final String SNIPPET_PATTERN_PREFIX_PART2 = ") throws Throwable {\n";
    private static final String SNIPPET_PATTERN_SUFFIX = " ;\n}\n";

    public static final class FactoryImpl extends TaskFactory {

        @Override
        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singletonList(new EmbeddingProviderImpl());
        }

    }

}
