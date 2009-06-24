package org.netbeans.modules.jackpot30.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 */
public class WebUtilities {

    private WebUtilities() {
    }

    public static String requestStringResponse (URI uri) {
        final StringBuffer sb = new StringBuffer ();
        final URL url;
        try {
            url = uri.toURL();
            final URLConnection urlConnection = url.openConnection ();
            urlConnection.connect ();
            final Object content = urlConnection.getContent ();
//            System.out.println (content);
//            System.out.println (content.getClass ());
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
        } catch (IOException e) {
            e.printStackTrace ();  // TODO
        }
        return sb.toString ();
    }
    
    public static Iterable<String> requestStringArrayResponse (URI uri) {
        final List<String> result = new LinkedList<String> ();
        final URL url;
        try {
            url = uri.toURL();
            final URLConnection urlConnection = url.openConnection ();
            urlConnection.connect ();
            final Object content = urlConnection.getContent ();
//            System.out.println (content);
//            System.out.println (content.getClass ());
            final InputStream inputStream = (InputStream) content;
            final BufferedReader reader = new BufferedReader (new InputStreamReader (inputStream, "ASCII"));
            try {
                for (;;) {
                    String line = reader.readLine ();
                    if (line == null)
                        break;
                    result.add (line);
                }
            } finally {
                reader.close ();
            }
        } catch (IOException e) {
            e.printStackTrace ();  // TODO
        }
        return result;
    }

}
