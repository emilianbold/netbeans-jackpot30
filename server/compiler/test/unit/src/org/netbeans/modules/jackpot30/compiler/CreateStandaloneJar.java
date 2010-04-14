/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009-2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009-2010 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.compiler;

import com.sun.source.tree.Tree;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.classfile.ClassFile;
import org.netbeans.modules.classfile.ClassName;
import org.netbeans.modules.jackpot30.java.hints.JavaHintsHintProvider;
import org.netbeans.modules.java.hints.jackpot.code.CodeHintProviderImpl;
import org.netbeans.modules.java.hints.jackpot.code.FSWrapper;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.openide.util.NbPreferences.Provider;

/**
 *
 * @author lahvac
 */
public class CreateStandaloneJar extends NbTestCase {

    public CreateStandaloneJar(String name) {
        super(name);
    }

    public void testDumpImportantHack() throws Exception {
        String targetDir = System.getProperty("outputDir", System.getProperty("java.io.tmpdir"));

        createCompiler(new File(targetDir, "jackpotc.jar"), new File(targetDir, "hints"));
    }

    public static void createCompiler(File targetCompilerFile, File targetHintsFile) throws Exception {
        JarOutputStream out = new JarOutputStream(new FileOutputStream(targetCompilerFile));
        List<String> toProcess = new LinkedList<String>(INCLUDE);

        for (FSWrapper.ClassWrapper cw : FSWrapper.listClasses()) {
            toProcess.add(cw.getName().replace('.', '/'));
        }

        Set<String> done = new HashSet<String>();
        Set<String> bundlesToCopy = new HashSet<String>();

        while (!toProcess.isEmpty()) {
            String fqn = toProcess.remove(0);

            if (!done.add(fqn)) {
                continue;
            }

//            System.err.println("processing: " + fqn);

            String fileName = fqn + ".class";
            URL url = HintsAnnotationProcessingTest.class.getClassLoader().getResource(fileName);

            if (url == null) {
                //probably array:
                continue;
            }
            final String className = fqn.replace('/', '.');

            Class<?> clazz = Class.forName(className, false, HintsAnnotationProcessingTest.class.getClassLoader());

            if (    clazz.getProtectionDomain().getCodeSource() == null
                && !clazz.getName().startsWith("com.sun.source")
                && !clazz.getName().startsWith("com.sun.javadoc")
                && !clazz.getName().startsWith("javax.tools")
                && !clazz.getName().startsWith("javax.annotation.processing")
                && !clazz.getName().startsWith("javax.lang.model")) {
                //probably platform class:
                continue;
            }

            byte[] bytes = readFile(url);

            ClassFile cf = new ClassFile(new ByteArrayInputStream(bytes));

            for (ClassName classFromCP : cf.getConstantPool().getAllClassNames()) {
                toProcess.add(classFromCP.getInternalName());
            }

            out.putNextEntry(new ZipEntry(fileName));
            out.write(bytes);

            if (COPY_REGISTRATION.contains(className)) {
                String serviceName = "META-INF/services/" + className;
                Enumeration<URL> resources = HintsAnnotationProcessingTest.class.getClassLoader().getResources(serviceName);

                if (resources.hasMoreElements()) {
                    out.putNextEntry(new ZipEntry(serviceName));

                    while (resources.hasMoreElements()) {
                        URL res = resources.nextElement();

                        out.write(readFile(res));
                    }
                }
            }

            int lastSlash = fileName.lastIndexOf('/');

            if (lastSlash > 0) {
                bundlesToCopy.add(fileName.substring(0, lastSlash + 1) + "Bundle.properties");
            }
        }

        bundlesToCopy.addAll(RESOURCES);
        copyResources(out, bundlesToCopy);

        //generated-layer.xml:
        Enumeration<URL> resources = HintsAnnotationProcessingTest.class.getClassLoader().getResources("META-INF/generated-layer.xml");

        while (resources.hasMoreElements()) {
            URL res = resources.nextElement();

            if (res.toExternalForm().contains("org-netbeans-modules-java-hints.jar")) {
                byte[] bytes = readFile(res);

                out.putNextEntry(new ZipEntry("META-INF/generated-layer.xml"));
                out.write(bytes);
                break;
            }
        }

        addMETA_INFRegistration(out, "javax.annotation.processing.Processor", HintsAnnotationProcessing.class.getName());
        addMETA_INFRegistration(out, "java.lang.SecurityManager", "org.netbeans.modules.masterfs.filebasedfs.utils.FileChangedManager");
        addMETA_INFRegistration(out, "org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation", HintsAnnotationProcessing.EmptySourceForBinaryQueryImpl.class.getName(), 0);
        addMETA_INFRegistration(out, "org.netbeans.modules.java.hints.jackpot.impl.Utilities$SPI", UtilitiesSPIImpl.class.getName());
        addMETA_INFRegistration(out, Provider.class.getName(), HintsAnnotationProcessing.PreferencesProvider.class.getName());
        addMETA_INFRegistration(out, MimeDataProvider.class.getName(), HintsAnnotationProcessing.StandaloneMimeDataProviderImpl.class.getName());

        out.close();

        Writer hints = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetHintsFile), "UTF-8"));

        hints.write(DumpHints.dumpHints());

        hints.close();
    }

    private static void copyResources(JarOutputStream out, Set<String> res) throws IOException {
        for (String resource : res) {
            URL url = HintsAnnotationProcessingTest.class.getClassLoader().getResource(resource);

            if (url == null) {
                continue;
            }
            
            out.putNextEntry(new ZipEntry(resource));
            out.write(readFile(url));
        }
    }

    private static byte[] readFile(URL url) throws IOException {
        InputStream ins = url.openStream();
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        try {
            int read;

            while ((read = ins.read()) != (-1)) {
                data.write(read);
            }
        } finally {
            ins.close();
            data.close();
        }

        return data.toByteArray();
    }

    private static void addMETA_INFRegistration(JarOutputStream out, String apiClassName, String implClassName) throws IOException {
        addMETA_INFRegistration(out, apiClassName, implClassName, null);
    }

    private static void addMETA_INFRegistration(JarOutputStream out, String apiClassName, String implClassName, Integer pos) throws IOException {
        out.putNextEntry(new ZipEntry("META-INF/services/" + apiClassName));
        if (pos != null) {
            out.write(("#position=" + pos.toString() + "\n").getBytes("UTF-8"));
        }
        out.write(implClassName.getBytes("UTF-8"));
    }


    private static final Set<String> INCLUDE = new HashSet<String>(Arrays.asList(
            HintsAnnotationProcessing.class.getName().replace('.', '/'),
            HintsAnnotationProcessing.EmptySourceForBinaryQueryImpl.class.getName().replace('.', '/'),
            HintsAnnotationProcessing.PreferencesProvider.class.getName().replace('.', '/'),
            HintsAnnotationProcessing.StandaloneMimeDataProviderImpl.class.getName().replace('.', '/'),
            CodeHintProviderImpl.class.getName().replace('.', '/'),
            JavaHintsHintProvider.class.getName().replace('.', '/'),
            DumpHints.class.getName().replace('.', '/'),
            RepositoryImpl.class.getName().replace('.', '/'),
            UtilitiesSPIImpl.class.getName().replace('.', '/'),
            "org/netbeans/core/startup/layers/ArchiveURLMapper",
            "org/netbeans/modules/jackpot30/file/DeclarativeHintRegistry",
            "org/netbeans/core/startup/layers/NbinstURLMapper",
            "org/netbeans/modules/masterfs/MasterURLMapper",
            "org/netbeans/core/NbLoaderPool",
            "org/netbeans/core/startup/preferences/PreferencesProviderImpl",
            "org/netbeans/modules/java/platform/DefaultJavaPlatformProvider",
            
            "com/sun/tools/javac/resources/compiler",
            "com/sun/tools/javac/resources/javac"


            , "org.netbeans.modules.java.hints.infrastructure.RulesManager$HintProviderImpl".replace('.', '/')
            , Tree.class.getName().replace('.', '/')
        ));

    private static final Set<String> COPY_REGISTRATION = new HashSet<String>(Arrays.<String>asList(
            "org.netbeans.modules.jackpot30.spi.HintProvider",
            "org.netbeans.modules.java.hints.jackpot.spi.HintProvider",
            "org.openide.filesystems.URLMapper",
            "org.openide.util.Lookup",
            "org.netbeans.modules.openide.util.PreferencesProvider"            
            ));

    private static final Set<String> RESOURCES = new HashSet<String>(Arrays.asList(
        "com/sun/tools/javac/resources/javac_zh_CN.properties",
        "com/sun/tools/javac/resources/compiler_ja.properties",
        "com/sun/tools/javac/resources/compiler_zh_CN.properties",
        "com/sun/tools/javac/resources/legacy.properties",
        "com/sun/tools/javac/resources/compiler.properties",
        "com/sun/tools/javac/resources/javac_ja.properties",
        "com/sun/tools/javac/resources/javac.properties",
        "org/netbeans/modules/java/source/resources/icons/error-badge.gif",
        "org/netbeans/modules/java/source/resources/layer.xml"
    ));

}
