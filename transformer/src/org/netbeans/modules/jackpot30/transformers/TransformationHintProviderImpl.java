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
 * License Header, with the fields enclosed by brackets [] replaced bysanno
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

package org.netbeans.modules.jackpot30.transformers;

import com.sun.source.tree.Tree;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ClasspathInfo.PathKind;
import org.netbeans.api.java.source.CompilationController;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.Task;
import org.netbeans.api.java.source.TypeMirrorHandle;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.modules.jackpot30.spi.ElementBasedHintProvider;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.PatternDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;
import org.netbeans.modules.jackpot30.spi.HintDescriptionFactory;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.jackpot30.spi.support.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;

import org.openide.util.lookup.ServiceProvider;
import static org.netbeans.modules.jackpot30.transformers.Annotations.*;

/**
 *
 * @author Jan Lahoda
 */
@ServiceProvider(service=ElementBasedHintProvider.class)
public class TransformationHintProviderImpl implements ElementBasedHintProvider {

    private JavaSource prepareJavaSource(CompilationInfo info) {
        ClasspathInfo currentCP = info.getClasspathInfo();
        ClassPath overlayCompileCP = prepareOverlayCompileCP();
        ClassPath extendedCompileCP = ClassPathSupport.createProxyClassPath(overlayCompileCP, currentCP.getClassPath(PathKind.COMPILE));
        ClassPath overlayBootCP = prepareOverlayBootCP();
        ClassPath extendedBootCP = ClassPathSupport.createProxyClassPath(overlayBootCP, currentCP.getClassPath(PathKind.BOOT));
        ClasspathInfo extendedCPInfo = ClasspathInfo.create(extendedBootCP, extendedCompileCP, currentCP.getClassPath(PathKind.SOURCE));

        return JavaSource.create(extendedCPInfo);
    }
    
    public Collection<? extends HintDescription> computeHints(final CompilationInfo info) {
        final List<HintDescription> hints = new LinkedList<HintDescription>();

        if (ANNOTATIONS_JAR != null && (JDK_OVERLAY_JAR != null || NB_OVERLAY_JAR != null)) {
            try {
                prepareJavaSource(info).runUserActionTask(new Task<CompilationController>() {
                    public void run(final CompilationController overlayInfo) throws Exception {
                        List<HintDescription> w = doComputeHints(info, overlayInfo);

                        if (w != null) {
                            hints.addAll(w);
                        }
                    }
                }, true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            List<HintDescription> w = doComputeHints(info, null);

            if (w != null) {
                hints.addAll(w);
            }
        }

        return hints;
    }

    private static final Set<ElementKind> KINDS_FOR_ELEMENT_HANDLE = EnumSet.of(
            ElementKind.PACKAGE, ElementKind.CLASS, ElementKind.INTERFACE, ElementKind.ENUM,
            ElementKind.ANNOTATION_TYPE, ElementKind.METHOD, ElementKind.CONSTRUCTOR,
            ElementKind.INSTANCE_INIT, ElementKind.STATIC_INIT, ElementKind.FIELD,
            ElementKind.ENUM_CONSTANT);
    
    private  List<HintDescription> doComputeHints(CompilationInfo info,
                                                  CompilationInfo overlayInfo) {

        FindAnnotation ann = new FindAnnotation(info, overlayInfo, /*XXX*/new AtomicBoolean());

        ann.scan(info.getCompilationUnit(), null);

        return ann.hints;
    }

    private final class FindAnnotation extends CancellableTreePathScanner<Void, Void> {

        private final CompilationInfo info;
        private final CompilationInfo overlayInfo;
        private final List<HintDescription> hints = new LinkedList<HintDescription>();

        public FindAnnotation(CompilationInfo info, CompilationInfo overlayInfo, AtomicBoolean cancel) {
            super(cancel);
            this.info = info;
            this.overlayInfo = overlayInfo;
        }

        private final Set<Element> handledElements = new HashSet<Element>();
        
        private void handleElementImpl(Element el) {
            if (!handledElements.add(el)) return ;

            List<HintDescription> currentHints = processAnnotations(el, el.getEnclosingElement(), info, info);

            if (overlayInfo != null && currentHints.isEmpty() && KINDS_FOR_ELEMENT_HANDLE.contains(el.getKind())) {
                Element overlay = ElementHandle.create(el).resolve(overlayInfo);

                if (overlay != null) {
                    currentHints = processAnnotations(overlay, overlay.getEnclosingElement(), info, overlayInfo);
                }
            }

            this.hints.addAll(currentHints);
        }

        private void handleElement(Element el) {
            PackageElement p = info.getElements().getPackageOf(el);

            while (p != el) {
                handleElementImpl(el);
                el = el.getEnclosingElement();
            }

            handleElementImpl(p);
        }

        @Override
        public Void scan(Tree tree, Void p) {
            if (tree == null) return null;

            TreePath tp = new TreePath(getCurrentPath(), tree);
            Element el = info.getTrees().getElement(tp);

            if (el != null) {
                handleElement(el);
            }

            return super.scan(tree, p);
        }
    }

    private static final class WorkerImpl implements Worker {
        
        private final String displayName;
        private final List<FixDescription> fixDescriptions;

        public WorkerImpl(String displayName, List<FixDescription> fixDescriptions) {
            this.displayName = displayName;
            this.fixDescriptions = fixDescriptions;
        }

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
            //XXX: antipatterns not supported by the current infrastructure
//            TreePath tp = treePath;
//
//            while (tp != null) {
//                if (!pd.getPattern().checkAntipatterns(tp)) {
//                    return null;
//                }
//
//                tp = tp.getParentPath();
//            }

            List<Fix> fixes = new LinkedList<Fix>();

            for (FixDescription d : fixDescriptions) {
                String dn = d.getDisplayName();

                if (dn.length() == 0) {
                    dn = defaultFixDisplayName(ctx.getInfo(), ctx.getVariables(), d);
                }

                fixes.add(JavaFix.rewriteFix(ctx.getInfo(), dn, ctx.getPath(), d.getPattern(), ctx.getVariables(), ctx.getMultiVariables(), ctx.getVariableNames(), Collections.<String, TypeMirror>emptyMap()/*XXX: pd.pattern.getConstraints()*/));
            }

            return Collections.singletonList(ErrorDescriptionFactory.forName(ctx, ctx.getPath(), displayName, fixes.toArray(new Fix[0])));
        }
    }

    private static <T> T findValue(AnnotationMirror m, String name, Class<T> clazz) {
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : m.getElementValues().entrySet()) {
            if (name.equals(e.getKey().getSimpleName().toString())) {
                return clazz.cast(e.getValue().getValue());
            }
        }

        TypeElement te = (TypeElement) m.getAnnotationType().asElement();

        for (ExecutableElement ee : ElementFilter.methodsIn(te.getEnclosedElements())) {
            if (name.equals(ee.getSimpleName().toString())) {
                if (ee.getDefaultValue() == null) {
                    return null;
                }
                return clazz.cast(ee.getDefaultValue().getValue());
            }
        }

        return null;
    }

