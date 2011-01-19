/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.file;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.CodeSource;
import javax.lang.model.element.Modifier;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.jackpot30.file.Condition.False;
import org.netbeans.modules.jackpot30.file.Condition.Instanceof;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation.ParameterKind;
import org.netbeans.modules.jackpot30.file.Condition.Otherwise;
import org.netbeans.modules.jackpot30.spi.Hacks;
import org.netbeans.modules.jackpot30.spi.HintDescription.Literal;
import org.netbeans.modules.jackpot30.spi.HintDescription.MarkCondition;
import org.netbeans.modules.jackpot30.spi.HintDescription.Operator;
import org.netbeans.modules.jackpot30.spi.HintDescription.Selector;
import org.netbeans.modules.jackpot30.spi.HintDescription.Value;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Severity;

import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import static org.netbeans.modules.jackpot30.file.DeclarativeHintTokenId.*;

/**
 *
 * @author lahvac
 */
public class DeclarativeHintsParser {

    //used by tests:
    static Class<?>[] auxConditionClasses;

    private static final class Impl {
    private final FileObject file;
    private final CharSequence text;
    private final TokenSequence<DeclarativeHintTokenId> input;
    private final Map<String, String> options = new HashMap<String, String>();
    private       String importsBlockCode;
    private       int[] importsBlockSpan;
    private final List<HintTextDescription> hints = new LinkedList<HintTextDescription>();
    private final List<String> blocksCode = new LinkedList<String>();
    private final List<int[]> blocksSpan = new LinkedList<int[]>();
    private final List<ErrorDescription> errors = new LinkedList<ErrorDescription>();
    private final MethodInvocationContext mic;
    private final Map<MethodInvocation, int[]> allMIConditions = new IdentityHashMap<MethodInvocation, int[]>();

    private Impl(FileObject file, CharSequence text, TokenSequence<DeclarativeHintTokenId> input) {
        this.file = file;
        this.text = text;
        this.input = input;
        this.mic = new MethodInvocationContext();

        if (auxConditionClasses != null) {
            this.mic.ruleUtilities.addAll(Arrays.asList(auxConditionClasses));
        }
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

    private void readUntil(DeclarativeHintTokenId first, DeclarativeHintTokenId... other) {
        Set<DeclarativeHintTokenId> ids = EnumSet.of(first, other);

        while (   !ids.contains(id())
               && !eof)
            nextToken();

        //XXX: iff eof produce an error
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
            }

            wasFirstRule = true;
        }

        mic.setCode(importsBlockCode, blocksCode);
        input.moveStart();
        eof = false;
        
        while (nextToken()) {
            if (id() == JAVA_BLOCK) {
                continue;
            }
            
            maybeParseOptions(options);
            parseRule();
        }
    }

    private void parseRule() {
        String displayName = parseDisplayName();
        int patternStart = input.offset();
        
        readUntil(LEADS_TO, DOUBLE_COLON, DOUBLE_SEMICOLON, OPTIONS);

        if (eof) {
            //XXX: should report an error
            return ;
        }

        int patternEnd = input.offset();

        Map<String, String> ruleOptions = new HashMap<String, String>();

        maybeParseOptions(ruleOptions);

        List<Condition> conditions = new LinkedList<Condition>();
        List<int[]> conditionsSpans = new LinkedList<int[]>();

        if (id() == DOUBLE_COLON) {
            parseConditions(conditions, conditionsSpans);
        }

        List<FixTextDescription> targets = new LinkedList<FixTextDescription>();

        while (id() == LEADS_TO && !eof) {
            nextToken();

            String fixDisplayName = parseDisplayName();

            int targetStart = input.offset();

            readUntil(LEADS_TO, DOUBLE_COLON, DOUBLE_SEMICOLON, OPTIONS);

            int targetEnd = input.offset();
            
            Map<String, String> fixOptions = new HashMap<String, String>();

            maybeParseOptions(fixOptions);

            int[] span = new int[] {targetStart, targetEnd};
            List<Condition> fixConditions = new LinkedList<Condition>();
            List<int[]> fixConditionSpans = new LinkedList<int[]>();

            if (id() == DOUBLE_COLON) {
                parseConditions(fixConditions, fixConditionSpans);
            }

            targets.add(new FixTextDescription(fixDisplayName, span, fixConditions, fixConditionSpans, fixOptions));
        }

        hints.add(new HintTextDescription(displayName, patternStart, patternEnd, conditions, conditionsSpans, targets, ruleOptions));
    }
    
    private void parseConditions(List<Condition> conditions, List<int[]> spans) {
        do {
            nextToken();
            parseCondition(conditions, spans);
        } while (id() == AND && !eof);
    }

