package org.netbeans.modules.jackpot30.file;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.modules.jackpot30.file.Condition.MethodInvocation.ParameterKind;
import org.netbeans.modules.jackpot30.file.conditionapi.Context;
import org.netbeans.modules.jackpot30.file.conditionapi.DefaultRuleUtilities;
import org.netbeans.modules.jackpot30.file.conditionapi.Matcher;
import org.netbeans.modules.jackpot30.file.conditionapi.Variable;
import org.netbeans.modules.jackpot30.spi.Hacks;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class MethodInvocationContext {

    private final List<Class<?>> ruleUtilities;
    
    public MethodInvocationContext() {
        ruleUtilities = new LinkedList<Class<?>>();
        ruleUtilities.add(DefaultRuleUtilities.class);
    }

    public boolean invokeMethod(HintContext ctx, String methodName, Map<? extends String, ? extends ParameterKind> params) {
        Collection<Class<?>> paramTypes = new LinkedList<Class<?>>();
        Collection<Object> paramValues = new LinkedList<Object>();

        for (Entry<? extends String, ? extends ParameterKind> e : params.entrySet()) {
            switch ((ParameterKind) e.getValue()) {
                case VARIABLE:
                    paramTypes.add(Variable.class);
                    paramValues.add(new Variable(e.getKey())); //TODO: security/safety
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

        Context context = new Context(ctx);
        Matcher matcher = new Matcher(ctx);

        for (Class<?> clazz : ruleUtilities) {
            try {
                Method m = clazz.getDeclaredMethod(methodName, paramTypes.toArray(new Class<?>[0]));
                Constructor<?> c = clazz.getDeclaredConstructor(Context.class, Matcher.class);

                m.setAccessible(true);
                c.setAccessible(true);
                
                Object instance = c.newInstance(context, matcher);

                return (Boolean) m.invoke(instance, paramValues.toArray(new Object[0]));
            } catch (InstantiationException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalAccessException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            } catch (NoSuchMethodException ex) {
                //TODO: should log evenually:
            } catch (SecurityException ex) {
                //TODO: should only log evenually:
                Exceptions.printStackTrace(ex);
            }
        }


        throw new IllegalStateException(methodName + " not found. Parameter types: " + paramTypes.toString());
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

    private static final AtomicInteger c = new AtomicInteger();
    void setCode(String imports, Iterable<? extends String> blocks) {
        String className = "RuleUtilities$" + c.getAndIncrement();
        StringBuilder code = new StringBuilder();

        code.append("package $;\n");
        code.append("import org.netbeans.modules.jackpot30.file.conditionapi.Context;\n");
        code.append("import org.netbeans.modules.jackpot30.file.conditionapi.Matcher;\n");
        code.append("import org.netbeans.modules.jackpot30.file.conditionapi.Variable;\n");

        if (imports != null)
            code.append(imports);

        code.append("class " + className + "{\n");
        code.append("private final Context context;\n");
        code.append("private final Matcher matcher;\n");

        code.append(className + "(Context context, Matcher matcher) {this.context = context; this.matcher = matcher;}");

        for (String b : blocks) {
            code.append(b);
        }

        code.append("}\n");

        ClassPath boot = JavaPlatform.getDefault().getBootstrapLibraries();
        URL jarFile = MethodInvocationContext.class.getProtectionDomain().getCodeSource().getLocation();
        ClassPath compile = ClassPathSupport.createClassPath(FileUtil.urlForArchiveOrDir(FileUtil.archiveOrDirForURL(jarFile)));

        try {
            final Map<String, byte[]> classes = Hacks.compile(boot, compile, code.toString());

            ClassLoader l = new ClassLoader(MethodInvocationContext.class.getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    if (classes.containsKey(name)) {
                        byte[] bytes = classes.get(name);
                        
                        return defineClass(name, bytes, 0, bytes.length);
                    }
                    
                    return super.findClass(name);
                }
            };

            Class<?> c = l.loadClass("$." + className);

            ruleUtilities.add(c);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

}