package org.netbeans.modules.jackpot30.file.test;

import junit.framework.TestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataObject;

public class DeclarativeHintsTestDataObjectTest extends TestCase {

    public DeclarativeHintsTestDataObjectTest(String testName) {
        super(testName);
    }

    public void testDataObject() throws Exception {
        FileObject root = Repository.getDefault().getDefaultFileSystem().getRoot();
        FileObject template = root.getFileObject("Templates/Other/DeclarativeHintsTestTemplate.test");
        assertNotNull("Template file shall be found", template);

        DataObject obj = DataObject.find(template);
        assertEquals("It is our data object", DeclarativeHintsTestDataObject.class, obj.getClass());
    }
}
