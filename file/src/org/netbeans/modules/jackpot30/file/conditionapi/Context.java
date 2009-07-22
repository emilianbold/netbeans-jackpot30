package org.netbeans.modules.jackpot30.file.conditionapi;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.java.queries.SourceLevelQuery;
import org.netbeans.modules.jackpot30.spi.HintContext;

/**
 *
 * @author lahvac
 */
public class Context {

    private final HintContext ctx;
    private final AtomicInteger auxiliaryVariableCounter = new AtomicInteger();

    //XXX: should not be public:
    public Context(HintContext ctx) {
        this.ctx = ctx;
    }

    public @NonNull SourceVersion sourceVersion() {
        String sourceLevel = SourceLevelQuery.getSourceLevel(ctx.getInfo().getFileObject());

        if (sourceLevel == null) {
            return SourceVersion.latest(); //TODO
        }

        String[] splited = sourceLevel.split("\\.");
        String   spec    = splited[1];

        return SourceVersion.valueOf("RELEASE_"+  spec);//!!!
    }

    public @NonNull Set<Modifier> modifiers(@NonNull Variable variable) {
        final Element e = ctx.getInfo().getTrees().getElement(ctx.getVariables().get(variable.variableName));

        if (e == null) {
            return Collections.unmodifiableSet(EnumSet.noneOf(Modifier.class));
        }

        return Collections.unmodifiableSet(e.getModifiers());
    }

    public @CheckForNull ElementKind elementKind(@NonNull Variable variable) {
        final Element e = ctx.getInfo().getTrees().getElement(ctx.getVariables().get(variable.variableName));

        if (e == null) {
            return null;
        }

        return e.getKind();
    }

    public @CheckForNull Variable parent(@NonNull Variable variable) {
        TreePath tp = ctx.getVariables().get(variable.variableName);

        if (tp.getParentPath() == null) {
            return null;
        }
        
        String output = "*" + auxiliaryVariableCounter.getAndIncrement();

        ctx.getVariables().put(output, tp.getParentPath());

        return new Variable(output);
    }

    public @NonNull Variable variableForName(@NonNull String variableName) {
        if (!ctx.getVariables().containsKey(variableName)) {
            throw new IllegalStateException("Unknown variable");
        }
        
        return new Variable(variableName);
    }

    public boolean isNullLiteral(@NonNull Variable var) {
        TreePath varPath = ctx.getVariables().get(var.variableName);

        return varPath.getLeaf().getKind() == Kind.NULL_LITERAL;
    }

    static Iterable<? extends TreePath> getVariable(HintContext ctx, Variable v) {
        if (isMultistatementWildcard(v.variableName)) {
            return ctx.getMultiVariables().get(v.variableName);
        } else {
            return Collections.singletonList(ctx.getVariables().get(v.variableName));
        }
    }

    //XXX: copied from jackpot30.impl.Utilities:
    private static boolean isMultistatementWildcard(/*@NonNull */CharSequence name) {
        return name.charAt(name.length() - 1) == '$';
    }
}
