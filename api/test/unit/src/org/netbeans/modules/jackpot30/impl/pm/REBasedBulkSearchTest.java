package org.netbeans.modules.jackpot30.impl.pm;

/**
 *
 * @author lahvac
 */
public class REBasedBulkSearchTest extends BulkSearchTestPerformer {

    public REBasedBulkSearchTest(String name) {
        super(name);
    }

    @Override
    protected BulkSearch createSearch() {
        return new REBasedBulkSearch();
    }

    public void testSynchronizedAndMultiStatementVariables() throws Exception {
        //XXX
    }

    public void testJackpot30_2() throws Exception {
        //XXX
    }

    @Override
    public void testIdentifierInPureMemberSelect() throws Exception {
        //XXX
    }

}