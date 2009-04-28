/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.javahints.pm;

import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.modules.javahints.epi.Hint;
import org.netbeans.modules.javahints.epi.HintContext;
import org.netbeans.modules.javahints.epi.TriggerTreeKind;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbCollections;

/**
 *
 * @author lahvac
 */
public class RulesManager {

    private final Map<Kind, List<Method>> hints = new HashMap<Kind, List<Method>>();
    private final Map<String, List<Method>> pattern2Hint = new HashMap<String, List<Method>>();

    private static final RulesManager INSTANCE = new RulesManager();

    public static RulesManager getInstance() {
        return INSTANCE;
    }

    private RulesManager() {
        this(findLoader(), "META-INF/nb-hints/hints");
    }

    public RulesManager(ClassLoader l, String path) {
        try {
            Set<String> classes = new HashSet<String>();

            for (URL u : NbCollections.iterable(l.getResources(path))) {
                BufferedReader r = null;

                try {
                    r = new BufferedReader(new InputStreamReader(u.openStream(), "UTF-8"));
                    String line;

                    while ((line = r.readLine()) != null) {
                        classes.add(line);
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                } finally {
                    if (r != null) {
                        r.close();
                    }
                }
            }

            for (String c : classes) {
                try {
                    Class clazz = l.loadClass(c);

                    for (Method m : clazz.getDeclaredMethods()) {
                        if (m.getAnnotation(Hint.class) != null) {
                            TriggerTreeKind kindTrigger = m.getAnnotation(TriggerTreeKind.class);

                            if (kindTrigger == null) {
                                continue;
                            }

                            for (Kind k : new HashSet<Kind>(Arrays.asList(kindTrigger.value()))) {
                                List<Method> methods = this.hints.get(k);

                                if (methods == null) {
                                    this.hints.put(k, methods = new LinkedList<Method>());
                                }

                                methods.add(m);
                            }
                        }
                    }
                } catch (ClassNotFoundException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static ClassLoader findLoader() {
        ClassLoader l = Lookup.getDefault().lookup(ClassLoader.class);

        if (l == null) {
            return HintsInvoker.class.getClassLoader();
        }

        return l;
    }

    public Map<Kind, List<Method>> getKindBasedHints() {
        return hints;
    }

    public Map<String, List<Method>> getPatternBasedHints() {
        return pattern2Hint;
    }
    
}
