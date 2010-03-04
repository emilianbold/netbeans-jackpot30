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

package org.netbeans.modules.jackpot30.refactoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import org.netbeans.api.queries.FileEncodingQuery;
import org.netbeans.modules.refactoring.spi.BackupFacility;
import org.netbeans.modules.refactoring.spi.BackupFacility.Handle;
import org.netbeans.modules.refactoring.spi.SimpleRefactoringElementImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.text.PositionBounds;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author lahvac
 */
public class RefactoringElementImpl extends SimpleRefactoringElementImplementation {

    private final File target;
    private final String rule;

    public RefactoringElementImpl(File target, String rule) {
        this.target = target;
        this.rule = rule;
    }

    @Override
    public String getText() {
        return "Jackpot 3.0 rule";
    }

    @Override
    public String getDisplayText() {
        return "Jackpot 3.0 rule";
    }

    private Handle backup;

    @Override
    public void performChange() {
        try {
            FileObject fo = FileUtil.toFileObject(target);
            Charset encoding = FileEncodingQuery.getEncoding(getParentFile());
            String current;

            if (fo != null) {
                backup = BackupFacility.getDefault().backup(fo);
                current = read(target, encoding).toString();
            } else {
                if (!target.exists()) {
                    backup = new Handle() {
                        @Override
                        public void restore() throws IOException {
                            FileObject fo = FileUtil.toFileObject(target);

                            fo.delete();
                        }
                    };
                    current = "";
                } else {
                    //XXX: log
                    current = read(target, encoding).toString();
                }
            }

            current += rule;
            current += "\n";

            write(target, encoding, current);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public void undoChange() {
        if (backup != null) {
            try {
                backup.restore();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        backup = null;
    }

    @Override
    protected String getNewFileContent() {
        try {
            Charset encoding = FileEncodingQuery.getEncoding(getParentFile());
            String current;

            if (!target.exists()) {
                current = "";
            } else {
                //XXX: log
                current = read(target, encoding).toString();
            }

            current += rule;
            current += "\n";

            return current;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    @Override
    public Lookup getLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public FileObject getParentFile() {
        File f = target;

        while (true) {
            FileObject fo = FileUtil.toFileObject(f);

            if (fo != null) {
                return fo;
            }

            f = f.getParentFile();
        }
    }

    @Override
    public PositionBounds getPosition() {
        //XXX:
        return null;
    }

    private CharSequence read(File f, Charset encoding) throws IOException {
        Reader in = null;
        StringBuilder result = new StringBuilder();

        try {
            in = new InputStreamReader(new FileInputStream(f), encoding);

            int r;

            while ((r = in.read()) != (-1)) {
                result.append((char) r);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        return result;
    }

    private void write(File f, Charset encoding, CharSequence text) throws IOException {
        Writer out = null;

        try {
            FileObject fo = FileUtil.createData(f);
            out = new OutputStreamWriter(fo.getOutputStream(), encoding);

            out.write(text.toString());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

}