    private void parseCondition(List<Condition> conditions, List<int[]> spans) {
        int conditionStart = input.offset();

        if (id() == OTHERWISE) {
            nextToken();
            conditions.add(new Otherwise());
            spans.add(new int[] {conditionStart, input.offset()});
            return ;
        }

        boolean not = false;

        if (id() == NOT) {
            not = true;
            nextToken();
        }
        
        if (id() == VARIABLE) {
            String name = token().text().toString();

            nextToken();

            if (id() == DOT) {
                parseMarkConditionRest(name, conditionStart, not, conditions, spans);
                return ;
            }

            if (id() != INSTANCEOF) {
                //XXX: report an error
                return ;
            }
            
            nextToken();

            int typeStart = input.offset();

            readUntil(LEADS_TO, AND, DOUBLE_SEMICOLON);

            int typeEnd = input.offset();

            conditions.add(new Instanceof(not, name, text.subSequence(typeStart, typeEnd).toString(), new int[] {typeStart, typeEnd}));
            spans.add(new int[] {conditionStart, typeEnd});
            return ;
        }

        int start   = input.offset();
        
        readUntil(LEADS_TO, AND, DOUBLE_SEMICOLON);
        
        int end = input.offset();

        try {
            Condition mi = resolve(mic, text.subSequence(start, end).toString(), not, conditionStart, file, errors);
            int[] span = new int[]{conditionStart, end};

            if ((mi instanceof MethodInvocation) && !((MethodInvocation) mi).link()) {
                if (file != null) {
                    errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.ERROR, "Cannot resolve method", file, span[0], span[1]));
                }

                mi = new False();
            }

            conditions.add(mi);
            spans.add(span);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void maybeParseOptions(Map<String, String> to) {
        if (id() != OPTIONS)
            return ;

        String opts = token().text().toString();

        if (opts.length() > 2) {
            parseOptions(opts.substring(2, opts.length() - 1), to);
        } else {
            //XXX: produce error
        }

        nextToken();
    }
    
    private String parseDisplayName() {
        if (token().id() == DeclarativeHintTokenId.CHAR_LITERAL || token().id() == DeclarativeHintTokenId.STRING_LITERAL) {
            Token<DeclarativeHintTokenId> t = token();

            if (input.moveNext()) {
                if (input.token().id() == DeclarativeHintTokenId.COLON) {
                    String displayName = t.text().subSequence(1, t.text().length() - 1).toString();

                    nextToken();
                    return displayName;
                } else {
                    input.movePrevious();
                }
            }
        }

        return null;
    }

    private void parseMarkConditionRest(String name, int conditionStart, boolean not, List<Condition> conditions, List<int[]> spans) {
        Value left = parseSelectorRest(name);
        Operator op;

        switch (id()) {
            case EQUALS:
                op = Operator.EQUALS;
                break;
            case NOT_EQUALS:
                op = Operator.NOT_EQUALS;
                break;
            case ASSIGN:
                op = Operator.ASSIGN;
                break;
            default:
                op = null;
                break;
        }

        Value right;

        if (op != null) {
            nextToken();
            right = parseSelectorOrValue();
        } else {
            op = not ? Operator.NOT_EQUALS : Operator.EQUALS;
            right = new Literal(true);
        }

        MarkCondition cond = new MarkCondition(left, op, right);

        conditions.add(new Condition.MarkCondition(cond));
        spans.add(new int[] {conditionStart, input.offset()});
    }
    
    private Selector parseSelectorRest(String name) {
        List<String> selected = new LinkedList<String>();

        selected.add(name);

        while (id() == DOT) {
            nextToken();

            if (id() != IDENTIFIER) {
                //TODO: report error:
                selected.add("<error>");
                break;
            }

            selected.add(token().text().toString());
            nextToken();
        }

        return new Selector(selected);
    }

