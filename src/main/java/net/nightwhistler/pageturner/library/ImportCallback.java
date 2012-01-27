package net.nightwhistler.pageturner.library;

import java.util.List;

public interface ImportCallback {

	void importComplete( int booksImported );
	
	void importCompleteWithFailures( int booksImported, List<String> failures );
	
	void importFailed( String reason );
	
	void importCancelled();
	
}
