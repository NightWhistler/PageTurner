package net.nightwhistler.pageturner.catalog;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;

public interface LoadFeedCallback {

    public static enum ResultType { REPLACE, APPEND }
	
	void setNewFeed( Feed feed, ResultType resultType );

	void errorLoadingFeed( String error );
		
	void notifyLinkUpdated();

    void onLoadingStart();

    void onLoadingDone();
}
