package net.nightwhistler.pageturner.view;

import android.graphics.Color;

public interface TextSelectionCallback {

	void lookupWikipedia( String text );
	
	void lookupDictionary( String text );
	
	void lookupGoogle( String text );
	
	boolean isDictionaryAvailable();
	
	void highLight( int from, int to, Color color );
	
}
