/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
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
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.indexer.usages;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ElementUtilities;
import org.netbeans.modules.jackpot30.backend.impl.spi.IndexAccessor;
import org.netbeans.modules.java.preprocessorbridge.spi.JavaIndexerPlugin;
import org.netbeans.modules.java.source.usages.ClassFileUtil;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author lahvac
 */
public class IndexerImpl implements JavaIndexerPlugin {

    private static final String KEY_SIGNATURES = "signatures";

    private final URL root;

    public IndexerImpl(URL root) {
        this.root = root;
    }

    @Override
    public void process(CompilationUnitTree toProcess, Indexable indexable, Lookup services) {
        try {
            final String file = IndexAccessor.getCurrent().getPath(indexable.getURL());
            final Trees trees = services.lookup(Trees.class);
            final Elements elements = services.lookup(Elements.class);
            final ElementUtilities eu = services.lookup(ElementUtilities.class);
            final Document usages = new Document();

            usages.add(new Field("file", file, Store.YES, Index.NO));

            new TreePathScanner<Void, Void>() {
                private final Set<String> SEEN_SIGNATURES = new HashSet<String>();
                @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                    handleNode();
                    return super.visitIdentifier(node, p);
                }
                @Override public Void visitMemberSelect(MemberSelectTree node, Void p) {
                    handleNode();
                    return super.visitMemberSelect(node, p);
                }
                @Override public Void visitNewClass(NewClassTree node, Void p) {
                    handleNode();
                    return super.visitNewClass(node, p);
                }
                private void handleNode() {
                    Element el = trees.getElement(getCurrentPath());

                    if (el != null && Common.SUPPORTED_KINDS.contains(el.getKind())) {
                        String serialized = Common.serialize(ElementHandle.create(el));

                        if (SEEN_SIGNATURES.add(serialized)) {
                            usages.add(new Field(KEY_SIGNATURES, serialized, Store.YES, Index.NOT_ANALYZED));
                        }

                        if (el.getKind() == ElementKind.METHOD) {
                            for (ExecutableElement e : overrides(eu, (ExecutableElement) el)) {
                                serialized = Common.serialize(ElementHandle.create(e));

                                if (SEEN_SIGNATURES.add(serialized)) {
                                    usages.add(new Field(KEY_SIGNATURES, serialized, Store.YES, Index.NOT_ANALYZED));
                                }
                            }
                        }
                    }
                }

                private String currentClassFQN;
                @Override public Void visitClass(ClassTree node, Void p) {
                    String oldClassFQN = currentClassFQN;
                    boolean oldInMethod = inMethod;

                    try {
                        Element el = trees.getElement(getCurrentPath());

                        if (el != null) {
                            try {
                                TypeElement tel = (TypeElement) el;
                                currentClassFQN = elements.getBinaryName(tel).toString();
                                Document currentClassDocument = new Document();

                                currentClassDocument.add(new Field("classFQN", currentClassFQN, Store.YES, Index.NO));
                                currentClassDocument.add(new Field("classSimpleName", node.getSimpleName().toString(), Store.YES, Index.NOT_ANALYZED));
                                currentClassDocument.add(new Field("classSimpleNameLower", node.getSimpleName().toString().toLowerCase(), Store.YES, Index.NOT_ANALYZED));

                                recordSuperTypes(currentClassDocument, tel, new HashSet<String>(Arrays.asList(tel.getQualifiedName().toString())));

                                currentClassDocument.add(new Field("file", file, Store.YES, Index.NO));

                                IndexAccessor.getCurrent().getIndexWriter().addDocument(currentClassDocument);
                            } catch (CorruptIndexException ex) {
                                Exceptions.printStackTrace(ex);
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                            }
                        }

                        inMethod = false;

                        return super.visitClass(node, p);
                    } finally {
                        currentClassFQN = oldClassFQN;
                        inMethod = oldInMethod;
                    }
                }

                private boolean inMethod;
                @Override public Void visitMethod(MethodTree node, Void p) {
                    boolean oldInMethod = inMethod;

                    try {
                        handleFeature();
                        inMethod = true;
                        return super.visitMethod(node, p);
                    } finally {
                        inMethod = oldInMethod;
                    }
                }

                @Override public Void visitVariable(VariableTree node, Void p) {
                    if (!inMethod)
                        handleFeature();
                    return super.visitVariable(node, p);
                }

                public void handleFeature() {
                    Element el = trees.getElement(getCurrentPath());

                    if (el != null) {
                        try {
                            Document currentFeatureDocument = new Document();

                            currentFeatureDocument.add(new Field("featureClassFQN", currentClassFQN, Store.YES, Index.NO));
                            currentFeatureDocument.add(new Field("featureSimpleName", el.getSimpleName().toString(), Store.YES, Index.NOT_ANALYZED));
                            currentFeatureDocument.add(new Field("featureSimpleNameLower", el.getSimpleName().toString().toLowerCase(), Store.YES, Index.NOT_ANALYZED));
                            currentFeatureDocument.add(new Field("featureKind", el.getKind().name(), Store.YES, Index.NO));
                            for (Modifier m : el.getModifiers()) {
                                currentFeatureDocument.add(new Field("featureModifiers", m.name(), Store.YES, Index.NO));
                            }
                            currentFeatureDocument.add(new Field("file", file, Store.YES, Index.NO));

                            if (el.getKind() == ElementKind.METHOD || el.getKind() == ElementKind.CONSTRUCTOR) {
                                String featureSignature = methodTypeSignature(elements, (ExecutableElement) el);

                                currentFeatureDocument.add(new Field("featureSignature", featureSignature, Store.YES, Index.NO));
                                currentFeatureDocument.add(new Field("featureVMSignature", ClassFileUtil.createExecutableDescriptor((ExecutableElement) el)[2], Store.YES, Index.NO));

                                for (ExecutableElement e : overrides(eu, (ExecutableElement) el)) {
                                    currentFeatureDocument.add(new Field("featureOverrides", Common.serialize(ElementHandle.create(e)), Store.YES, Index.NOT_ANALYZED));
                                }
                            }

                            IndexAccessor.getCurrent().getIndexWriter().addDocument(currentFeatureDocument);
                        } catch (CorruptIndexException ex) {
                            Exceptions.printStackTrace(ex);
                        } catch (IOException ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
            }.scan(toProcess, null);

            IndexAccessor.getCurrent().getIndexWriter().addDocument(usages);
        } catch (CorruptIndexException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private static Iterable<? extends ExecutableElement> overrides(ElementUtilities eu, ExecutableElement method) {
        List<ExecutableElement> result = new LinkedList<ExecutableElement>();

        //XXX: one method may override+implement more than one method
        while ((method = eu.getOverriddenMethod(method)) != null) {
            result.add(method);
        }

        return result;
    }
    
    private static void recordSuperTypes(Document target, TypeElement tel, Set<String> alreadySeen) {
        String fqn = tel.getQualifiedName().toString();

        if (alreadySeen.add(fqn)) {
            target.add(new Field("classSupertypes", fqn, Store.YES, Index.NOT_ANALYZED));
        }

        if (tel.getSuperclass().getKind() == TypeKind.DECLARED) {
            recordSuperTypes(target, (TypeElement) ((DeclaredType) tel.getSuperclass()).asElement(), alreadySeen);
        }

        for (TypeMirror i : tel.getInterfaces()) {
            if (i.getKind() == TypeKind.DECLARED) {
                recordSuperTypes(target, (TypeElement) ((DeclaredType) i).asElement(), alreadySeen);
            }
        }
    }

    private static void encodeTypeParameters(Elements elements, Collection<? extends TypeParameterElement> params, StringBuilder result) {
        if (params.isEmpty()) return;
        result.append("<");
        for (TypeParameterElement tpe : params) {
            result.append(tpe.getSimpleName());
            boolean wasClass = false;
            
            for (TypeMirror tm : tpe.getBounds()) {
                assert tm.getKind() == TypeKind.DECLARED : String.valueOf(tm);
                
                if (!((DeclaredType) tm).asElement().getKind().isClass() && !wasClass) {
                    result.append(":Ljava/lang/Object;");
                }
                
                wasClass = true;
                result.append(':');
                encodeType(elements, tm, result);
            }
        }
        result.append(">");
    }
    
    static String methodTypeSignature(Elements elements, ExecutableElement ee) {
        StringBuilder sb = new StringBuilder ();
        encodeTypeParameters(elements, ee.getTypeParameters(), sb);
        sb.append('(');             // NOI18N
        for (VariableElement pd : ee.getParameters()) {
            encodeType(elements, pd.asType(),sb);
        }
        sb.append(')');             // NOI18N
        encodeType(elements, ee.getReturnType(), sb);
        for (TypeMirror tm : ee.getThrownTypes()) {
            sb.append('^');
            encodeType(elements, tm, sb);
        }
        sb.append(';'); //TODO: unsure about this, but classfile signatures seem to have it
        return sb.toString();
    }

    private static void encodeType(Elements elements, final TypeMirror type, final StringBuilder sb) {
	switch (type.getKind()) {
	    case VOID:
		sb.append('V');	    // NOI18N
		break;
	    case BOOLEAN:
		sb.append('Z');	    // NOI18N
		break;
	    case BYTE:
		sb.append('B');	    // NOI18N
		break;
	    case SHORT:
		sb.append('S');	    // NOI18N
		break;
	    case INT:
		sb.append('I');	    // NOI18N
		break;
	    case LONG:
		sb.append('J');	    // NOI18N
		break;
	    case CHAR:
		sb.append('C');	    // NOI18N
		break;
	    case FLOAT:
		sb.append('F');	    // NOI18N
		break;
	    case DOUBLE:
		sb.append('D');	    // NOI18N
		break;
	    case ARRAY:
		sb.append('[');	    // NOI18N
		assert type instanceof ArrayType;
		encodeType(elements, ((ArrayType)type).getComponentType(),sb);
		break;
	    case DECLARED:
            {
		sb.append('L');	    // NOI18N
                DeclaredType dt = (DeclaredType) type;
		TypeElement te = (TypeElement) dt.asElement();
                sb.append(elements.getBinaryName(te).toString().replace('.', '/'));
                if (!dt.getTypeArguments().isEmpty()) {
                    sb.append('<');
                    for (TypeMirror tm : dt.getTypeArguments()) {
                        encodeType(elements, tm, sb);
                    }
                    sb.append('>');
                }
		sb.append(';');	    // NOI18N
		break;
            }
	    case TYPEVAR:
            {
		assert type instanceof TypeVariable;
		TypeVariable tr = (TypeVariable) type;
                sb.append('T');
                sb.append(tr.asElement().getSimpleName());
                sb.append(';');
		break;
            }
            case WILDCARD: {
                WildcardType wt = (WildcardType) type;

                if (wt.getExtendsBound() != null) {
                    sb.append('+');
                    encodeType(elements, wt.getExtendsBound(), sb);
                } else if (wt.getSuperBound() != null) {
                    sb.append('-');
                    encodeType(elements, wt.getSuperBound(), sb);
                } else {
                    sb.append('*');
                }
                break;
            }
            case ERROR:
            {
                TypeElement te = (TypeElement) ((ErrorType)type).asElement();
                if (te != null) {
                    sb.append('L');
                    sb.append(elements.getBinaryName(te).toString().replace('.', '/'));
                    sb.append(';');	    // NOI18N
                    break;
                }
            }
	    default:
		throw new IllegalArgumentException (type.getKind().name());
	}
    }


    @Override
    public void delete(Indexable indexable) {
//        assert false : indexable.getURL().toExternalForm() + "/" + indexable.getRelativePath();
    }

    @Override
    public void finish() {}

    @MimeRegistration(mimeType="text/x-java", service=Factory.class)
    public static final class FactoryImpl implements Factory {

        @Override
        public JavaIndexerPlugin create(URL root, FileObject cacheFolder) {
            return new IndexerImpl(root);
        }

    }

}
