package org.netbeans.modules.jackpot30.impl.pm;

/**
 *
 * @author lahvac
 */
public class CopyFinderBasedBulkSearchTest extends BulkSearchTestPerformer {

    public CopyFinderBasedBulkSearchTest(String name) {
        super(name);
    }

    @Override
    protected BulkSearch createSearch() {
        return new CopyFinderBasedBulkSearch();
    }

    @Override
    protected boolean verifyIndexingData() {
        return false;
    }

    @Override
    public void testSerialization() throws Exception {
        //XXX
    }

    @Override
    public void testNoExponentialTimeComplexity() throws Exception {
        //XXX
    }

}