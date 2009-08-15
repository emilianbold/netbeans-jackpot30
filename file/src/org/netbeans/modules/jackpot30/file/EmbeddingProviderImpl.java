package org.netbeans.modules.jackpot30.file;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.file.Condition.Instanceof;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.FixTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.HintTextDescription;
import org.netbeans.modules.jackpot30.file.DeclarativeHintsParser.Result;
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

        result.add(snapshot.create(GLOBAL_PATTERN_PACKAGE, "text/x-java"));

        Result parsed = new DeclarativeHintsParser().parse(snapshot.getText(), ts);

        if (parsed.importsBlock != null) {
            result.add(snapshot.create(parsed.importsBlock[0], parsed.importsBlock[1] - parsed.importsBlock[0], "text/x-java"));
            result.add(snapshot.create("\n", "text/x-java"));
        }

        if (!parsed.blocks.isEmpty()) {
            for (String imp : MethodInvocationContext.AUXILIARY_IMPORTS) {
                result.add(snapshot.create(imp + "\n", "text/x-java"));
            }
        }

        result.add(snapshot.create(GLOBAL_PATTERN_CLASS, "text/x-java"));

        for (HintTextDescription hint : parsed.hints) {
            result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART1.replaceAll("\\{0\\}", "" + (index++)), "text/x-java"));

            StringBuilder builder = new StringBuilder();
            boolean first = true;

            for (Condition c : hint.conditions) {
                if (!(c instanceof Instanceof))
                    continue;

                Instanceof i = (Instanceof) c;
                
                if (!first) {
                    result.add(snapshot.create(", ", "text/x-java"));
                    builder.append(", ");
                }
                
                Embedding e1 = snapshot.create(i.constraintSpan[0], i.constraintSpan[1] - i.constraintSpan[0], "text/x-java");
                Embedding e2 = snapshot.create(" " + i.variable, "text/x-java");

                result.add(Embedding.create(Arrays.asList(e1, e2)));

                builder.append(i.constraint);
                builder.append(" " + i.variable);
                
                first = false;
            }

            result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART2, "text/x-java"));
            result.add(snapshot.create(hint.textStart, hint.textEnd - hint.textStart, "text/x-java"));
            result.add(snapshot.create(SNIPPET_PATTERN_SUFFIX, "text/x-java"));

            for (FixTextDescription f : hint.fixes) {
                int[] fixes = f.fixSpan;
                result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART1.replaceAll("\\{0\\}", "" + (index++)), "text/x-java"));
                result.add(snapshot.create(builder.toString(), "text/x-java"));
                result.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART2, "text/x-java"));
                result.add(snapshot.create(fixes[0], fixes[1] - fixes[0], "text/x-java"));
                result.add(snapshot.create(SNIPPET_PATTERN_SUFFIX, "text/x-java"));
            }
        }

        if (!parsed.blocks.isEmpty()) {
            result.add(snapshot.create(CUSTOM_CONDITIONS_VARIABLES, "text/x-java"));
        }
        
        for (int[] span : parsed.blocks) {
            result.add(snapshot.create(span[0], span[1] - span[0], "text/x-java"));
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

    private static final String GLOBAL_PATTERN_PACKAGE = "package $;\n";
    private static final String GLOBAL_PATTERN_CLASS = "class $ {\n";
    private static final String GLOBAL_PATTERN_SUFFIX = "\n}\n";
    private static final String SNIPPET_PATTERN_PREFIX_PART1 = "private void ${0}(";
    private static final String SNIPPET_PATTERN_PREFIX_PART2 = ") throws Throwable {\n";
    private static final String SNIPPET_PATTERN_SUFFIX = " ;\n}\n";

    private static final String CUSTOM_CONDITIONS_VARIABLES = "private final Context context = null;\nprivate final Matcher matcher = null;\n";

    public static final class FactoryImpl extends TaskFactory {

        @Override
        public Collection<? extends SchedulerTask> create(Snapshot snapshot) {
            return Collections.singletonList(new EmbeddingProviderImpl());
        }

    }

}
