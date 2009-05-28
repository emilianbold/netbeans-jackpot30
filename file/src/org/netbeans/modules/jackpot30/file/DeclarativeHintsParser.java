package org.netbeans.modules.jackpot30.file;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.spi.editor.hints.ErrorDescription;

import static org.netbeans.modules.jackpot30.file.DeclarativeHintTokenId.*;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintsParser {

    private static final class Impl {
    private final TokenSequence<DeclarativeHintTokenId> input;
    private final List<HintTextDescription> hints = new LinkedList<HintTextDescription>();
    private final List<ErrorDescription> errors = new LinkedList<ErrorDescription>();

    private Impl(TokenSequence<DeclarativeHintTokenId> input) {
        this.input = input;
    }

    private boolean nextToken() {
        while (input.moveNext()) {
            if (id() != WHITESPACE) {
                return true;
            }
        }

        return false;
    }

    private Token<DeclarativeHintTokenId> token() {
        return input.token();
    }

    private DeclarativeHintTokenId id() {
        return token().id();
    }

    private boolean readToken(DeclarativeHintTokenId id) {
        if (id() == id) {
            nextToken();
            return true;
        }

        return false;
    }

    private void parseInput() {
        while (nextToken()) {
            if (token().id() == DeclarativeHintTokenId.WHITESPACE) {
                nextToken();
            }

            parseRule();
        }
    }

    private void parseRule() {
        String displayName = null;

        if (token().id() == DeclarativeHintTokenId.DISPLAY_NAME) {
            displayName = token().text().toString();

            displayName = displayName.substring(1, displayName.length() - 2);
            
            nextToken();
        }

        int patternStart = input.offset();
        
        while (   id() != LEADS_TO
               && id() != DOUBLE_COLON
               && id() != DOUBLE_SEMICOLON) {
            nextToken();
        }

        int patternEnd = input.offset();

        Map<String, int[]> variables = new HashMap<String, int[]>();

        if (id() == DOUBLE_COLON) {
            parseConditions(variables);
        }

        List<int[]> targets = new LinkedList<int[]>();

        while (id() == LEADS_TO) {
            nextToken();

            int targetStart = input.offset();

            while (   id() != LEADS_TO
                   && id() != DOUBLE_COLON
                   && id() != DOUBLE_SEMICOLON) {
                nextToken();
            }

            int targetEnd = input.offset();

            targets.add(new int[] {targetStart, targetEnd});
            
            if (id() == DOUBLE_COLON) {
                parseCondition(new HashMap<String, int[]>());
            }
        }

        hints.add(new HintTextDescription(displayName, patternStart, patternEnd, variables, targets));
    }
    
    private void parseConditions(Map<String, int[]> variables) {
        do {
            nextToken();
            parseCondition(variables);
        } while (id() == AND);
    }

    private void parseCondition(Map<String, int[]> variables) {
        assert id() == VARIABLE;

        String name = token().text().toString();

        nextToken();

        assert id() == INSTANCEOF;

        nextToken();

        int start = input.offset();

        nextToken();

        int end = input.offset();

        variables.put(name, new int[] {start, end});
    }
    }

    public Result parse(TokenSequence<DeclarativeHintTokenId> ts) {
        Impl i = new Impl(ts);

        i.parseInput();

        return new Result(i.hints);
    }
    
    public static final class Result {
        
        public final List<HintTextDescription> hints;

        public Result(List<HintTextDescription> hints) {
            this.hints = hints;
        }

    }

    public static final class HintTextDescription {
        public final String displayName;
        public final int textStart;
        public final int textEnd;
        public final Map<String, int[]> variables2Constraints;
        public final List<int[]> fixes;

        public HintTextDescription(String displayName, int textStart, int textEnd, Map<String, int[]> variables2Constraints, List<int[]> fixes) {
            this.displayName = displayName;
            this.textStart = textStart;
            this.textEnd = textEnd;
            this.variables2Constraints = variables2Constraints;
            this.fixes = fixes;
        }

    }
}
