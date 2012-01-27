package net.nightwhistler.pageturner.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.activity.LibraryActivity;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;

public class ImportTask extends AsyncTask<File, Integer, Void> implements OnCancelListener {	
	
	private Context context;
	private LibraryService libraryService;
	private ImportCallback callBack;	
	
	private List<String> errors = new ArrayList<String>();
	
	private static final Logger LOG = LoggerFactory.getLogger(ImportTask.class);
	
	private static final int UPDATE_FOLDER = 1;
	private static final int UPDATE_IMPORT = 2;
	
	private int foldersScanned = 0;
	private int booksImported = 0;
	
	private boolean oldKeepScreenOn;
	private boolean keepRunning;	
	
	@Override
	protected void onPreExecute() {
		
		importDialog.setOnCancelListener(this);
		importDialog.show();			
		
				
		this.oldKeepScreenOn = listView.getKeepScreenOn();
		listView.setKeepScreenOn(true);
	}		
	
	@Override
	public void onCancel(DialogInterface dialog) {
		LOG.debug("User aborted import.");		
	}
	
	@Override
	protected Void doInBackground(File... params) {
		File parent = params[0];
		List<File> books = new ArrayList<File>();			
		findEpubsInFolder(parent, books);
		
		int total = books.size();
		int i = 0;			
        
		while ( i < books.size() && keepRunning ) {
			
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
			
			boolean copy = settings.getBoolean("copy_to_library", true);
        	libraryService.storeBook(file, importedBook, false, copy);	        		        	
			
		} catch (Exception io ) {
			errors.add( file + ": " + io.getMessage() );
			LOG.error("Error while reading book: " + file, io); 
		}
	}
	
	@Override
	protected void onProgressUpdate(Integer... values) {
		
		if ( values[0] == UPDATE_IMPORT ) {
			String importing = String.format(getString(R.string.importing), values[1], values[2]);
			importDialog.setMessage(importing);
		} else {
			String message = String.format(getString(R.string.scan_folders), values[1]);
			importDialog.setMessage(message);
		}
	}
	
	@Override
	protected void onPostExecute(Void result) {
		
		importDialog.hide();			
		
		OnClickListener dismiss = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();						
			}
		};
		
		//If the user cancelled the import, don't bug him/her with alerts.
		if ( (! errors.isEmpty()) && ! isCancelled() ) {
			AlertDialog.Builder builder = new AlertDialog.Builder(LibraryActivity.this);
			builder.setTitle(R.string.import_errors);
			
			builder.setItems( errors.toArray(new String[errors.size()]), null );				
			
			builder.setNeutralButton(android.R.string.ok, dismiss );
			
			builder.show();
		}
		
		listView.setKeepScreenOn(oldKeepScreenOn);
		
		if ( booksImported > 0 ) {			
			//Switch to the "recently added" view.
			if ( spinner.getSelectedItemPosition() == Selections.LAST_ADDED.ordinal() ) {
				new LoadBooksTask().execute(Selections.LAST_ADDED);
			} else {
				spinner.setSelection(Selections.LAST_ADDED.ordinal());
			}
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(LibraryActivity.this);
			builder.setTitle(R.string.no_books_found);
			builder.setMessage( getString(R.string.no_bks_fnd_text) );
			builder.setNeutralButton(android.R.string.ok, dismiss);
			
			builder.show();
		}
	}
}
