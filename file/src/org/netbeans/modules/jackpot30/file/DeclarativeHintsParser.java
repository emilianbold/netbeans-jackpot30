package org.netbeans.modules.jackpot30.file;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.file.Condition.Instanceof;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation.ParameterKind;
import org.netbeans.modules.jackpot30.spi.Hacks;
import org.netbeans.spi.editor.hints.ErrorDescription;

import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import static org.netbeans.modules.jackpot30.file.DeclarativeHintTokenId.*;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintsParser {

    private static final class Impl {
    private final CharSequence text;
    private final TokenSequence<DeclarativeHintTokenId> input;
    private       String importsBlockCode;
    private       int[] importsBlockSpan;
    private final List<HintTextDescription> hints = new LinkedList<HintTextDescription>();
    private final List<String> blocksCode = new LinkedList<String>();
    private final List<int[]> blocksSpan = new LinkedList<int[]>();
    private final List<ErrorDescription> errors = new LinkedList<ErrorDescription>();
    private final MethodInvocationContext mic;

    private Impl(CharSequence text, TokenSequence<DeclarativeHintTokenId> input) {
        this.text = text;
        this.input = input;
        this.mic = new MethodInvocationContext();
    }

    private boolean nextToken() {
        while (input.moveNext()) {
            if (id() != WHITESPACE && id() != BLOCK_COMMENT && id() != LINE_COMMENT) {
                return true;
            }
        }

        eof = true;
        
        return false;
    }

    private boolean eof;
        
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
        boolean wasFirstRule = false;
        
        while (nextToken()) {
            if (id() == JAVA_BLOCK) {
                String text = token().text().toString();
                text = text.substring(2, text.length() - 2);
                int[] span = new int[] {token().offset(null) + 2, token().offset(null) + token().length() - 2};
                if (importsBlockCode == null && !wasFirstRule) {
                    importsBlockCode = text;
                    importsBlockSpan = span;
                } else {
                    blocksCode.add(text);
                    blocksSpan.add(span);
                }
            } else {
                parseRule();
                wasFirstRule = true;
            }
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
               && id() != DOUBLE_SEMICOLON
               && !eof) {
            nextToken();
        }

        if (eof) {
            //XXX: should report an error
            return ;
        }

        int patternEnd = input.offset();

        List<Condition> conditions = new LinkedList<Condition>();

        if (id() == DOUBLE_COLON) {
            parseConditions(conditions);
        }

        List<FixTextDescription> targets = new LinkedList<FixTextDescription>();

        while (id() == LEADS_TO && !eof) {
            nextToken();

            int targetStart = input.offset();

            while (   id() != LEADS_TO
                   && id() != DOUBLE_COLON
                   && id() != DOUBLE_SEMICOLON
                   && !eof) {
                nextToken();
            }

            int targetEnd = input.offset();
            int[] span = new int[] {targetStart, targetEnd};
            List<Condition> fixConditions = new LinkedList<Condition>();

            if (id() == DOUBLE_COLON) {
                parseConditions(fixConditions);
            }

            targets.add(new FixTextDescription(span, fixConditions));
        }

        hints.add(new HintTextDescription(displayName, patternStart, patternEnd, conditions, targets));
    }
    
    private void parseConditions(List<Condition> conditions) {
        do {
            nextToken();
            parseCondition(conditions);
        } while (id() == AND && !eof);
    }

    private void parseCondition(List<Condition> conditions) {
        boolean not = false;

        if (id() == NOT) {
            not = true;
            nextToken();
        }
        
        if (id() == VARIABLE) {
            String name = token().text().toString();

            nextToken();

            if (id() != INSTANCEOF) {
                //XXX: report an error
                return ;
            }
            
            nextToken();

            int typeStart = input.offset();

            nextToken();

            int typeEnd = input.offset();

            conditions.add(new Instanceof(not, name, text.subSequence(typeStart, typeEnd).toString(), new int[] {typeStart, typeEnd}));
            return ;
        }

        int start   = input.offset();
        
        while (id() != AND && id() != LEADS_TO && id() != DOUBLE_SEMICOLON && !eof) {
            nextToken();
        }
        
        int end = input.offset();

        try {
            MethodInvocation mi = resolve(mic, text.subSequence(start, end).toString(), not);

            if (mi != null) {
                conditions.add(mi);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    }

    public Result parse(CharSequence text, TokenSequence<DeclarativeHintTokenId> ts) {
        Impl i = new Impl(text, ts);

        i.parseInput();
        i.mic.setCode(i.importsBlockCode, i.blocksCode);

        return new Result(i.importsBlockSpan, i.hints, i.blocksSpan);
    }

    private static final ClassPath EMPTY = ClassPathSupport.createClassPath(new FileObject[0]);
    private static MethodInvocation resolve(MethodInvocationContext mic, final String invocation, final boolean not) throws IOException {
        final String[] methodName = new String[1];
        final Map<String, ParameterKind> params = new LinkedHashMap<String, ParameterKind>();
        JavaSource.create(ClasspathInfo.create(JavaPlatform.getDefault().getBootstrapLibraries(), EMPTY, EMPTY)).runUserActionTask(new Task<CompilationController>() {
            public void run(CompilationController parameter) throws Exception {
                ExpressionTree et = parameter.getTreeUtilities().parseExpression(invocation, new SourcePositions[1]);

                if (et.getKind() != Kind.METHOD_INVOCATION) {
                    //XXX: report an error
                    return ;
                }

                Scope s = Hacks.constructScope(parameter, "javax.lang.model.SourceVersion", "javax.lang.model.element.Modifier", "javax.lang.model.element.ElementKind");

                parameter.getTreeUtilities().attributeTree(et, s);

                MethodInvocationTree mit = (MethodInvocationTree) et;

                methodName[0] = ((IdentifierTree) mit.getMethodSelect()).getName().toString();

                for (ExpressionTree t : mit.getArguments()) {
                    switch (t.getKind()) {
                        case STRING_LITERAL:
                            params.put(((LiteralTree) t).getValue().toString(), ParameterKind.STRING_LITERAL);
                            break;
                        case IDENTIFIER:
                            String name = ((IdentifierTree) t).getName().toString();

                            if (name.startsWith("$")) {
                                params.put(name, ParameterKind.VARIABLE);
                                break;
                            }
                        case MEMBER_SELECT:
                            TreePath tp = parameter.getTrees().getPath(s.getEnclosingClass());
                            Element e = parameter.getTrees().getElement(new TreePath(tp, t));

                            assert e.getKind() == ElementKind.ENUM_CONSTANT : e.getKind();

                            params.put(((TypeElement) e.getEnclosingElement()).getQualifiedName().toString() + "." + e.getSimpleName().toString(), ParameterKind.ENUM_CONSTANT);
                            break;
                    }
                }
            }
        }, true);

        if (methodName[0] == null) {
            return null;
        }
        
        return new MethodInvocation(not, methodName[0], params, mic);
    }
    
    public static final class Result {

        public final int[] importsBlock;
        public final List<HintTextDescription> hints;
        public final List<int[]> blocks;

        public Result(int[] importsBlock, List<HintTextDescription> hints, List<int[]> blocks) {
            this.importsBlock = importsBlock;
            this.hints = hints;
            this.blocks = blocks;
        }

    }

    public static final class HintTextDescription {
        public final String displayName;
        public final int textStart;
        public final int textEnd;
        public final List<Condition> conditions;
        public final List<FixTextDescription> fixes;

        public HintTextDescription(String displayName, int textStart, int textEnd, List<Condition> conditions, List<FixTextDescription> fixes) {
            this.displayName = displayName;
            this.textStart = textStart;
            this.textEnd = textEnd;
            this.conditions = conditions;
            this.fixes = fixes;
        }

    }

    public static final class FixTextDescription {
        public final int[] fixSpan;
        public final List<Condition> conditions;

        public FixTextDescription(int[] fixSpan, List<Condition> conditions) {
            this.fixSpan = fixSpan;
            this.conditions = conditions;
        }
    }
}
