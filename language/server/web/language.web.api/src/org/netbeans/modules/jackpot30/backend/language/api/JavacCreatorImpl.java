/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2012 Sun Microsystems, Inc.
 */
package org.netbeans.modules.jackpot30.backend.language.api;

import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javadoc.Messager;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import org.netbeans.lib.nbjavac.services.NBAttr;
import org.netbeans.lib.nbjavac.services.NBClassReader;
import org.netbeans.lib.nbjavac.services.NBClassWriter;
import org.netbeans.lib.nbjavac.services.NBJavacTrees;
import org.netbeans.lib.nbjavac.services.NBJavadocEnter;
import org.netbeans.lib.nbjavac.services.NBJavadocMemberEnter;
import org.netbeans.lib.nbjavac.services.NBParserFactory;
import org.netbeans.lib.nbjavac.services.NBTreeMaker;
import org.netbeans.modules.jackpot30.resolve.api.JavacCreator;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author lahvac
 */
@ServiceProvider(service = JavacCreator.class)
public class JavacCreatorImpl extends JavacCreator {

    @Override
    protected JavacTaskImpl doCreate(Writer out, JavaFileManager fileManager, DiagnosticListener<? super JavaFileObject> diagnosticListener, Iterable<String> options, Iterable<String> classes, Iterable<? extends JavaFileObject> compilationUnits) {
        List<String> realOptions = new ArrayList<String>();
        for (String option : options) {
            realOptions.add(option);
        }
        realOptions.add("-Xjcov"); //NOI18N, Make the compiler store end positions
        realOptions.add("-XDallowStringFolding=false"); //NOI18N
        realOptions.add("-XDshouldStopPolicy=GENERATE");   // NOI18N, parsing should not stop in phase where an error is found
        realOptions.add("-XDsuppressAbortOnBadClassFile=true");
        Context context = new Context();
        //need to preregister the Messages here, because the getTask below requires Log instance:
        Messager.preRegister(context, null, DEV_NULL, DEV_NULL, DEV_NULL);
        JavacTaskImpl task = (JavacTaskImpl) JavacTool.create().getTask(out, fileManager, diagnosticListener, realOptions, classes, compilationUnits, context);
        NBClassReader.preRegister(context, true);
        NBAttr.preRegister(context);
        NBClassWriter.preRegister(context);
        NBParserFactory.preRegister(context);
        NBTreeMaker.preRegister(context);
        NBJavacTrees.preRegister(context);
        NBJavadocEnter.preRegister(context);
        NBJavadocMemberEnter.preRegister(context);

        JavaCompiler.instance(context).keepComments = true;

        return task;
    }

    private static final PrintWriter DEV_NULL = new PrintWriter(new NullWriter(), false);

    private static class NullWriter extends Writer {

        public void write(char[] cbuf, int off, int len) throws IOException {
        }

        public void flush() throws IOException {
        }

        public void close() throws IOException {
        }
    }
}
