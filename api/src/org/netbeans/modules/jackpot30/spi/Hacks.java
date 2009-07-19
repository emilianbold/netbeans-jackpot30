package org.netbeans.modules.jackpot30.spi;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Scope;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.ClassPath.Entry;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.source.JavaSourceAccessor;
import org.netbeans.modules.java.source.parsing.FileObjects;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class Hacks {

    //XXX: copied from Utilities, for declarative hints:
    private static long inc;

    public static Scope constructScope(CompilationInfo info, String... importedClasses) {
        StringBuilder clazz = new StringBuilder();

        clazz.append("package $$;\n");

        for (String i : importedClasses) {
            clazz.append("import " + i + ";\n");
        }

        clazz.append("public class $" + (inc++) + "{");

        clazz.append("private void test() {\n");
        clazz.append("}\n");
        clazz.append("}\n");

        JavacTaskImpl jti = JavaSourceAccessor.getINSTANCE().getJavacTask(info);
        Context context = jti.getContext();

        Log.instance(context).nerrors = 0;

        JavaFileObject jfo = FileObjects.memoryFileObject("$$", "$", new File("/tmp/t.java").toURI(), System.currentTimeMillis(), clazz.toString());

        try {
            Iterable<? extends CompilationUnitTree> parsed = jti.parse(jfo);
            CompilationUnitTree cut = parsed.iterator().next();

            jti.analyze(jti.enter(parsed));

            return new ScannerImpl().scan(cut, info);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    private static final class ScannerImpl extends TreePathScanner<Scope, CompilationInfo> {

        @Override
        public Scope visitBlock(BlockTree node, CompilationInfo p) {
            return p.getTrees().getScope(getCurrentPath());
        }

        @Override
        public Scope visitMethod(MethodTree node, CompilationInfo p) {
            if (node.getReturnType() == null) {
                return null;
            }
            return super.visitMethod(node, p);
        }

        @Override
        public Scope reduce(Scope r1, Scope r2) {
            return r1 != null ? r1 : r2;
        }

    }

    public static Map<String, byte[]> compile(ClassPath boot, ClassPath compile, final String code) throws IOException {
        StandardJavaFileManager sjfm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);

        sjfm.setLocation(StandardLocation.PLATFORM_CLASS_PATH, toFiles(boot));
        sjfm.setLocation(StandardLocation.CLASS_PATH, toFiles(compile));

        final Map<String, ByteArrayOutputStream> class2BAOS = new HashMap<String, ByteArrayOutputStream>();

        JavaFileManager jfm = new ForwardingJavaFileManager(sjfm) {
            @Override
            public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
                final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                
                class2BAOS.put(className, buffer);
                return new SimpleJavaFileObject(sibling.toUri(), kind) {
                    @Override
                    public OutputStream openOutputStream() throws IOException {
                        return buffer;
                    }
                };
            }
        };

        JavaFileObject file = new SimpleJavaFileObject(URI.create("mem://mem"), Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return code;
            }
        };
        ToolProvider.getSystemJavaCompiler().getTask(null, jfm, null, null, null, Arrays.asList(file)).call();

        Map<String, byte[]> result = new HashMap<String, byte[]>();

        for (Map.Entry<String, ByteArrayOutputStream> e : class2BAOS.entrySet()) {
            result.put(e.getKey(), e.getValue().toByteArray());
        }

        return result;
    }

    private static Iterable<? extends File> toFiles(ClassPath cp) {
        List<File> result = new LinkedList<File>();

        for (Entry e : cp.entries()) {
            File f = FileUtil.archiveOrDirForURL(e.getURL());

            if (f == null) {
                Logger.getLogger(Hacks.class.getName()).log(Level.INFO, "file == null, url={0}", e.getURL());
                continue;
            }

            result.add(f);
        }

        return result;
    }
}