    private Value parseSelectorOrValue() {
        if (id() != VARIABLE && id() != IDENTIFIER) {
            //XXX: produce error
            return new Literal(false);
        }

        String name = token().text().toString();

        if ("true".contentEquals(name) || "false".contentEquals(name)) {
            nextToken();
            return new Literal(Boolean.valueOf(name));
        }

        nextToken();

        if (id() == DOT) {
            return parseSelectorRest(name);
        } else {
            return new Selector(name);
        }
    }
    }

    private static final Pattern OPTION = Pattern.compile("([^=]+)=(([^\"].*?)|(\".*?\")),");
    
    static void parseOptions(String options, Map<String, String> to) {
        Matcher m = OPTION.matcher(options);
        int end = 0;

        while (m.find()) {
            to.put(m.group(1), unquote(m.group(2)));
            end = m.end();
        }

        String[] keyValue = options.substring(end).split("=");

        if (keyValue.length == 1) {
            //TODO: semantics? error?
            to.put(keyValue[0], "");
        } else {
            to.put(keyValue[0], unquote(keyValue[1]));
        }
    }
    
    private static String unquote(String what) {
        if (what.length() > 2 && what.charAt(0) == '"' && what.charAt(what.length() - 1) == '"')
            return what.substring(1, what.length() - 1);
        else
            return what;
    }
    
    public Result parse(@NullAllowed FileObject file, CharSequence text, TokenSequence<DeclarativeHintTokenId> ts) {
        Impl i = new Impl(file, text, ts);

        i.parseInput();

        return new Result(i.options, i.importsBlockSpan, i.hints, i.blocksSpan, i.errors);
    }

    private static final ClassPath EMPTY = ClassPathSupport.createClassPath(new FileObject[0]);
    private static @NonNull Condition resolve(MethodInvocationContext mic, final String invocation, final boolean not, final int offset, final FileObject file, final List<ErrorDescription> errors) throws IOException {
        final String[] methodName = new String[1];
        final Map<String, ParameterKind> params = new LinkedHashMap<String, ParameterKind>();
        CodeSource codeSource = Modifier.class.getProtectionDomain().getCodeSource();
        URL javacApiJar = codeSource != null ? codeSource.getLocation() : null;
        ClasspathInfo cpInfo = Hacks.createUniversalCPInfo();
         if (javacApiJar != null) {
            Logger.getLogger(DeclarativeHintsParser.class.getName()).log(Level.FINE, "javacApiJar={0}", javacApiJar);
            File aj = FileUtil.archiveOrDirForURL(javacApiJar);
            cpInfo = ClasspathInfo.create(ClassPathSupport.createProxyClassPath(ClassPathSupport.createClassPath(FileUtil.urlForArchiveOrDir(aj)), cpInfo.getClassPath(ClasspathInfo.PathKind.BOOT)), ClassPath.EMPTY, ClassPath.EMPTY);
         }
        JavaSource.create(cpInfo).runUserActionTask(new Task<CompilationController>() {
            @SuppressWarnings("fallthrough")
            public void run(CompilationController parameter) throws Exception {
                SourcePositions[] positions = new SourcePositions[1];
                ExpressionTree et = parameter.getTreeUtilities().parseExpression(invocation, positions);

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

                            if (e.getKind() != ElementKind.ENUM_CONSTANT) {
                                int start = (int) positions[0].getStartPosition(null, t) + offset;
                                int end = (int) positions[0].getEndPosition(null, t) + offset;
                                errors.add(ErrorDescriptionFactory.createErrorDescription(Severity.ERROR, "Cannot resolve enum constant", file, start, end));
                                break;
                            }

                            params.put(((TypeElement) e.getEnclosingElement()).getQualifiedName().toString() + "." + e.getSimpleName().toString(), ParameterKind.ENUM_CONSTANT);
                            break;
                    }
                }
            }
        }, true);

        if (methodName[0] == null) {
            return new False();
        }
        
        return new MethodInvocation(not, methodName[0], params, mic);
    }

    public static final class Result {

        public final Map<String, String> options;
        public final int[] importsBlock;
        public final List<HintTextDescription> hints;
        public final List<int[]> blocks;
        public final List<ErrorDescription> errors;

        public Result(Map<String, String> options, int[] importsBlock, List<HintTextDescription> hints, List<int[]> blocks, List<ErrorDescription> errors) {
            this.options = options;
            this.importsBlock = importsBlock;
            this.hints = hints;
            this.blocks = blocks;
            this.errors = errors;
        }

    }

    public static final class HintTextDescription {
        public final String displayName;
        public final int textStart;
        public final int textEnd;
        public final List<Condition> conditions;
        public final List<int[]> conditionSpans;
        public final List<FixTextDescription> fixes;
        public final Map<String, String> options;

        public HintTextDescription(String displayName, int textStart, int textEnd, List<Condition> conditions, List<int[]> conditionSpans, List<FixTextDescription> fixes, Map<String, String> options) {
            this.displayName = displayName;
            this.textStart = textStart;
            this.textEnd = textEnd;
            this.conditions = conditions;
            this.conditionSpans = conditionSpans;
            this.fixes = fixes;
            this.options = options;
        }

    }

    public static final class FixTextDescription {
        public final String displayName;
        public final int[] fixSpan;
        public final List<Condition> conditions;
        public final List<int[]> conditionSpans;
        public final Map<String, String> options;

        public FixTextDescription(String displayName, int[] fixSpan, List<Condition> conditions, List<int[]> conditionSpans, Map<String, String> options) {
            this.displayName = displayName;
            this.fixSpan = fixSpan;
            this.conditions = conditions;
            this.conditionSpans = conditionSpans;
            this.options = options;
        }
    }
}
