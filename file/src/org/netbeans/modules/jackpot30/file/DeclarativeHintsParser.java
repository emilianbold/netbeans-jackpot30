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
    private final List<HintTextDescription> hints = new LinkedList<HintTextDescription>();
    private final List<ErrorDescription> errors = new LinkedList<ErrorDescription>();

    private Impl(CharSequence text, TokenSequence<DeclarativeHintTokenId> input) {
        this.text = text;
        this.input = input;
    }

    private boolean nextToken() {
        while (input.moveNext()) {
            if (id() != WHITESPACE && id() != BLOCK_COMMENT && id() != LINE_COMMENT) {
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

        List<Condition> conditions = new LinkedList<Condition>();

        if (id() == DOUBLE_COLON) {
            parseConditions(conditions);
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
                parseCondition(new LinkedList<Condition>());
            }
        }

        hints.add(new HintTextDescription(displayName, patternStart, patternEnd, conditions, targets));
    }
    
    private void parseConditions(List<Condition> conditions) {
        do {
            nextToken();
            parseCondition(conditions);
        } while (id() == AND);
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

            assert id() == INSTANCEOF;
            
            nextToken();

            int typeStart = input.offset();

            nextToken();

            int typeEnd = input.offset();

            conditions.add(new Instanceof(false, name, text.subSequence(typeStart, typeEnd).toString(), new int[] {typeStart, typeEnd}));
            return ;
        }

        int start   = input.offset();
        
        while (id() != AND && id() != LEADS_TO && id() != DOUBLE_SEMICOLON) {
            nextToken();
        }
        
        int end = input.offset();

        try {
            MethodInvocation mi = resolve(text.subSequence(start, end).toString(), not);

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

        return new Result(i.hints);
    }

    private static final ClassPath EMPTY = ClassPathSupport.createClassPath(new FileObject[0]);
    private static MethodInvocation resolve(final String invocation, final boolean not) throws IOException {
        final String[] methodName = new String[1];
        final Map<String, ParameterKind> params = new LinkedHashMap<String, ParameterKind>();
        JavaSource.create(ClasspathInfo.create(JavaPlatform.getDefault().getBootstrapLibraries(), EMPTY, EMPTY)).runUserActionTask(new Task<CompilationController>() {
            public void run(CompilationController parameter) throws Exception {
                ExpressionTree et = parameter.getTreeUtilities().parseExpression(invocation, new SourcePositions[1]);

                assert et.getKind() == Kind.METHOD_INVOCATION;

                Scope s = Hacks.constructScope(parameter, "javax.lang.model.element.Modifier", "javax.lang.model.SourceVersion");

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

        return new MethodInvocation(not, methodName[0], params);
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
        public final List<Condition> conditions;
        public final List<int[]> fixes;

        public HintTextDescription(String displayName, int textStart, int textEnd, List<Condition> conditions, List<int[]> fixes) {
            this.displayName = displayName;
            this.textStart = textStart;
            this.textEnd = textEnd;
            this.conditions = conditions;
            this.fixes = fixes;
        }

    }
}
