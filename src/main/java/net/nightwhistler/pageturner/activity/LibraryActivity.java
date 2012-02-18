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
package net.nightwhistler.pageturner.activity;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.Configuration.LibraryView;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.ImportCallback;
import net.nightwhistler.pageturner.library.ImportTask;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.QueryResultAdapter;
import net.nightwhistler.pageturner.view.BookCaseView;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import nl.siegmann.epublib.service.MediatypeService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.inject.Inject;

public class LibraryActivity extends RoboActivity implements ImportCallback {
	
	@Inject 
	private LibraryService libraryService;
	
	@InjectView(R.id.librarySpinner)
	private Spinner spinner;
	
	@InjectView(R.id.libraryList)
	private ListView listView;
	
	@InjectView(R.id.bookCaseView)
	private BookCaseView bookCaseView;
	
	@InjectResource(R.drawable.river_diary)
	private Drawable backupCover;
	
	@InjectView(R.id.libHolder)
	private ViewSwitcher switcher;
	
	@Inject
	private Configuration config;
		
	private static enum Selections {
		 BY_LAST_READ, LAST_ADDED, UNREAD, BY_TITLE, BY_AUTHOR;
	}	
	
	private static final int[] ICONS = { R.drawable.book_binoculars,
		R.drawable.book_add, R.drawable.book_star,
		R.drawable.book, R.drawable.user };
	
