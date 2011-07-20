/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Sun Microsystems, Inc. All rights reserved.
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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */

package org.netbeans.modules.jackpot30.remoting.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.annotations.common.CheckForNull;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

/**
 *
 */
public class WebUtilities {

    private WebUtilities() {
    }

    private static final RequestProcessor LOADER = new RequestProcessor(WebUtilities.class.getName(), 100, true, false);

    public static @CheckForNull String requestStringResponse (final URI uri) {
        return requestStringResponse(uri, new AtomicBoolean());
    }

    public static @CheckForNull String requestStringResponse (final URI uri, AtomicBoolean cancel) {
        final String[] result = new String[1];
        Task task = LOADER.create(new Runnable() {
            @Override
            public void run() {
        final StringBuffer sb = new StringBuffer ();
        final URL url;
        try {
            url = uri.toURL();
            final URLConnection urlConnection = url.openConnection ();
            urlConnection.connect ();
            final Object content = urlConnection.getContent ();
            final InputStream inputStream = (InputStream) content;
            final BufferedReader reader = new BufferedReader (new InputStreamReader (inputStream, "ASCII"));
            try {
                for (;;) {
                    String line = reader.readLine ();
                    if (line == null)
                        break;
                    sb.append (line).append ('\n');
                }
            } finally {
                reader.close ();
            }
            result[0] = sb.toString();
        } catch (IOException e) {
            e.printStackTrace ();  // TODO
        }
            }
        });

        task.schedule(0);
        
        while (!cancel.get()) {
            try {
                if (task.waitFinished(1000)) return result[0];
            } catch (InterruptedException ex) {
                Logger.getLogger(WebUtilities.class.getName()).log(Level.FINE, null, ex);
            }
        }
        return null;
    }

    public static Collection<? extends String> requestStringArrayResponse (URI uri) {
        return requestStringArrayResponse(uri, new AtomicBoolean());
    }

    public static Collection<? extends String> requestStringArrayResponse (URI uri, AtomicBoolean cancel) {
        String content = requestStringResponse(uri, cancel);
        
        if (content == null) return null;
        
        return Arrays.asList(content.split("\n"));
    }

    private static String[] c = new String[] {"&", "<", ">", "\n", "\""}; // NOI18N
    private static String[] tags = new String[] {"&amp;", "&lt;", "&gt;", "<br>", "&quot;"}; // NOI18N

    public static String escapeForHTMLElement(String input) {
        for (int cntr = 0; cntr < c.length; cntr++) {
            input = input.replaceAll(c[cntr], tags[cntr]);
        }

        return input;
    }

    public static String escapeForQuery(String pattern) throws URISyntaxException {
        if (pattern == null) return null;
        return new URI(null, null, null, -1, null, pattern, null).getRawQuery().replaceAll(Pattern.quote("&"), Matcher.quoteReplacement("%26"));
    }

}