    private static HintDescription create(CompilationInfo info, Element el, AnnotationMirror transformation, CompilationInfo fake) {
        Collection rawPatterns = findValue(transformation, "pattern", Collection.class);

        if (rawPatterns == null || rawPatterns.isEmpty()) {
            return null;
        }

        Iterable<AnnotationMirror> patterns = Utilities.checkedIterableByFilter(rawPatterns, AnnotationMirror.class, true);

        String patternString = findValue(patterns.iterator().next(), "value", String.class);

        if (patternString == null) {
            return null;
        }

        Collection rawConstraints = findValue(transformation, "constraint", Collection.class);

        if (rawConstraints == null) {
            return null;
        }

        Iterable<AnnotationMirror> constraints = Utilities.checkedIterableByFilter(rawConstraints, AnnotationMirror.class, true);
        Map<String, String> variableTypes = new HashMap<String, String>();

        for (AnnotationMirror m : constraints) {
            String var = findValue(m, "variable", String.class);
            TypeMirror tm = findValue(m, "type", TypeMirror.class);

            if (var == null || tm == null) {
                return null;
            }

            variableTypes.put(var, tm.toString()); //XXX: toString()
        }

        if (!el.getModifiers().contains(Modifier.STATIC) && !variableTypes.containsKey("$this")) {
            TypeMirror parent = el.getEnclosingElement().asType();

            variableTypes.put("$this", TypeMirrorHandle.create(fake.getTypes().erasure(parent)).resolve(info).toString()); //XXX: toString()
        }

        List<String> aps = new LinkedList<String>();

        for (AnnotationMirror ap : Utilities.checkedIterableByFilter(findValue(transformation, "antipattern", Collection.class), AnnotationMirror.class, true)) {
            aps.add(findValue(ap, "value", String.class));
        }

        List<FixDescription> fixes = new LinkedList<FixDescription>();
        Collection rawFixes = findValue(transformation, "fix", Collection.class);

        if (rawFixes == null) {
            return null;
        }

        for (AnnotationMirror f : Utilities.checkedIterableByFilter(rawFixes, AnnotationMirror.class, true)) {
            String dn = findValue(f, "displayName", String.class);
            String to = findValue(f, "value", String.class);

            if (dn == null || to == null) {
                return null;
            }

            fixes.add(new FixDescription(dn, to));
        }

        String displayName = findValue(transformation, "displayName", String.class);

        if (displayName == null) {
            return null;//XXX
        }

        return HintDescriptionFactory.create()
                                     .setDisplayName(displayName)
                                     .setTriggerPattern(PatternDescription.create(patternString, variableTypes))
                                     .setWorker(new WorkerImpl(displayName, fixes))
                                     .produce();
    }


