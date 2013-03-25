package net.nightwhistler.pageturner.catalog;

import net.nightwhistler.nucular.atom.Entry;
import net.nightwhistler.nucular.atom.Feed;

public interface LoadFeedCallback {
	
	void setNewFeed( Feed feed );
	
	void loadFakeFeed( Entry entry );
	
	void errorLoadingFeed( String error );
		
	void notifyLinkUpdated();
}
