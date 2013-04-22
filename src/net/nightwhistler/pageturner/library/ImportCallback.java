package net.nightwhistler.pageturner.library;

import android.os.AsyncTask;

import java.util.List;

public interface ImportCallback {

    void booksDeleted(int numberOfDeletedBooks);

	void importComplete( int booksImported, List<String> failures, boolean emptyLibrary, boolean silent );
	
	void importStatusUpdate( String update, boolean silent );
		
	void importFailed( String reason, boolean silent );

    void taskCompleted( AsyncTask<?,?,?> task, boolean wasCancelled );
	
}