    private static final class FixDescription {
        private final String displayName;
        private final String pattern;

        public FixDescription(String displayName, String pattern) {
            this.displayName = displayName;
            this.pattern = pattern;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPattern() {
            return pattern;
        }

    }

    private static final File ANNOTATIONS_JAR;
    private static final File NB_OVERLAY_JAR;
    private static final File JDK_OVERLAY_JAR;

    static {
        ANNOTATIONS_JAR = InstalledFileLocator.getDefault().locate("libs/annotations.jar", null, false);
        NB_OVERLAY_JAR = InstalledFileLocator.getDefault().locate("overlay/org-netbeans-nboverlay.jar", null, false);
        JDK_OVERLAY_JAR = InstalledFileLocator.getDefault().locate("overlay/jdk.jar", null, false);
    }
    
    private ClassPath prepareOverlayCompileCP() throws IllegalArgumentException {
        if (NB_OVERLAY_JAR ==null) {
            return ClassPathSupport.createClassPath(new URL[0]);
        }
        
        return ClassPathSupport.createClassPath(FileUtil.urlForArchiveOrDir(NB_OVERLAY_JAR));
    }

    private ClassPath prepareOverlayBootCP() throws IllegalArgumentException {
        if (ANNOTATIONS_JAR == null) {
            return ClassPathSupport.createClassPath(new URL[0]);
        }

        if (JDK_OVERLAY_JAR == null) {
            return ClassPathSupport.createClassPath(FileUtil.urlForArchiveOrDir(ANNOTATIONS_JAR));
        }

        return ClassPathSupport.createClassPath(FileUtil.urlForArchiveOrDir(ANNOTATIONS_JAR), FileUtil.urlForArchiveOrDir(JDK_OVERLAY_JAR));
    }

    private List<HintDescription> processAnnotations(Element e, Element enclosing, CompilationInfo info, CompilationInfo fake) {
        boolean foundTransformations = false;
        List<HintDescription> result = new LinkedList<HintDescription>();
        
        for (AnnotationMirror am : e.getAnnotationMirrors()) {
            String fqn = ((TypeElement) am.getAnnotationType().asElement()).getQualifiedName().toString();
            List<AnnotationMirror> transformations = new LinkedList<AnnotationMirror>();
            if (TRANSFORMATION.toFQN().equals(fqn)) {
                transformations.add(am);
                foundTransformations = true;
            }
            if (TRANSFORMATION_SET.toFQN().equals(fqn)) {
                for (AnnotationMirror m : Utilities.checkedIterableByFilter(findValue(am, "value", Collection.class), AnnotationMirror.class, true)) {
                    transformations.add(m);
                }
                foundTransformations = true;
            }
            
            for (AnnotationMirror t : transformations) {
                HintDescription pd = create(info, e, t, fake);

                if (pd != null) {
                    result.add(pd);
                }
            }
        }

        if (!foundTransformations && e.getKind() == ElementKind.METHOD) {
            TypeElement owner = (TypeElement) e.getEnclosingElement();

            for (TypeMirror tm : fake.getTypes().directSupertypes(owner.asType())) {
                DeclaredType dt     = (DeclaredType) tm;
                TypeElement  parent = (TypeElement)  dt.asElement();

                for (ExecutableElement ee : ElementFilter.methodsIn(parent.getEnclosedElements())) {
                    if (!fake.getElements().overrides((ExecutableElement) e, ee, (TypeElement) enclosing)) {
                        continue;
                    }
                    List<HintDescription> r = processAnnotations(ee, enclosing, info, fake);

                    result.addAll(r);
                }

            }
        }

        return result;
    }

    private static String defaultFixDisplayName(CompilationInfo info, Map<String, TreePath> variables, FixDescription d) {
        Map<String, String> stringsForVariables = new HashMap<String, String>();

        for (Entry<String, TreePath> e : variables.entrySet()) {
            Tree t = e.getValue().getLeaf();
            SourcePositions sp = info.getTrees().getSourcePositions();
            int startPos = (int) sp.getStartPosition(info.getCompilationUnit(), t);
            int endPos = (int) sp.getEndPosition(info.getCompilationUnit(), t);
            
            stringsForVariables.put(e.getKey(), info.getText().substring(startPos, endPos));
        }

        if (!stringsForVariables.containsKey("$this")) {
            stringsForVariables.put("$this", "this");
        }

        String replaceTarget = d.getPattern();

        for (Entry<String, String> e : stringsForVariables.entrySet()) {
            String quotedVariable = java.util.regex.Pattern.quote(e.getKey());
            String quotedTarget = Matcher.quoteReplacement(e.getValue());
            replaceTarget = replaceTarget.replaceAll(quotedVariable, quotedTarget);
        }

        return "Rewrite to " + replaceTarget;
    }

}
