/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2010 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.java.hints;

import com.sun.source.util.TreePath;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.modules.jackpot30.spi.HintContext;
import org.netbeans.modules.jackpot30.spi.HintDescription;
import org.netbeans.modules.jackpot30.spi.HintDescription.Worker;
import org.netbeans.modules.jackpot30.spi.HintDescriptionFactory;
import org.netbeans.modules.jackpot30.spi.HintMetadata;
import org.netbeans.modules.jackpot30.spi.HintMetadata.HintSeverity;
import org.netbeans.modules.jackpot30.spi.HintMetadata.Kind;
import org.netbeans.modules.jackpot30.spi.HintProvider;
import org.netbeans.modules.jackpot30.spi.JavaFix;
import org.netbeans.modules.java.hints.jackpot.impl.RulesManager;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=HintProvider.class)
public class JavaHintsHintProvider implements HintProvider {

    public Map<HintMetadata, Collection<? extends HintDescription>> computeHints() {
        Map<HintMetadata, Collection<? extends HintDescription>> result = new HashMap<HintMetadata, Collection<? extends HintDescription>>();

        for (Entry<org.netbeans.modules.java.hints.jackpot.spi.HintMetadata, Collection<? extends org.netbeans.modules.java.hints.jackpot.spi.HintDescription>> e : RulesManager.getInstance().allHints.entrySet()) {
            HintMetadata hm = convert(e.getKey());
            List<HintDescription> hints = new LinkedList<HintDescription>();
            
            for (org.netbeans.modules.java.hints.jackpot.spi.HintDescription hd : e.getValue()) {
                Worker w = new WorkerImpl(hd.getWorker(), hd.getMetadata());
                HintDescriptionFactory fact = HintDescriptionFactory.create()
                                                                   .setMetadata(hm)
                                                                   .setWorker(w);
                if (hd.getTriggerPattern() != null) {
                    List<String> imports = new LinkedList<String>();
                    for (String imp : hd.getTriggerPattern().getImports()) {
                        imports.add(imp);
                    }
                    HintDescription.PatternDescription pd = HintDescription.PatternDescription.create(hd.getTriggerPattern().getPattern(), hd.getTriggerPattern().getConstraints(), imports.toArray(new String[0]));
                    fact = fact.setTriggerPattern(pd);
                } else {
                    fact = fact.setTriggerKind(hd.getTriggerKind());
                }

                hints.add(fact.produce());
            }

            result.put(hm, hints);
        }

        return result;
    }

    private static HintMetadata convert(org.netbeans.modules.java.hints.jackpot.spi.HintMetadata hm) {
        return HintMetadata.create(hm.id, hm.displayName, hm.description, hm.category, hm.enabled, convert(hm.kind), convert(hm.severity), /*XXX: customizer*/null, hm.suppressWarnings);
    }

    private static Kind convert(org.netbeans.modules.java.hints.jackpot.spi.HintMetadata.Kind kind) {
        return kind != null ? Kind.valueOf(kind.name()) : null;
    }

    private static HintSeverity convert(org.netbeans.modules.java.hints.spi.AbstractHint.HintSeverity sev) {
        return sev != null ? HintSeverity.valueOf(sev.name()) : null;
    }

    private static class WorkerImpl implements Worker {
        private final org.netbeans.modules.java.hints.jackpot.spi.HintDescription.Worker worker;
        private final org.netbeans.modules.java.hints.jackpot.spi.HintMetadata hm;

        public WorkerImpl(org.netbeans.modules.java.hints.jackpot.spi.HintDescription.Worker worker, org.netbeans.modules.java.hints.jackpot.spi.HintMetadata hm) {
            this.worker = worker;
            this.hm = hm;
        }

        public Collection<? extends ErrorDescription> createErrors(HintContext ctx) {
            org.netbeans.modules.java.hints.jackpot.spi.HintContext newCTX = new org.netbeans.modules.java.hints.jackpot.spi.HintContext(ctx.getInfo(), hm, ctx.getPath(), ctx.getVariables(), ctx.getMultiVariables(), ctx.getVariableNames());
            List<ErrorDescription> result = new LinkedList<ErrorDescription>();
            Collection<? extends ErrorDescription> origErrors = worker.createErrors(newCTX);

            if (origErrors == null) {
                return Collections.<ErrorDescription>emptyList();
            }
            
            for (ErrorDescription ed : origErrors) {
                List<Fix> fixes = new LinkedList<Fix>();

                for (Fix f : ed.getFixes().getFixes()) { //XXX: computed
                    if (f instanceof org.netbeans.modules.java.hints.jackpot.impl.JavaFixImpl) {
                        fixes.add(JavaFix.toEditorFix(new JavaFixWrapper(ctx.getInfo(), ((org.netbeans.modules.java.hints.jackpot.impl.JavaFixImpl) f).jf)));
                    } else {
                        fixes.add(f);
                    }
                }

                result.add(ErrorDescriptionFactory.createErrorDescription(ed.getSeverity(), ed.getDescription(), fixes, ed.getFile(), ed.getRange().getBegin().getOffset(), ed.getRange().getEnd().getOffset()));
            }

            return result;
        }
    }

    private static final class JavaFixWrapper extends JavaFix {
        private final org.netbeans.modules.java.hints.jackpot.spi.JavaFix orig;

        public JavaFixWrapper(CompilationInfo info, org.netbeans.modules.java.hints.jackpot.spi.JavaFix orig) {
//            super(findTPH(orig));
            super(info, findTPH(orig).resolve(info));
            this.orig = orig;
        }

        private static TreePathHandle findTPH(org.netbeans.modules.java.hints.jackpot.spi.JavaFix orig) {
            //XXX:
            try {
                Field handle = org.netbeans.modules.java.hints.jackpot.spi.JavaFix.class.getDeclaredField("handle");

                handle.setAccessible(true);
                return (TreePathHandle) handle.get(orig);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException(ex);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            } catch (NoSuchFieldException ex) {
                throw new IllegalStateException(ex);
            } catch (SecurityException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        protected String getText() {
            return org.netbeans.modules.java.hints.jackpot.impl.JavaFixImpl.Accessor.INSTANCE.getText(orig);
        }

        @Override
        protected void performRewrite(WorkingCopy wc, TreePath tp, final UpgradeUICallback callback) {
            try {
                org.netbeans.modules.java.hints.jackpot.impl.JavaFixImpl.Accessor.INSTANCE.process(orig, wc, new org.netbeans.modules.java.hints.jackpot.spi.JavaFix.UpgradeUICallback() {
                    public boolean shouldUpgrade(String comment) {
                        return callback.shouldUpgrade(comment);
                    }
                });
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

}
