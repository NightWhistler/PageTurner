package net.nightwhistler.pageturner.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.nightwhistler.pageturner.R;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;

public class ImportTask extends AsyncTask<File, Integer, Void> implements OnCancelListener {	
	
	private Context context;
	private LibraryService libraryService;
	private ImportCallback callBack;	
	
	private boolean copyToLibrary;
	
	private List<String> errors = new ArrayList<String>();
	
	private static final Logger LOG = LoggerFactory.getLogger(ImportTask.class);
	
	private static final int UPDATE_FOLDER = 1;
	private static final int UPDATE_IMPORT = 2;
	
	private int foldersScanned = 0;
	private int booksImported = 0;
	
	private String importFailed = null;
	
	public ImportTask( Context context, LibraryService libraryService,
			ImportCallback callBack, boolean copyToLibrary ) {
		this.context = context;
		this.libraryService = libraryService;
		this.callBack = callBack;
		this.copyToLibrary = copyToLibrary;
	}		
	
	@Override
	public void onCancel(DialogInterface dialog) {
		LOG.debug("User aborted import.");	
		this.cancel(true);
	}
	
	@Override
	protected Void doInBackground(File... params) {
		File parent = params[0];
		
		if ( ! parent.exists() ) {
			importFailed = String.format( context.getString(R.string.no_such_folder), parent.getPath());			
			return null;
		}
		
		List<File> books = new ArrayList<File>();			
		findEpubsInFolder(parent, books);
		
		int total = books.size();
		int i = 0;			
        
		while ( i < books.size() && ! isCancelled() ) {
			
			File book = books.get(i);
			
			LOG.info("Importing: " + book.getAbsolutePath() );
			try {
				if ( ! libraryService.hasBook(book.getName() ) ) {
					importBook( book.getAbsolutePath() );
				}					
			} catch (OutOfMemoryError oom ) {
				errors.add(book.getName() + ": Out of memory.");
				return null;
			}
			
			i++;
			publishProgress(UPDATE_IMPORT, i, total);
			booksImported++;
		}
		
		return null;
	}
	
	private void findEpubsInFolder( File folder, List<File> items) {
		
		if ( folder == null || folder.getAbsolutePath().startsWith(LibraryService.BASE_LIB_PATH) ) {
			return;
		}			
		
		if ( isCancelled() ) {
			return;
		}		
		
		if ( folder.isDirectory() && folder.listFiles() != null) {
			
			for (File child: folder.listFiles() ) {
				findEpubsInFolder(child, items); 
			}
			
			foldersScanned++;
			publishProgress(UPDATE_FOLDER, foldersScanned);
			
		} else {
			if ( folder.getName().endsWith(".epub") ) {
				items.add(folder);
			}
		}
	}
	
	private void importBook(String file) {
		try {
			// read epub file
	        EpubReader epubReader = new EpubReader();
	        				
			Book importedBook = epubReader.readEpubLazy(file, "UTF-8", 
					Arrays.asList(MediatypeService.mediatypes));							
			
        	libraryService.storeBook(file, importedBook, false, this.copyToLibrary);	        		        	
			
		} catch (Exception io ) {
			errors.add( file + ": " + io.getMessage() );
			LOG.error("Error while reading book: " + file, io); 
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		
		String message;
		
		if ( values[0] == UPDATE_IMPORT ) {
			message = String.format(context.getString(R.string.importing), values[1], values[2]);			
		} else {
			message = String.format(context.getString(R.string.scan_folders), values[1]);			
		}
		
		callBack.importStatusUpdate(message);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		
		if ( importFailed != null ) {
			callBack.importFailed(importFailed);
		} else if ( ! isCancelled() ) {
			this.callBack.importComplete(booksImported, errors);
		}		
	}
}
