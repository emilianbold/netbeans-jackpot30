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

package org.netbeans.modules.jackpot30.server.indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.netbeans.modules.jackpot30.impl.Utilities;
import org.netbeans.modules.jackpot30.impl.indexing.Cache;

/**
 *
 * @author lahvac
 */
public class Index {

    static final String PARAM_CONSTRUCT_DUPLICATES_INDEX = "-construct-duplicates-index";
    static final String PARAM_STORE_SOURCES = "-store-sources";
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        new Index().index(args);
    }

    void index(String[] args) throws IOException {
        List<String> argsList = new ArrayList<String>(Arrays.asList(args));
        boolean constructDuplicatesIndex = !argsList.isEmpty() && PARAM_CONSTRUCT_DUPLICATES_INDEX.equals(argsList.get(0));

        if (constructDuplicatesIndex) argsList.remove(0);

        boolean storeSources = !argsList.isEmpty() && PARAM_STORE_SOURCES.equals(argsList.get(0));

        if (storeSources) argsList.remove(0);
        
        String modified = null;
        String removed  = null;

        if (argsList.size() != 2 && argsList.size() != 4) {
            printHelp();
            return ;
        }

        if (argsList.size() == 4) {
            modified = argsList.get(2);
            removed = argsList.get(3);
        }

        long startTime = System.currentTimeMillis();

        Cache.setStandaloneCacheRoot(new File(argsList.get(1)));
        invokeIndexer(new File(argsList.get(0)), constructDuplicatesIndex, storeSources, modified, removed);

        long endTime = System.currentTimeMillis();

        System.out.println("indexing took: " + Utilities.toHumanReadableTime(endTime - startTime));
    }

    protected void invokeIndexer(File root, boolean duplicatesIndex, boolean storeSources, String modified, String removed) throws IOException {
        StandaloneIndexer.index(root, duplicatesIndex, storeSources, modified, removed);
    }

    protected void printHelp() {
        System.err.println("Usage: java -jar " + Index.class.getProtectionDomain().getCodeSource().getLocation().getPath() + " [" + PARAM_CONSTRUCT_DUPLICATES_INDEX + "] [" + PARAM_STORE_SOURCES + "] <source-root> <cache> [<modified-files> <removed-files>]");
    }
}
