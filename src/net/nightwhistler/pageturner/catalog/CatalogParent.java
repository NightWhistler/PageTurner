package net.nightwhistler.pageturner.catalog;

import net.nightwhistler.nucular.atom.Feed;

/**
 * Created with IntelliJ IDEA.
 * User: alex
 * Date: 4/25/13
 * Time: 7:17 AM
 * To change this template use File | Settings | File Templates.
 */
public interface CatalogParent {

    void loadFakeFeed( Feed fakeFeed );

    void loadFeedFromUrl( String url );

    void onFeedReplaced( Feed feed );

}
