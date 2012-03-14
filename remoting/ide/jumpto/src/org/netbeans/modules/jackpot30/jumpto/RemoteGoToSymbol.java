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
package org.netbeans.modules.jackpot30.jumpto;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.swing.Icon;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.java.source.ui.ElementIcons;
import org.netbeans.api.java.source.ui.ElementOpen;
import org.netbeans.modules.jackpot30.jumpto.RemoteGoToSymbol.RemoteSymbolDescriptor;
import org.netbeans.modules.jackpot30.jumpto.RemoteQuery.SimpleNameable;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.netbeans.spi.jumpto.symbol.SymbolDescriptor;
import org.netbeans.spi.jumpto.symbol.SymbolProvider;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service=SymbolProvider.class)
public class RemoteGoToSymbol extends RemoteQuery<RemoteSymbolDescriptor, Map<String, Object>> implements SymbolProvider {
    private static final Logger LOG = Logger.getLogger(RemoteGoToSymbol.class.getName());

    @Override
    public String name() {
        return "Jackpot 3.0 Remote Index Symbol Provider";
    }

    @Override
    public String getDisplayName() {
        return "Jackpot 3.0 Remote Index Symbol Provider";
    }

    @Override
    public void computeSymbolNames(Context context, final Result result) {
        performQuery(context.getText(), context.getSearchType(), new ResultWrapper<RemoteSymbolDescriptor>() {
            @Override public void setMessage(String message) {
                result.setMessage(message);
            }
            @Override public void addResult(RemoteSymbolDescriptor r) {
                result.addResult(r);
            }
        });
    }

