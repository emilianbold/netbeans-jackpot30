package org.netbeans.modules.jackpot30.file;

import com.sun.source.util.TreePath;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public abstract class Condition {

    public final boolean not;

    private Condition(boolean not) {
        this.not = not;
    }

    public abstract boolean holds(HintContext ctx, boolean global);

    @Override
    public abstract String toString();
    
    public static final class Instanceof extends Condition {

        public final String variable;
        public final String constraint;
        public final int[]  constraintSpan;

        public Instanceof(boolean not, String variable, String constraint, int[]  constraintSpan) {
            super(not);
            this.variable = variable;
            this.constraint = constraint;
            this.constraintSpan = constraintSpan;
        }

        @Override
        public boolean holds(HintContext ctx, boolean global) {
            if (global && !not) {
                //if this is a global condition, not == false, then the computation should always lead to true
                //note that ctx.getVariables().get(variable) might even by null (implicit this)
                return true;
            }

            TreePath boundTo = ctx.getVariables().get(variable);
            TypeMirror realType = ctx.getInfo().getTrees().getTypeMirror(boundTo);
            TypeElement jlObject = ctx.getInfo().getElements().getTypeElement("java.lang.Object");
            TypeMirror designedType = ctx.getInfo().getTreeUtilities().parseType(constraint, jlObject);

            return not ^ ctx.getInfo().getTypes().isSubtype(realType, designedType);
        }

        @Override
        public String toString() {
            return "(INSTANCEOF " + (not ? "!" : "") + variable + "/" + constraint + ")";
        }

    }

    public static final class MethodInvocation extends Condition {

        private final String methodName;
        private final Map<? extends String, ? extends ParameterKind> params;
        private final MethodInvocationContext mic;

        public MethodInvocation(boolean not, String methodName, Map<? extends String, ? extends ParameterKind> params, MethodInvocationContext mic) {
            super(not);
            this.methodName = methodName;
            this.params = params;
            this.mic = mic;
        }

        @Override
        public boolean holds(HintContext ctx, boolean global) {
            return mic.invokeMethod(ctx, methodName, params) ^ not;
        }

        @Override
        public String toString() {
            return "(METHOD_INVOCATION " + (not ? "!" : "") + ":" + methodName + "(" + params.toString() + "))";
        }

        public enum ParameterKind {
            VARIABLE,
            STRING_LITERAL,
            ENUM_CONSTANT;
        }
    }

}
