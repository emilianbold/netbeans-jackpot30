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

package org.netbeans.modules.javahints.support;

import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.swing.text.Document;
import org.netbeans.api.java.lexer.JavaTokenId;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.api.java.source.JavaSource.Phase;
import org.netbeans.api.java.source.JavaSource.Priority;
import org.netbeans.api.java.source.support.CancellableTreePathScanner;
import org.netbeans.api.java.source.support.EditorAwareJavaSourceTaskFactory;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public final class SetLanguage implements CancellableTask<CompilationInfo> {

    private final AtomicBoolean cancel = new AtomicBoolean();

    public void cancel() {
        cancel.set(true);
    }

    public void run(final CompilationInfo info) throws Exception {
        cancel.set(false);
        
        new CancellableTreePathScanner<Void, Void>(cancel) {
            @Override
            public Void visitLiteral(LiteralTree node, Void p) {
                if (node.getValue() instanceof String) {
                    maybeSetMimeType(info, getCurrentPath());
                }
                return super.visitLiteral(node, p);
            }
        }.scan(info.getCompilationUnit(), null);
    }


    private void maybeSetMimeType(CompilationInfo info, TreePath tp) {
        TreePath parent = tp.getParentPath();

        if (parent.getLeaf().getKind() != Kind.METHOD_INVOCATION) {
            return ;
        }

        MethodInvocationTree mit = (MethodInvocationTree) tp.getParentPath().getLeaf();
        Element el = info.getTrees().getElement(tp.getParentPath());
        
        if (el == null || el.getKind() != ElementKind.METHOD) {
            return ;
        }

        for (int index = 0; index < mit.getArguments().size(); index++) {
            if (!tp.getLeaf().equals(mit.getArguments().get(index))) {
                continue;
            }
            
            VariableElement ve = ((ExecutableElement) el).getParameters().get(index);
            
            for (AnnotationMirror am : ve.getAnnotationMirrors()) {
                Element annotationElement = am.getAnnotationType().asElement();
                
                if (!annotationElement.getSimpleName().contentEquals("Language")) {
                    continue;
                }

                for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : am.getElementValues().entrySet()) {
                    if (!e.getKey().getSimpleName().contentEquals("mimeType"))
                        continue;

                    String mimeType = e.getValue().getValue().toString();
                    int offset = (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), tp.getLeaf());
                    
                    ensureMimeTypeSet(info, offset, mimeType);
                }
            }

            return ;
        }
    }

    private void ensureMimeTypeSet(CompilationInfo info, int offset, String mimeType) {
        Language<?> l = Language.find(mimeType);

        if (l == null) {
            return ;
        }
        
        try {
            Document doc = info.getDocument();
            TokenHierarchy<Document> h = TokenHierarchy.get(doc);
            TokenSequence<JavaTokenId> ts = h.tokenSequence(JavaTokenId.language());

            if (ts == null) {
                return ;
            }

            ts.move(offset);
            
            if (ts.moveNext()) {
                if (ts.embedded(l) == null) {
                    ts.createEmbedding(l, 1, 1);
                }
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static final class FactoryImpl extends EditorAwareJavaSourceTaskFactory {

        public FactoryImpl() {
            super(Phase.RESOLVED, Priority.LOW);
        }

        @Override
        protected CancellableTask<CompilationInfo> createTask(FileObject file) {
            return new SetLanguage();
        }
        
    }
}
