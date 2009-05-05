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

package org.netbeans.modules.jackpot30.hints.pm;

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
import org.netbeans.modules.jackpot30.hints.epi.Constraint;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.modules.jackpot30.hints.epi.Hint;
import org.netbeans.modules.jackpot30.hints.epi.HintContext;
import org.netbeans.modules.jackpot30.hints.epi.Pattern;
import org.netbeans.modules.jackpot30.hints.epi.TriggerPattern;
import org.netbeans.modules.jackpot30.hints.epi.TriggerTreeKind;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbCollections;

/**
 *
 * @author lahvac
 */
public class RulesManager {

    private final Map<Kind, List<Method>> kind2Hints = new HashMap<Kind, List<Method>>();
    private final Map<PatternDescription, List<Method>> pattern2Hint = new HashMap<PatternDescription, List<Method>>();

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
                            processMethod(this.kind2Hints, this.pattern2Hint, m);
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
        return kind2Hints;
    }

    public Map<PatternDescription, List<Method>> getPatternBasedHints() {
        return pattern2Hint;
    }

    static void processMethod(Map<Kind, List<Method>> kind2Hints, Map<PatternDescription, List<Method>> pattern2Hint, Method m) {
        //XXX: combinations of TriggerTreeKind and TriggerPattern?
        processTreeKindHint(kind2Hints, m);
        processPatternHint(pattern2Hint, m);
    }
    
    private static void processTreeKindHint(Map<Kind, List<Method>> kind2Hints, Method m) {
        TriggerTreeKind kindTrigger = m.getAnnotation(TriggerTreeKind.class);

        if (kindTrigger == null) {
            return ;
        }
        
        for (Kind k : new HashSet<Kind>(Arrays.asList(kindTrigger.value()))) {
            List<Method> methods = kind2Hints.get(k);
            if (methods == null) {
                kind2Hints.put(k, methods = new LinkedList<Method>());
            }
            methods.add(m);
        }
    }
    
    private static void processPatternHint(Map<PatternDescription, List<Method>> pattern2Hint, Method m) {
        TriggerPattern patternTrigger = m.getAnnotation(TriggerPattern.class);

        if (patternTrigger == null) {
            return ;
        }

        String pattern = patternTrigger.value();
        Map<String, String> constraints = new HashMap<String, String>();

        for (Constraint c : patternTrigger.constraints()) {
            constraints.put(c.variable(), c.type());
        }

        PatternDescription pd = new PatternDescription(pattern, constraints);

        List<Method> methods = pattern2Hint.get(pd);
        
        if (methods == null) {
            pattern2Hint.put(pd, methods = new LinkedList<Method>());
        }
        
        methods.add(m);
    }

    public static final class PatternDescription {
        private final String pattern;
        private final Map<String, String> constraints;

        public PatternDescription(String pattern, Map<String, String> constraints) {
            this.pattern = pattern;
            this.constraints = constraints;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final PatternDescription other = (PatternDescription) obj;
            if ((this.pattern == null) ? (other.pattern != null) : !this.pattern.equals(other.pattern)) {
                return false;
            }
            if (this.constraints != other.constraints && (this.constraints == null || !this.constraints.equals(other.constraints))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 71 * hash + (this.pattern != null ? this.pattern.hashCode() : 0);
            hash = 71 * hash + (this.constraints != null ? this.constraints.hashCode() : 0);
            return hash;
        }

        public String getPattern() {
            return pattern;
        }

        public Map<String, String> getConstraints() {
            return constraints;
        }

    }
}
