/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008-2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.code.processor;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("org.netbeans.modules.jackpot30.code.spi.*")
@ServiceProvider(service=Processor.class)
public class JavaHintsAnnotationProcessor extends AbstractProcessor {

    private static final Logger LOG = Logger.getLogger(JavaHintsAnnotationProcessor.class.getName());
    
    private final Set<String> hintTypes = new HashSet<String>();
    private final Set<String> compileTimeHintTypes = new HashSet<String>();
    
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            generateTypeList("org.netbeans.modules.jackpot30.code.spi.Hint", roundEnv, hintTypes);
            generateTypeList("org.netbeans.modules.jackpot30.code.spi.TriggerCompileTime", roundEnv, compileTimeHintTypes);
        } else {
            generateTypeFile(hintTypes, "hints");
            generateTypeFile(compileTimeHintTypes, "compile-time");
        }

        if (true) return false;
        
        ClassLoader l = new HintsClassLoader(JavaHintsAnnotationProcessor.class.getClassLoader(), processingEnv.getFiler(), processingEnv.getMessager());

        try {
            Class rmClass = Class.forName("org.netbeans.modules.javahints.pm.RulesManager", true, l);
            Object rm = rmClass.getConstructor(ClassLoader.class, String.class).newInstance(l, "META-INF/nb-hints/compile-time");
            Class hiClass = Class.forName("org.netbeans.modules.javahints.pm.HintsInvoker", true, l);
            Method computeHints = hiClass.getDeclaredMethod("computeHints", URI.class, ProcessingEnvironment.class, CompilationUnitTree.class, rmClass);
            Set<CompilationUnitTree> cuts = new HashSet<CompilationUnitTree>();

            processingEnv.getMessager().printMessage(Kind.NOTE, "roundEnv.getRootElements()=" + roundEnv.getRootElements());
            
            for (Element e : roundEnv.getRootElements()) {
                TreePath tp = Trees.instance(processingEnv).getPath(e);
                CompilationUnitTree cut = tp.getCompilationUnit();

                if (cuts.add(cut)) {
                    processingEnv.getMessager().printMessage(Kind.NOTE, cut.toString());
                    computeHints.invoke(null, cut.getSourceFile().toUri(), processingEnv, cut, rm);
                }
            }
        } catch (InstantiationException ex) {
            StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); processingEnv.getMessager().printMessage(Kind.ERROR, w.toString());
        } catch (IllegalAccessException ex) {
            StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); processingEnv.getMessager().printMessage(Kind.ERROR, w.toString());
        } catch (IllegalArgumentException ex) {
            StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); processingEnv.getMessager().printMessage(Kind.ERROR, w.toString());
        } catch (InvocationTargetException ex) {
            StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); processingEnv.getMessager().printMessage(Kind.ERROR, w.toString());
        } catch (NoSuchMethodException ex) {
            StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); processingEnv.getMessager().printMessage(Kind.ERROR, w.toString());
        } catch (SecurityException ex) {
            StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); processingEnv.getMessager().printMessage(Kind.ERROR, w.toString());
        } catch (ClassNotFoundException ex) {
            StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); processingEnv.getMessager().printMessage(Kind.ERROR, w.toString());
//        } catch (IOException ex) {
//            Exceptions.printStackTrace(ex);
        }
        
        return false;
    }

    private void generateTypeList(String annotationName, RoundEnvironment roundEnv, Set<String> hintTypes) {
        TypeElement hint = processingEnv.getElementUtils().getTypeElement(annotationName);
        for (Element method : roundEnv.getElementsAnnotatedWith(hint)) {
            TypeElement enclosing = (TypeElement) method.getEnclosingElement();
            hintTypes.add(processingEnv.getElementUtils().getBinaryName(enclosing).toString());
        }
    }

    private void generateTypeFile(Set<String> types, String fileName) {
        Set<String> toWrite = new HashSet<String>(types);

        BufferedReader r = null;

        try {
            FileObject source = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/nb-hints/" + fileName);
            
            r = new BufferedReader(new InputStreamReader(source.openInputStream(), "UTF-8"));

            String line;

            while ((line = r.readLine()) != null) {
                toWrite.add(line);
            }
        } catch (IOException ex) {
            //ok.
            LOG.log(Level.FINE, null, ex);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        PrintWriter w = null;
        try {

            FileObject fo = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/nb-hints/" + fileName);
            w = new PrintWriter(new OutputStreamWriter(fo.openOutputStream(), "UTF-8"));
            for (String ht : toWrite) {
                w.println(ht);
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }

    private static final class HintsClassLoader extends ClassLoader {

        private final Filer f;
        private Messager m;
        
        public HintsClassLoader(ClassLoader parent, Filer f, Messager m) {
            super(parent);
            this.f = f;
            this.m = m;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            int lastDot = name.lastIndexOf('.');

            if (lastDot == (-1)) {
                throw new ClassNotFoundException(name);
            }

            String pack = name.substring(0, lastDot);
            String shortName = name.substring(lastDot + 1) + ".class";

            InputStream in = null;

            try {
                FileObject fo = f.getResource(StandardLocation.CLASS_OUTPUT, pack, shortName);

                m.printMessage(Kind.NOTE, "fo=" + fo);
                in = fo.openInputStream();

                List<Byte> l = new ArrayList<Byte>(in.available());
                int read;

                while ((read = in.read()) != (-1)) {
                    l.add((byte) read);
                }

                byte[] arr = new byte[l.size()];
                int count = 0;
                
                for (Byte b : l) {
                    arr[count++] = b;
                }

                return super.defineClass(name, arr, 0, arr.length);
            } catch (IOException ex) {
                StringWriter w = new StringWriter(); PrintWriter p = new PrintWriter(w); ex.printStackTrace(p); p.close(); m.printMessage(Kind.ERROR, w.toString());
                throw new ClassNotFoundException(name, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }

        @Override
        protected URL findResource(String name) {
            try {
                FileObject fo = f.getResource(StandardLocation.CLASS_OUTPUT, "", name);

                return fo.toUri().toURL();
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        protected Enumeration<URL> findResources(String name) throws IOException {
            URL u = findResource(name);

            if (u == null) {
                return Collections.enumeration(Collections.<URL>emptyList());
            }

            return Collections.enumeration(Collections.singletonList(u));
        }

    }

}
