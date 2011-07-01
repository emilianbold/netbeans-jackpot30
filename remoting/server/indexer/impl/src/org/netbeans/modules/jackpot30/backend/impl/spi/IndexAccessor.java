/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.jackpot30.backend.impl.spi;

import org.apache.lucene.index.IndexWriter;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

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

    public String getPath(FileObject file) {
        return FileUtil.getRelativePath(root, file);
    }

    public static IndexAccessor current;
    public static IndexAccessor getCurrent() {
        return current;
    }
}