    @Override
    protected URI computeURL(RemoteIndex idx, String text, SearchType searchType) {
        try {
            return new URI(idx.remote.toExternalForm() + "/symbol/search?path=" + WebUtilities.escapeForQuery(idx.remoteSegment) + "&prefix=" + WebUtilities.escapeForQuery(text) + "&querykind=" + WebUtilities.escapeForQuery(searchType.name()));
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    @Override
    protected RemoteSymbolDescriptor decode(RemoteIndex idx, String root, Map<String, Object> properties) {
        return new RemoteSymbolDescriptor(idx, properties);
    }

    static final class RemoteSymbolDescriptor extends SymbolDescriptor implements SimpleNameable {

        private final Map<String, Object> properties;
        private final AbstractDescriptor delegate;

        public RemoteSymbolDescriptor(final RemoteIndex origin, final Map<String, Object> properties) {
            this.properties = properties;
            this.delegate = new AbstractDescriptor() {
                @Override
                protected FileObject resolveFile() {
                    String relativePath = (String) properties.get("file");
                    FileObject originFolder = URLMapper.findFileObject(origin.getLocalFolder());

                    return originFolder != null ? originFolder.getFileObject(relativePath) : null;
                }
            };
        }

        @Override
        public Icon getIcon() {
            ElementKind kind = resolveKind();
            Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
            Set<String> modNames = new HashSet<String>((Collection<String>) properties.get("modifiers"));

            for (Modifier mod : Modifier.values()) {
                if (modNames.contains(mod.name())) {
                    modifiers.add(mod);
                }
            }

            return ElementIcons.getElementIcon(kind, modifiers);
        }

        private ElementKind resolveKind() {
            String kindName = (String) properties.get("kind"); //XXX: cast
            ElementKind kind = ElementKind.OTHER;

            for (ElementKind k : ElementKind.values()) {
                if (k.name().equals(kindName)) {
                    kind = k;
                    break;
                }
            }

            return kind;
        }

        @Override
        public String getProjectName() {
            return delegate.getProjectName();
        }

        @Override
        public Icon getProjectIcon() {
            return delegate.getProjectIcon();
        }

        @Override
        public FileObject getFileObject() {
            return delegate.getFileObject();
        }

        @Override
        public int getOffset() {
            return 0;
        }

        @Override
        public void open() {
            FileObject file = getFileObject();

            if (file == null) return ; //XXX tell to the user

            ClasspathInfo cpInfo = ClasspathInfo.create(file);
            ElementHandle<?> handle = createElementHandle(resolveKind(), (String) properties.get("enclosingFQN"), (String) properties.get("simpleName"), (String) properties.get("vmsignature"));

            ElementOpen.open(cpInfo, handle);
        }

        @Override
        public String getSymbolName() {
            StringBuilder name = new StringBuilder();

            name.append(properties.get("simpleName"));

            if (properties.containsKey("signature") && (resolveKind() == ElementKind.METHOD || resolveKind() == ElementKind.CONSTRUCTOR)) {
                methodParameterTypes((String) properties.get("signature"), new int[] {0}, name);
            }

            return name.toString();
        }

        @Override
        public String getOwnerName() {
            return (String) properties.get("enclosingFQN");
        }

        @Override
        public String getSimpleName() {
            return (String) properties.get("simpleName");
        }

    }

    private static ElementHandle<?> createElementHandle(ElementKind kind, String clazz, String simpleName, String signature) {
        try {
            Class<?> elementHandleAccessor = Class.forName("org.netbeans.modules.java.source.ElementHandleAccessor", false, ElementHandle.class.getClassLoader());
            Field instance = elementHandleAccessor.getDeclaredField("INSTANCE");
            Method m = elementHandleAccessor.getDeclaredMethod("create", ElementKind.class, String.class, String.class, String.class);
            return (ElementHandle<?>) m.invoke(instance.get(null), kind, clazz, simpleName, signature);
        } catch (IllegalAccessException ex) {
            LOG.log(Level.INFO, null, ex);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.INFO, null, ex);
        } catch (InvocationTargetException ex) {
            LOG.log(Level.INFO, null, ex);
        } catch (NoSuchMethodException ex) {
            LOG.log(Level.INFO, null, ex);
        } catch (NoSuchFieldException ex) {
            LOG.log(Level.INFO, null, ex);
        } catch (SecurityException ex) {
            LOG.log(Level.INFO, null, ex);
        } catch (ClassNotFoundException ex) {
            LOG.log(Level.INFO, null, ex);
        }

        return ElementHandle.createTypeElementHandle(ElementKind.CLASS, clazz);
    }
    
    private static char getChar (final String buffer, final int pos) {
        if (pos>=buffer.length()) {
            throw new IllegalStateException ();
        }
        return buffer.charAt(pos);
    }

    private static String typeArgument (final String jvmTypeId, final int[] pos) {
        char c = getChar (jvmTypeId, pos[0]);
        switch (c) {
            case '*': 
                pos[0]++;
                return ""; //XXX?
            case '+':
                pos[0]++;
                return "? extends " + typeSignatureType(jvmTypeId, pos);
            case '-':
                pos[0]++;
                return "? super " + typeSignatureType(jvmTypeId, pos);
            default:
                return typeSignatureType (jvmTypeId, pos);
        }
    }


    private static void typeArgumentsList (final String jvmTypeId, final int[] pos, StringBuilder result) {
        char c = getChar (jvmTypeId, pos[0]++);
        if (c != '<') {
            throw new IllegalStateException (jvmTypeId);
        }
        c = getChar (jvmTypeId, pos[0]);
        boolean first = true;
        while (c !='>') {
            if (!first) result.append(", ");
            first = false;
            result.append(typeArgument (jvmTypeId, pos));
            c = getChar (jvmTypeId, pos[0]);
        }
        pos[0]++;
    }

    static boolean generateSimpleNames = true;

    private static String typeSignatureType (final String jvmTypeId, final int[] pos) {
        char c = getChar(jvmTypeId, pos[0]++);
        switch (c) {
            case 'B': return "byte";
            case 'C': return "char";
            case 'D': return "double";
            case 'F': return "float";
            case 'I': return "int";
            case 'J': return "long";
            case 'S': return "short";
            case 'V': return "void";
            case 'Z': return "boolean";
            case 'L': {
                StringBuilder builder = new StringBuilder ();
                c = getChar(jvmTypeId, pos[0]++);
                while (c != ';') {
                    if (c == '/' || c == '$') {
                        if (generateSimpleNames) builder.delete(0, builder.length());
                        else builder.append('.');
                    } else {
                        builder.append(c);
                    }

                    if (c=='<') {
                        pos[0]--;
                        typeArgumentsList (jvmTypeId, pos, builder);
                        builder.append(">");
                    }
                    c = getChar(jvmTypeId, pos[0]++);
                }
                return builder.toString();
            }
            case 'T': {
                StringBuilder builder = new StringBuilder ();
                c = getChar(jvmTypeId, pos[0]++);
                while (c != ';') {
                    builder.append(c);
                    c = getChar(jvmTypeId, pos[0]++);
                }
                return builder.toString();
            }
            case '[':
                return typeSignatureType (jvmTypeId, pos) + "[]";
            default:
                return "<unknown-type>";
        }
    }

    private static void methodParameterTypes(final String jvmTypeId, final int[] pos, StringBuilder result) {
        char c = getChar (jvmTypeId, pos[0]);
        if (c == '<') {
            do {
                c = getChar (jvmTypeId, pos[0]++);
            } while (c != '>');
            c = getChar (jvmTypeId, pos[0]);
        }
        if (c!='(') {
            throw new IllegalStateException (jvmTypeId);
        }
        pos[0]++;
        c = getChar (jvmTypeId, pos[0]);
        result.append("(");
        boolean first = true;
        while (c != ')') {
            if (!first) result.append(", ");
            first = false;
            result.append(typeSignatureType (jvmTypeId, pos));
            c = getChar (jvmTypeId, pos[0]);
        }
        result.append(")");
    }
}
