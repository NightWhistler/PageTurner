/*
 * Copyright (C) 2011 Alex Kuiper
 * 
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */
package net.nightwhistler.pageturner;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.QueryResultAdapter;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class LibraryActivity extends ListActivity implements OnItemSelectedListener {
	
	private LibraryService libraryService;
	
	private BookAdapter bookAdapter;
	
	private ArrayAdapter<String> menuAdapter;
		
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
	
	private ProgressDialog waitDialog;
	private ProgressDialog importDialog;
		
	private Drawable backupCover;
	
	private int lastPosition;
	
	private static final String[] MENU_ITEMS = {
		"Most recently read books", "Most recently added books", "Unread books", "All books by title", "All books by author" 
	};	
	
	private static final Logger LOG = LoggerFactory.getLogger(LibraryActivity.class); 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		this.libraryService = new SqlLiteLibraryService(this);
		
		this.bookAdapter = new BookAdapter(this);
		this.menuAdapter = new ArrayAdapter<String>(this, R.layout.menu_row, 
				R.id.bookTitle, MENU_ITEMS);
				
		setListAdapter(menuAdapter);
				
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		
		this.importDialog = new ProgressDialog(this);
		this.importDialog.setOwnerActivity(this);
		
		this.backupCover = getResources().getDrawable(R.drawable.river_diary );
		
		registerForContextMenu(getListView());		
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		if ( this.getListAdapter() == this.menuAdapter ) {
			return;
		}
		
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		final LibraryBook selectedBook = bookAdapter.getResultAt(info.position);
		
		MenuItem detailsItem = menu.add( "View details");
		
		detailsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent( LibraryActivity.this, BookDetailsActivity.class );
				intent.putExtra("book", selectedBook);				
				startActivity(intent);					
				return true;
			}
		});
		
		MenuItem deleteItem = menu.add("Delete from library");
		
		deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				libraryService.deleteBook( selectedBook.getFileName() );
				new LoadBooksTask().execute(lastPosition);
				return true;					
			}
		});				
		
	}	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add("Scan folder for books");
		item.setIcon( getResources().getDrawable(R.drawable.folder) );
		
		MenuItem item2 = menu.add("Show bookcase");
		item2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(LibraryActivity.this, BookCaseActivity.class);
				startActivity(intent);
				return true;
			}
		});
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");

		try {
			startActivityForResult(intent, 1);
		} catch (ActivityNotFoundException e) {
			new ImportBooksTask().execute(new File("/sdcard"));  
		}

		return true;
	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

    	if ( resultCode == RESULT_OK && data != null) {
    		// obtain the filename
    		Uri fileUri = data.getData();
    		if (fileUri != null) {
    			String filePath = fileUri.getPath();
    			if (filePath != null) {
    				new ImportBooksTask().execute(new File(filePath));    				
    			}
    		}
    	}	
	}	
	
	@Override
	protected void onStop() {		
		this.libraryService.close();	
		this.waitDialog.dismiss();
		this.importDialog.dismiss();
		super.onStop();
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
				
	}	
	
	@Override
	public void onBackPressed() {
		if ( getListAdapter() == this.bookAdapter ) {			
			setListAdapter(this.menuAdapter);
		} else {
			finish();
		}	
	}	
	
	@Override
	protected void onPause() {
		
		this.bookAdapter.clear();
		this.libraryService.close();
		//We clear the list to free up memory.
		
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();		
		
		if ( getListAdapter() == this.bookAdapter ) {
			new LoadBooksTask().execute(this.lastPosition);
		}
	}
	
	/**
	 * Based on example found here:
	 * http://www.vogella.de/articles/AndroidListView/article.html
	 * 
	 * @author work
	 *
	 */
	private class BookAdapter extends QueryResultAdapter<LibraryBook> {	
		
		private Context context;
		
		public BookAdapter(Context context) {
			this.context = context;
		}		
		
		
		@Override
		public View getView(int index, LibraryBook book, View convertView,
				ViewGroup parent) {
			
			View rowView;
			
			if ( convertView == null ) {			
				LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.book_row, parent, false);
			} else {
				rowView = convertView;
			}
			
			TextView titleView = (TextView) rowView.findViewById(R.id.bookTitle);
			TextView authorView = (TextView) rowView.findViewById(R.id.bookAuthor);
			TextView dateView = (TextView) rowView.findViewById(R.id.addedToLibrary);
			
			ImageView imageView = (ImageView) rowView.findViewById(R.id.bookCover);
						
			authorView.setText("by " + book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName() );
			titleView.setText(book.getTitle());
			
			dateView.setText( "Added on " + DATE_FORMAT.format(book.getAddedToLibrary()));
			
			if ( book.getCoverImage() != null ) {
				byte[] cover = book.getCoverImage();
				imageView.setImageBitmap( BitmapFactory.decodeByteArray(cover, 0, cover.length ));
			} else {
				imageView.setImageDrawable(backupCover);
			}
			
			return rowView;
		}	
	
	}

	
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		
		if ( getListAdapter() == menuAdapter ) {
			handleMenuClick(position);
		} else {
		
			LibraryBook book = this.bookAdapter.getResultAt(position);
		
			Intent intent = new Intent(this, ReadingActivity.class);
		
			intent.setData( Uri.parse(book.getFileName()));
			this.setResult(RESULT_OK, intent);
				
			startActivityIfNeeded(intent, 99);
		}

	}
	
	private void handleMenuClick(int position) {
		this.lastPosition = position;
		this.bookAdapter.clear();		
		
		this.setListAdapter(bookAdapter);
		new LoadBooksTask().execute(position);
	}
	
	private class ImportBooksTask extends AsyncTask<File, Integer, Void> {	
		
		private boolean hadError = false;
		
		private static final int UPDATE_FOLDER = 1;
		private static final int UPDATE_IMPORT = 2;
		
		private int foldersScanned = 0;
		
		private boolean oldKeepScreenOn;
			
		@Override
		protected void onPreExecute() {
			importDialog.setTitle("Importing books...");
			importDialog.setMessage("Scanning for EPUB files.");
			importDialog.show();
			
			this.oldKeepScreenOn = getListView().getKeepScreenOn();
			getListView().setKeepScreenOn(true);
		}		
		
		@Override
		protected Void doInBackground(File... params) {
			File parent = params[0];
			List<File> books = new ArrayList<File>();			
			findEpubsInFolder(parent, books);
			
			int total = books.size();
			int i = 0;			
	        
			while ( i < books.size() ) {
				
				File book = books.get(i);
				
				LOG.info("Importing: " + book.getAbsolutePath() );
				try {
					if ( ! libraryService.hasBook(book.getAbsolutePath() ) ) {
						importBook( book.getAbsolutePath() );
					}					
				} catch (OutOfMemoryError oom ) {
					hadError = true;
					return null;
				}
				
				i++;
				publishProgress(UPDATE_IMPORT, i, total);
			}
			
			return null;
		}
		
		private void findEpubsInFolder( File folder, List<File> items) {
			
			if ( folder == null ) {
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
		        				
				Book importedBook = epubReader.readEpubLazy(file, "UTF-8", Arrays.asList(MediatypeService.mediatypes));								
				
	        	libraryService.storeBook(file, importedBook, false);	        		        	
				
			} catch (IOException io ) {}
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			
			if ( values[0] == UPDATE_IMPORT ) {
				importDialog.setMessage("Importing " + values[1] + " / " + values[2]);
			} else {
				importDialog.setMessage("Scanning for EPUB files.\nFolders scanned: " + values[1] );
			}
		}
		
		@Override
		protected void onPostExecute(Void result) {
			importDialog.hide();
			
			if ( hadError ) {
				AlertDialog.Builder builder = new AlertDialog.Builder(LibraryActivity.this);
				builder.setTitle("Error while importing books.");
				builder.setMessage( "Could not import all books. Please try again." );
				builder.setNeutralButton("OK", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();						
					}
				});
				
				builder.show();
			}
			
			getListView().setKeepScreenOn(oldKeepScreenOn);
			new LoadBooksTask().execute(1);
		}
	}
	
	private class LoadBooksTask extends AsyncTask<Integer, Integer, QueryResult<LibraryBook>> {		
		
		@Override
		protected void onPreExecute() {
			waitDialog.setTitle("Loading library...");
			waitDialog.show();
		}
		
		@Override
		protected QueryResult<LibraryBook> doInBackground(Integer... params) {
			switch ( params[0] ) {			
			case 1:
				return libraryService.findAllByLastAdded();
			case 2:
				return libraryService.findUnread();
			case 3:
				return libraryService.findAllByTitle();
			case 4:
				return libraryService.findAllByAuthor();
			default:
				return libraryService.findAllByLastRead();
			}			
		}
		
		@Override
		protected void onPostExecute(QueryResult<LibraryBook> result) {
			bookAdapter.setResult(result);
			waitDialog.hide();			
		}
		
	}
	
	
	
}
