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
package org.netbeans.modules.jackpot30.cmdline.lib;

import com.sun.source.tree.Tree;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.classfile.ClassFile;
import org.netbeans.modules.classfile.ClassName;
import org.netbeans.modules.jackpot30.cmdline.lib.StandaloneTools.RepositoryImpl;
import org.netbeans.modules.jackpot30.impl.Utilities.SPI;
import org.netbeans.modules.jackpot30.java.hints.JavaHintsHintProvider;
import org.netbeans.modules.java.hints.jackpot.code.CodeHintProviderImpl;
import org.netbeans.modules.java.hints.jackpot.code.FSWrapper;
import org.netbeans.spi.editor.mimelookup.MimeDataProvider;
import org.openide.util.NbPreferences.Provider;
import org.openide.xml.EntityCatalog;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author lahvac
 */
public abstract class CreateStandaloneJar extends NbTestCase {
    private final String toolName;

    public CreateStandaloneJar(String name, String toolName) {
        super(name);
        this.toolName = toolName;
    }

    public void testDumpImportantHack() throws Exception {
        String targetDir = System.getProperty("outputDir", System.getProperty("java.io.tmpdir"));
        String targetName = System.getProperty("targetName", toolName + ".jar");

        createCompiler(new File(targetDir, targetName), new File(targetDir, "hints"));
    }

    protected abstract Info computeInfo();

    public void createCompiler(File targetCompilerFile, File targetHintsFile) throws Exception {
        JarOutputStream out = new JarOutputStream(new FileOutputStream(targetCompilerFile));
        List<String> toProcess = new LinkedList<String>(INCLUDE);

        for (FSWrapper.ClassWrapper cw : FSWrapper.listClasses()) {
            toProcess.add(cw.getName());
        }

        Info info = computeInfo();

        toProcess.addAll(info.additionalRoots);

        Set<String> done = new HashSet<String>();
        Set<String> bundlesToCopy = new HashSet<String>();

        while (!toProcess.isEmpty()) {
            String fqn = toProcess.remove(0);

            if (!done.add(fqn)) {
                continue;
            }

//            System.err.println("processing: " + fqn);

            String fileName = fqn.replace('.', '/') + ".class";
            URL url = this.getClass().getClassLoader().getResource(fileName);

            if (url == null) {
                //probably array:
                continue;
            }

            Class<?> clazz = Class.forName(fqn, false, this.getClass().getClassLoader());

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
                toProcess.add(classFromCP.getInternalName().replace('/', '.'));
            }

            out.putNextEntry(new ZipEntry(escapeJavaxLang(fileName)));
            out.write(escapeJavaxLang(bytes));

            if (COPY_REGISTRATION.contains(fqn) || info.copyMetaInfRegistration.contains(fqn)) {
                String serviceName = "META-INF/services/" + fqn;
                Enumeration<URL> resources = this.getClass().getClassLoader().getResources(serviceName);

                if (resources.hasMoreElements()) {
                    out.putNextEntry(new ZipEntry(escapeJavaxLang(serviceName)));

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
        Enumeration<URL> resources = this.getClass().getClassLoader().getResources("META-INF/generated-layer.xml");
        Document main = null;

        while (resources.hasMoreElements()) {
            URL res = resources.nextElement();
            Document current = XMLUtil.parse(new InputSource(res.openStream()), false, false, null, new EntityCatalog() {
                @Override
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return new InputSource(CreateStandaloneJar.class.getResourceAsStream("/org/openide/filesystems/filesystem1_2.dtd"));
                }
            });

            if (main == null) {
                main = current;
            } else {
                NodeList children = current.getDocumentElement().getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    main.getDocumentElement().appendChild(main.importNode(children.item(i), true));
                }
            }
        }

        out.putNextEntry(new ZipEntry(escapeJavaxLang("META-INF/generated-layer.xml")));
        XMLUtil.write(main, out, "UTF-8");

        addMETA_INFRegistration(out, "java.lang.SecurityManager", "org.netbeans.modules.masterfs.filebasedfs.utils.FileChangedManager");
        addMETA_INFRegistration(out, "org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation", StandaloneTools.EmptySourceForBinaryQueryImpl.class.getName(), 0);
        addMETA_INFRegistration(out, Provider.class.getName(), StandaloneTools.PreferencesProvider.class.getName());
        addMETA_INFRegistration(out, MimeDataProvider.class.getName(), StandaloneTools.StandaloneMimeDataProviderImpl.class.getName());
        addMETA_INFRegistration(out, SPI.class.getName(), StandaloneTools.UtilitiesSPIImpl.class.getName());

        for (MetaInfRegistration r : info.metaInf) {
            addMETA_INFRegistration(out, r.apiClassName, r.implClassName, r.pos);
        }

        out.close();

        Writer hints = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetHintsFile), "UTF-8"));

