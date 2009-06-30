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

}