package org.netbeans.modules.jackpot30.file;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.lexer.PartType;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
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

        List<Token<DeclarativeHintTokenId>> parts = new LinkedList<Token<DeclarativeHintTokenId>>();
        List<Embedding> declarations = new LinkedList<Embedding>();
        Token<DeclarativeHintTokenId> previous = null;

        while (ts.moveNext()) {
            Token<DeclarativeHintTokenId> t = ts.token();
            
            if (t.id() == DeclarativeHintTokenId.PATTERN) {
                parts.add(t);

                if (t.partType() != PartType.END && t.partType() != PartType.COMPLETE) {
                    previous = t;
                    continue;
                }

                List<Embedding> embeddingParts = new LinkedList<Embedding>();

                String prefix = SNIPPET_PATTERN_PREFIX_PART1;

                prefix = prefix.replaceAll("\\{0\\}", "" + (index++));

                embeddingParts.add(snapshot.create(prefix, "text/x-java"));

                boolean first = true;

                for (Embedding d : declarations) {
                    if (!first) {
                        embeddingParts.add(snapshot.create(", ", "text/x-java"));
                    }
                    first = false;
                    embeddingParts.add(d);
                }

                embeddingParts.add(snapshot.create(SNIPPET_PATTERN_PREFIX_PART2, "text/x-java"));

                for (Token<DeclarativeHintTokenId> tokenPart : parts) {
                    embeddingParts.add(snapshot.create(tokenPart.offset(null), tokenPart.length(), "text/x-java"));
                }
                
                embeddingParts.add(snapshot.create(SNIPPET_PATTERN_SUFFIX, "text/x-java"));

                Embedding e = Embedding.create(embeddingParts);

                result.add(e);

                parts.clear();
            }

            if (t.id() == DeclarativeHintTokenId.TYPE && previous != null && previous.id() == DeclarativeHintTokenId.PATTERN) {
                Matcher m = VARIABLE_RE.matcher(previous.text().toString());

                if (m.matches()) {
                    String text = t.text().toString();

                    Embedding e1 = snapshot.create(ts.offset() + 1, text.length() - 2, "text/x-java");
                    Embedding e2 = snapshot.create(" " + m.group(1), "text/x-java");

                    declarations.add(Embedding.create(Arrays.asList(e1, e2)));
                }
            }

            if (t.id() == DeclarativeHintTokenId.DOUBLE_SEMICOLON) {
                declarations.clear();
            }

            previous = t;
        }

        if (result.isEmpty()) {
            return Collections.emptyList();
        }

        result.add(0, snapshot.create(GLOBAL_PATTERN_PREFIX, "text/x-java"));
        result.add(snapshot.create(GLOBAL_PATTERN_SUFFIX, "text/x-java"));
        
        return Collections.singletonList(Embedding.create(result));
    }

    private static final Pattern VARIABLE_RE = Pattern.compile(".*(\\$[A-Za-z0-9_]+)");

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