//        hints.write(DumpHints.dumpHints());

        hints.close();
    }

    public static final class Info {
        private final Set<String> additionalRoots = new HashSet<String>();
        private final List<MetaInfRegistration> metaInf = new LinkedList<MetaInfRegistration>();
        private final Set<String> copyMetaInfRegistration = new HashSet<String>();
        public Info() {}
        public Info addAdditionalRoots(String... fqns) {
            additionalRoots.addAll(Arrays.asList(fqns));
            return this;
        }
        public Info addMetaInfRegistrations(MetaInfRegistration... registrations) {
            metaInf.addAll(Arrays.asList(registrations));
            return this;
        }
        public Info addMetaInfRegistrationToCopy(String... registrationsToCopy) {
            copyMetaInfRegistration.addAll(Arrays.asList(registrationsToCopy));
            return this;
        }
    }

    public static final class MetaInfRegistration {
        private final String apiClassName;
        private final String implClassName;
        private final Integer pos;

        public MetaInfRegistration(String apiClassName, String implClassName) {
            this(apiClassName, implClassName, null);
        }
        
        public MetaInfRegistration(Class<?> apiClass, Class<?> implClass) {
            this(apiClass.getName(), implClass.getName(), null);
        }

        public MetaInfRegistration(String apiClassName, String implClassName, Integer pos) {
            this.apiClassName = apiClassName;
            this.implClassName = implClassName;
            this.pos = pos;
        }

    }

    private void copyResources(JarOutputStream out, Set<String> res) throws IOException {
        for (String resource : res) {
            URL url = this.getClass().getClassLoader().getResource(resource);

            if (url == null) {
                continue;
            }
            
            out.putNextEntry(new ZipEntry(escapeJavaxLang(resource)));
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
        out.putNextEntry(new ZipEntry(escapeJavaxLang("META-INF/services/" + apiClassName)));
        out.write(implClassName.getBytes("UTF-8"));
        if (pos != null) {
            out.write(("\n#position=" + pos.toString() + "\n").getBytes("UTF-8"));
        }
    }

    private static final Map<String, String> replaceWhat2With = new LinkedHashMap<String, String>();

    static {
        replaceWhat2With.put("javax/lang/", "jpt30/lang/");
        replaceWhat2With.put("javax/tools/", "jpt30/tools/");
    }
            

   private static byte[] escapeJavaxLang(byte[] source) throws UnsupportedEncodingException {
       for (Entry<String, String> e  : replaceWhat2With.entrySet()) {
           byte[] replaceSource = e.getKey().getBytes("UTF-8");
           byte[] replaceTarget = e.getValue().getBytes("UTF-8");

           OUTER:
           for (int i = 0; i < source.length - replaceSource.length; i++) {
               for (int j = 0; j < replaceSource.length; j++) {
                   if (source[i + j] != replaceSource[j]) {
                       continue OUTER;
                   }
               }

               for (int j = 0; j < replaceTarget.length; j++) {
                   source[i + j] = replaceTarget[j];
               }

               i += replaceTarget.length - 1;
           }
       }

       return source;
    }

    private static String escapeJavaxLang(String fileName) throws UnsupportedEncodingException {
        for (Entry<String, String> e : replaceWhat2With.entrySet()) {
            fileName = fileName.replace(e.getKey(), e.getValue());
        }

        return fileName;
    }



    private static final Set<String> INCLUDE = new HashSet<String>(Arrays.asList(
            StandaloneTools.class.getName(),
            StandaloneTools.EmptySourceForBinaryQueryImpl.class.getName(),
            StandaloneTools.PreferencesProvider.class.getName(),
            StandaloneTools.StandaloneMimeDataProviderImpl.class.getName(),
            CodeHintProviderImpl.class.getName(),
            JavaHintsHintProvider.class.getName(),
            JavaSource.class.getName(),
            DumpHints.class.getName(),
            RepositoryImpl.class.getName(),
            "org.netbeans.core.startup.layers.ArchiveURLMapper",
            "org.netbeans.modules.jackpot30.file.DeclarativeHintRegistry",
            "org.netbeans.core.startup.layers.NbinstURLMapper",
            "org.netbeans.modules.masterfs.MasterURLMapper",
            "org.netbeans.core.NbLoaderPool",
            "org.netbeans.core.startup.preferences.PreferencesProviderImpl",
            "org.netbeans.modules.java.platform.DefaultJavaPlatformProvider",
            
            "com.sun.tools.javac.resources.compiler",
            "com.sun.tools.javac.resources.javac"


            , "org.netbeans.modules.java.hints.infrastructure.RulesManager$HintProviderImpl"
            , Tree.class.getName()
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
        "com/sun/tools/javadoc/resources/javadoc.properties",
        "com/sun/tools/javadoc/resources/javadoc_ja.properties",
        "com/sun/tools/javadoc/resources/javadoc_zh_CN.properties",
        "org/netbeans/modules/java/source/resources/icons/error-badge.gif",
        "org/netbeans/modules/java/source/resources/layer.xml",
        "org/netbeans/modules/java/j2seproject/ui/resources/brokenProjectBadge.gif",
        "org/netbeans/modules/java/j2seproject/ui/resources/compileOnSaveDisabledBadge.gif",
        "org/netbeans/modules/parsing/impl/resources/error-badge.gif"
    ));

}