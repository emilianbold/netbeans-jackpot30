package org.netbeans.modules.jackpot30.file;

import com.sun.source.util.TreePath;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
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

    public abstract boolean holds(HintContext ctx);

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
        public boolean holds(HintContext ctx) {
            return true; //XXX: '!' not supported!
//            TreePath boundTo = ctx.getVariables().get(variable);
//            TypeMirror realType = ctx.getInfo().getTrees().getTypeMirror(boundTo);
//            TypeMirror designedType = ctx.getInfo().getTreeUtilities().parseType(variable, null)
        }

        @Override
        public String toString() {
            return "(INSTANCEOF " + variable + "/" + constraint + ")";
        }

    }

    public static final class MethodInvocation extends Condition {

        private final String methodName;
        private final Map<? extends String, ? extends ParameterKind> params;

        public MethodInvocation(boolean not, String methodName, Map<? extends String, ? extends ParameterKind> params) {
            super(not);
            this.methodName = methodName;
            this.params = params;
        }

        @Override
        public boolean holds(HintContext ctx) {
            Collection<Class<?>> paramTypes = new LinkedList<Class<?>>();
            Collection<Object> paramValues = new LinkedList<Object>();

            paramTypes.add(HintContext.class);
            paramValues.add(ctx);

            for (Entry<? extends String, ? extends ParameterKind> e : params.entrySet()) {
                switch ((ParameterKind) e.getValue()) {
                    case VARIABLE:
                        if (isMultistatementWildcard(e.getKey())) {
                            paramTypes.add(Iterable.class);
                            paramValues.add(ctx.getMultiVariables().get(e.getKey()));
                        } else {
                            paramTypes.add(TreePath.class);
                            paramValues.add(ctx.getVariables().get(e.getKey()));
                        }
                        break;
                    case STRING_LITERAL:
                        paramTypes.add(String.class);
                        paramValues.add(e.getKey());
                        break;
                    case ENUM_CONSTANT:
                        Enum<?> constant = loadEnumConstant(e.getKey());
                        
                        paramTypes.add(constant.getDeclaringClass());
                        paramValues.add(constant);
                        break;
                }
            }
            
            try {
                //resolve hardcoded rules:
                Method m = RuleUtilities.class.getDeclaredMethod(methodName, paramTypes.toArray(new Class<?>[0]));

                return (Boolean) m.invoke(null, paramValues.toArray(new Object[0])) ^ not;
            } catch (IllegalAccessException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            } catch (NoSuchMethodException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            } catch (SecurityException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            }

            throw new IllegalStateException(methodName + " not found. Parameter types: " + paramTypes.toString());
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

    private static Enum<?> loadEnumConstant(String fqn) {
        int lastDot = fqn.lastIndexOf('.');

        assert lastDot != (-1);

        String className = fqn.substring(0, lastDot);
        String constantName = fqn.substring(lastDot + 1);
        
        try {
            Class c = (Class) Class.forName(className);

            return Enum.valueOf(c, constantName);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    //XXX: copied from jackpot30.impl.Utilities:
    public static boolean isMultistatementWildcard(/*@NonNull */CharSequence name) {
        return name.charAt(name.length() - 1) == '$';
    }
}
