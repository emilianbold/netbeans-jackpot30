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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.jackpot30.jumpto.RemoteQuery.SimpleNameable;
import org.netbeans.modules.jackpot30.remoting.api.RemoteIndex;
import org.netbeans.modules.jackpot30.remoting.api.WebUtilities;
import org.netbeans.spi.jumpto.support.NameMatcher;
import org.netbeans.spi.jumpto.support.NameMatcherFactory;
import org.netbeans.spi.jumpto.type.SearchType;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

/**
 *
 * @author lahvac
 */
public abstract class RemoteQuery<R extends SimpleNameable> {

    private static final RequestProcessor WORKER = new RequestProcessor(RemoteGoToType.class.getName(), 1, true, false);

    private String mostGenericQueryText;
    private List<R> results;
    private AtomicBoolean cancel;
    private Task currentWorker;

    protected final void performQuery(final String text, final SearchType searchType, ResultWrapper<R> result) {
        if (!RemoteIndex.loadIndices().iterator().hasNext()) return; //TODO: optimize!

        synchronized (this) {
            if (mostGenericQueryText == null || !text.startsWith(mostGenericQueryText)) {
                if (currentWorker != null) {
                    cancel.set(true);
                    currentWorker.cancel();
                }

                mostGenericQueryText = text;

                currentWorker = WORKER.create(new ComputeResult(text, searchType, cancel = new AtomicBoolean()));

                currentWorker.schedule(0);
                results = new ArrayList<R>();
            }
        }

        try {
            currentWorker.waitFinished(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(RemoteGoToType.class.getName()).log(Level.FINE, null, ex);
        }

        boolean finished = currentWorker.isFinished();
        NameMatcher matcher = NameMatcherFactory.createNameMatcher(text, searchType);

        synchronized (this) {
            for (R td : results) {
                if (matcher.accept(td.getSimpleName()))
                    result.addResult(td);
            }
        }

        if (!finished) {
            result.setMessage("Remote query still running, some remote results may be missing");
        }
    }

    protected abstract URI computeURL(RemoteIndex idx, String text, SearchType searchType);
    protected abstract Collection<? extends R> decode(RemoteIndex idx, Reader received) throws IOException;

    private void compute(String text, SearchType searchType, AtomicBoolean cancel) {
        for (RemoteIndex ri : RemoteIndex.loadIndices()) {
            URI url = computeURL(ri, text, searchType);

            if (url == null) continue;
            
            String response = WebUtilities.requestStringResponse(url, cancel);

            if (cancel.get()) return;
            if (response == null) continue;

            Reader r = new StringReader(response);
            Collection<? extends R> decoded;

            try {
                decoded = decode(ri, r);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                continue;
            } finally {
                try {
                    r.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            synchronized (this) {
                if (cancel.get()) return;
                results.addAll(decoded);
            }
        }
    }

    public void cancel() {
    }

    public synchronized void cleanup() {
        if (currentWorker != null) {
            cancel.set(true);
            currentWorker.cancel();
        }
        mostGenericQueryText = null;
        results = null;
        cancel = null;
        currentWorker = null;
    }

    protected static interface ResultWrapper<R> {

        public void setMessage(String message);
        public void addResult(R r);

    }

    protected static interface SimpleNameable {
        public String getSimpleName();
    }

    private class ComputeResult implements Runnable, Cancellable {

        private final String text;
        private final SearchType searchType;
        private final AtomicBoolean cancel;

        public ComputeResult(String text, SearchType searchType, AtomicBoolean cancel) {
            this.text = text;
            this.searchType = searchType;
            this.cancel = cancel;
        }

        @Override public void run() {
            compute(text, searchType == SearchType.EXACT_NAME ? SearchType.PREFIX : searchType, cancel);
        }

        @Override
        public boolean cancel() {
            cancel.set(true);
            return true;
        }
    }

}
