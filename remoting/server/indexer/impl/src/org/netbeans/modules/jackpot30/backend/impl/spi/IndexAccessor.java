/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.jackpot30.backend.impl.spi;

import java.net.URISyntaxException;
import java.net.URL;
import org.apache.lucene.index.IndexWriter;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author lahvac
 */
public class IndexAccessor {

    private final FileObject root;
    private final IndexWriter w;

    public IndexAccessor(IndexWriter w, FileObject root) {
        this.w = w;
        this.root = root;
    }

    public IndexWriter getIndexWriter() {
        return w;
    }

    public String getPath(URL file) {
        try {
            return root.toURI().relativize(file.toURI()).toString();
        } catch (URISyntaxException ex) {
            Exceptions.printStackTrace(ex);
        }

        return file.toExternalForm();
    }

    public boolean isAcceptable(URL file) {
        return file.toString().startsWith(root.toURL().toString());
    }

    public static IndexAccessor current;
    public static IndexAccessor getCurrent() {
        return current;
    }
}