	private QueryResultAdapter<LibraryBook> bookAdapter;
		
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG);
	
	private ProgressDialog waitDialog;
	private ProgressDialog importDialog;	
	
	private AlertDialog importQuestion;
	
	private boolean askedUserToImport;
	private boolean oldKeepScreenOn;
	
	private SharedPreferences settings;
	
	private Selections lastSelection = Selections.LAST_ADDED;
	
	private static final Logger LOG = LoggerFactory.getLogger(LibraryActivity.class); 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_menu);
		
		if ( savedInstanceState != null ) {
			this.askedUserToImport = savedInstanceState.getBoolean("import_q", false);
		}
		
		if ( config.getLibraryView() == LibraryView.BOOKCASE ) {
			this.bookAdapter = new BookCaseAdapter(this);
			this.bookCaseView.setAdapter(bookAdapter);
			if ( switcher.getDisplayedChild() == 0 ) {
				switcher.showNext();
			}
		} else {		
			this.bookAdapter = new BookListAdapter(this);
			this.listView.setAdapter(bookAdapter);
		}
				
		this.settings = PreferenceManager.getDefaultSharedPreferences(this); 
						
		ArrayAdapter<String> adapter = new QueryMenuAdapter(this, 
				getResources().getStringArray(R.array.libraryQueries));
		
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(new MenuSelectionListener());	
						
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		
		this.importDialog = new ProgressDialog(this);
		
		this.importDialog.setOwnerActivity(this);
		importDialog.setTitle(R.string.importing_books);
		importDialog.setMessage(getString(R.string.scanning_epub));
		
		registerForContextMenu(this.listView);		
	}	
	
	private void onBookClicked( LibraryBook book ) {
		
		Intent intent = new Intent(this, ReadingActivity.class);
		
		intent.setData( Uri.parse(book.getFileName()));
		this.setResult(RESULT_OK, intent);
				
		startActivityIfNeeded(intent, 99);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		int pos;
		
		if ( menuInfo != null ) {		
			AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
			pos = info.position;
		} else {
			pos = (Integer) v.getTag();
		}
		
		final LibraryBook selectedBook = bookAdapter.getResultAt(pos);
		
		MenuItem detailsItem = menu.add( R.string.view_details);
		
		detailsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent( LibraryActivity.this, BookDetailsActivity.class );
				intent.putExtra("book", selectedBook.getFileName());				
				startActivity(intent);					
				return true;
			}
		});
		
		MenuItem deleteItem = menu.add(R.string.delete);
		
		deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				libraryService.deleteBook( selectedBook.getFileName() );
				new LoadBooksTask().execute(lastSelection);
				return true;					
			}
		});				
		
	}	
	
	private void startImport(File startFolder) {
		boolean copy = settings.getBoolean("copy_to_library", true);
		ImportTask importTask = new ImportTask(this, libraryService, this, copy);
		importDialog.setOnCancelListener(importTask);
		importDialog.show();		
				
		this.oldKeepScreenOn = listView.getKeepScreenOn();
		listView.setKeepScreenOn(true);
		
		importTask.execute(startFolder);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		MenuItem item2 = menu.add("Switch view");
		item2.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				
				if ( switcher.getDisplayedChild() == 0 ) {
					bookAdapter = new BookCaseAdapter(LibraryActivity.this);
					bookCaseView.setAdapter(bookAdapter);	
					config.setLibraryView(LibraryView.BOOKCASE);
				} else {
					bookAdapter = new BookListAdapter(LibraryActivity.this);
					listView.setAdapter(bookAdapter);
					config.setLibraryView(LibraryView.LIST);
				}
				
				switcher.showNext();
				new LoadBooksTask().execute(lastSelection);
				return true;				
            }
        });
		
        MenuItem prefs = menu.add(R.string.prefs);
		prefs.setIcon( getResources().getDrawable(R.drawable.cog) );
		
		prefs.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(LibraryActivity.this, PageTurnerPrefsActivity.class);
				startActivity(intent);
				
				return true;
			}
		});
		
		MenuItem item = menu.add(R.string.scan_books);
		item.setIcon( getResources().getDrawable(R.drawable.book_refresh) );
		
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");

				try {
					startActivityForResult(intent, 1);
				} catch (ActivityNotFoundException e) {
					startImport(new File("/sdcard"));  
				}

				return true;
			}
		});		
		
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
    				startImport(new File(filePath));    				
    			}
    		}
    	}	
	}	
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("import_q", askedUserToImport);
	}
	
	@Override
	protected void onStop() {		
		this.libraryService.close();	
		this.waitDialog.dismiss();
		this.importDialog.dismiss();
		super.onStop();
	}
	
	@Override
	public void onBackPressed() {
		finish();			
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
		
		if ( spinner.getSelectedItemPosition() != this.lastSelection.ordinal() ) {
			spinner.setSelection(this.lastSelection.ordinal());
		} else {
			new LoadBooksTask().execute(this.lastSelection);
		}
	}
	
	@Override
	public void importCancelled() {
		
		listView.setKeepScreenOn(oldKeepScreenOn);
		
		//Switch to the "recently added" view.
		if ( spinner.getSelectedItemPosition() == Selections.LAST_ADDED.ordinal() ) {
			new LoadBooksTask().execute(Selections.LAST_ADDED);
		} else {
			spinner.setSelection(Selections.LAST_ADDED.ordinal());
		}
	}
	
	@Override
	public void importComplete(int booksImported, List<String> errors) {
		
		importDialog.hide();			
		
		OnClickListener dismiss = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();						
			}
		};
		
		//If the user cancelled the import, don't bug him/her with alerts.
		if ( (! errors.isEmpty()) ) {
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
	
	
	@Override
	public void importFailed(String reason) {
		
	}
	
	@Override
	public void importStatusUpdate(String update) {
		importDialog.setMessage(update);
	}
	
	
	/**
	 * Based on example found here:
	 * http://www.vogella.de/articles/AndroidListView/article.html
	 * 
	 * @author work
	 *
	 */
	private class BookListAdapter extends QueryResultAdapter<LibraryBook> {	
		
		private Context context;
		
		public BookListAdapter(Context context) {
			this.context = context;
		}		
		
		
		@Override
		public View getView(int index, final LibraryBook book, View convertView,
				ViewGroup parent) {
			
			View rowView;
			
			if ( convertView == null ) {			
				LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.book_row, parent, false);
			} else {
				rowView = convertView;
			}
			
			rowView.setOnClickListener( new View.OnClickListener() {
				
				public void onClick(View v) {				
					onBookClicked(book);					
				}
			});
			
			TextView titleView = (TextView) rowView.findViewById(R.id.bookTitle);
			TextView authorView = (TextView) rowView.findViewById(R.id.bookAuthor);
			TextView dateView = (TextView) rowView.findViewById(R.id.addedToLibrary);
			TextView progressView = (TextView) rowView.findViewById(R.id.readingProgress);
			
			ImageView imageView = (ImageView) rowView.findViewById(R.id.bookCover);
						
			String authorText = String.format(getString(R.string.book_by),
					book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName() );
			
			authorView.setText(authorText);
			titleView.setText(book.getTitle());
			
			if ( book.getProgress() > 0 ) {
				progressView.setText( "" + book.getProgress() + "%");
			} else {
				progressView.setText("");
			}			
			
			String dateText = String.format(getString(R.string.added_to_lib),
					DATE_FORMAT.format(book.getAddedToLibrary()));
			dateView.setText( dateText );
			
			if ( book.getCoverImage() != null ) {				
				imageView.setImageBitmap(book.getCoverImage());
			} else {
				imageView.setImageDrawable(backupCover);
			}
			
			return rowView;
		}	
	
	}	
	
	private class BookCaseAdapter extends QueryResultAdapter<LibraryBook> {
		
		private Context context;
		
		public BookCaseAdapter(Context context) {
			this.context = context;
		}
		
		@Override
		public View getView(int index, final LibraryBook object, View convertView,
				ViewGroup parent) {
			
			View result;
		
			if ( convertView == null ) {				
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(
						Context.LAYOUT_INFLATER_SERVICE);
				result = inflater.inflate(R.layout.bookcase_row, parent, false);
				
			} else {
				result = convertView;
			}
			
			registerForContextMenu(result);
			
			result.setTag(index);
			
			result.setOnClickListener( new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onBookClicked(object);					
				}
			});	
			
			
			ImageView image = (ImageView) result.findViewById(R.id.bookCover);				
			Bitmap bitmap = object.getCoverImage();
			
			if ( bitmap != null ) {			
				image.setImageBitmap( object.getCoverImage() );
			} else {
				image.setImageDrawable(backupCover);
			}		
			
			return result;
		}
	}
	
	private class QueryMenuAdapter extends ArrayAdapter<String> {
		
		String[] strings;
		
		public QueryMenuAdapter(Context context, String[] strings) {
			super(context, android.R.layout.simple_spinner_item, strings);	
			this.strings = strings;
		}
		
		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			
			View rowView;
			
			if ( convertView == null ) {			
				LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.menu_row, parent, false);
			} else {
				rowView = convertView;
			}
			
			TextView textView = (TextView) rowView.findViewById(R.id.menuText);
			textView.setText(strings[position]);
			textView.setTextColor(Color.BLACK);
			
			ImageView img = (ImageView) rowView.findViewById(R.id.icon);
			
			img.setImageResource(ICONS[position]);
			
			return rowView;			
		}
	}
	
	private void buildImportQuestionDialog() {
		
		if ( importQuestion != null ) {
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(LibraryActivity.this);
		builder.setTitle(R.string.no_books_found);
		builder.setMessage( getString(R.string.scan_bks_question) );
		builder.setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();		
				startImport(new File("/sdcard"));
			}
		});
		
		builder.setNegativeButton(android.R.string.no, new android.content.DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();						
				importQuestion = null;
			}
		});				
		
		this.importQuestion = builder.create();
	}
	
	private class MenuSelectionListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			
			Selections newSelections = Selections.values()[pos];
			
			lastSelection = newSelections;
			bookAdapter.clear();		
						
			new LoadBooksTask().execute(newSelections);
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
						
		}
	}	

	private class LoadBooksTask extends AsyncTask<Selections, Integer, QueryResult<LibraryBook>> {		
		
		private Selections sel;
		
		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(R.string.loading_library);
			waitDialog.show();
		}
		
		@Override
		protected QueryResult<LibraryBook> doInBackground(Selections... params) {
			
			this.sel = params[0];
			
			switch ( sel ) {			
			case LAST_ADDED:
				return libraryService.findAllByLastAdded();
			case UNREAD:
				return libraryService.findUnread();
			case BY_TITLE:
				return libraryService.findAllByTitle();
			case BY_AUTHOR:
				return libraryService.findAllByAuthor();
			default:
				return libraryService.findAllByLastRead();
			}			
		}
		
		@Override
		protected void onPostExecute(QueryResult<LibraryBook> result) {
			bookAdapter.setResult(result);
			waitDialog.hide();			
			
			if ( sel == Selections.LAST_ADDED && result.getSize() == 0 && ! askedUserToImport ) {
				askedUserToImport = true;
				buildImportQuestionDialog();
				importQuestion.show();
			}
		}
		
	}
	
	
	
}
