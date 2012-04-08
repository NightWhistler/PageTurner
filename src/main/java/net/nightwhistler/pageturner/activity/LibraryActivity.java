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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.Configuration.LibrarySelection;
import net.nightwhistler.pageturner.Configuration.LibraryView;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.ImportCallback;
import net.nightwhistler.pageturner.library.ImportTask;
import net.nightwhistler.pageturner.library.KeyedQueryResult;
import net.nightwhistler.pageturner.library.KeyedResultAdapter;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.QueryResultAdapter;
import net.nightwhistler.pageturner.view.BookCaseView;
import net.nightwhistler.pageturner.view.FastBitmapDrawable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.google.inject.Inject;

public class LibraryActivity extends RoboActivity implements ImportCallback, OnItemClickListener {
	
	@Inject 
	private LibraryService libraryService;
	
	@InjectView(R.id.librarySpinner)
	private Spinner spinner;
	
	@InjectView(R.id.libraryList)
	private ListView listView;
	
	@InjectView(R.id.bookCaseView)
	private BookCaseView bookCaseView;
		
	@InjectView(R.id.alphabetList)
	private ListView alphabetBar;
	
	@InjectView(R.id.alphabetDivider)
	private ImageView alphabetDivider;
	
	@InjectView(R.id.libHolder)
	private ViewSwitcher switcher;
	
	@Inject
	private Configuration config;

	private Drawable backupCover;
	private Handler handler;
	
	private static final int[] ICONS = { R.drawable.book_binoculars,
		R.drawable.book_add, R.drawable.book_star,
		R.drawable.book, R.drawable.user };
	
	private KeyedResultAdapter bookAdapter;
		
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG);
	private static final int ALPHABET_THRESHOLD = 20;
	
	private ProgressDialog waitDialog;
	private ProgressDialog importDialog;	
	
	private AlertDialog importQuestion;
	
	private boolean askedUserToImport;
	private boolean oldKeepScreenOn;
	
	private static final Logger LOG = LoggerFactory.getLogger(LibraryActivity.class); 
	
	private IntentCallBack intentCallBack;
	private List<CoverCallback> callbacks = new ArrayList<CoverCallback>();
	private Map<String, Drawable> coverCache = new HashMap<String, Drawable>();
	
	private interface IntentCallBack {
		void onResult( int resultCode, Intent data );
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_menu);
		
		Bitmap backupBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.unknown_cover );
		this.backupCover = new FastBitmapDrawable(backupBitmap);
		
		this.handler = new Handler();
				
		if ( savedInstanceState != null ) {
			this.askedUserToImport = savedInstanceState.getBoolean("import_q", false);
		}
		
		this.bookCaseView.setOnScrollListener( new CoverScrollListener() );
		this.listView.setOnScrollListener( new CoverScrollListener() );
		this.alphabetBar.setFocusable(true);
		
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
		this.listView.setOnItemClickListener(this);
		
		setAlphabetBarVisible(false);
	}	
	
	private void onBookClicked( LibraryBook book ) {
		
		Intent intent = new Intent(this, ReadingActivity.class);
		
		intent.setData( Uri.parse(book.getFileName()));
		this.setResult(RESULT_OK, intent);
				
		startActivityIfNeeded(intent, 99);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos,
			long id) {
		onBookClicked(this.bookAdapter.getResultAt(pos));
	}	
	
	private Bitmap getCover( LibraryBook book ) {
		return BitmapFactory.decodeByteArray(book.getCoverImage(), 0, book.getCoverImage().length );
	}
	
	private void showBookDetails( final LibraryBook libraryBook ) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.book_details);
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.book_details, null);
		builder.setView( layout );
		
		ImageView coverView = (ImageView) layout.findViewById(R.id.coverImage );
		
		if ( libraryBook.getCoverImage() != null ) {			
			coverView.setImageBitmap( getCover(libraryBook) );
		} else {			
			coverView.setImageDrawable( getResources().getDrawable(R.drawable.unknown_cover));
		}

		TextView titleView = (TextView) layout.findViewById(R.id.titleField);
		TextView authorView = (TextView) layout.findViewById(R.id.authorField);
		TextView lastRead = (TextView) layout.findViewById(R.id.lastRead);
		TextView added = (TextView) layout.findViewById(R.id.addedToLibrary);
		TextView descriptionView = (TextView) layout.findViewById(R.id.bookDescription);
		
		titleView.setText(libraryBook.getTitle());
		String authorText = String.format( getString(R.string.book_by),
				 libraryBook.getAuthor().getFirstName() + " " 
				 + libraryBook.getAuthor().getLastName() );
		authorView.setText( authorText );

		if (libraryBook.getLastRead() != null && ! libraryBook.getLastRead().equals(new Date(0))) {
			String lastReadText = String.format(getString(R.string.last_read),
					DATE_FORMAT.format(libraryBook.getLastRead()));
			lastRead.setText( lastReadText );
		} else {
			String lastReadText = String.format(getString(R.string.last_read), getString(R.string.never_read));
			lastRead.setText( lastReadText );
		}

		String addedText = String.format( getString(R.string.added_to_lib),
				DATE_FORMAT.format(libraryBook.getAddedToLibrary()));
		added.setText( addedText );
		descriptionView.setText(new HtmlSpanner().fromHtml( libraryBook.getDescription()));		
		
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(R.string.read, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onBookClicked(libraryBook);				
			}
		});
		
		builder.show();
	}
	
	private void showDownloadDialog() {
		
		final List<String> names = new ArrayList<String>(){{ 
				add("Feedbooks");
				add("Manybooks.net");
				add("Gutenberg.org");
				}};
		
		final List<String> addresses = new ArrayList<String>(){{
				add("http://www.feedbooks.com/site/free_books.atom");
		
				add("http://www.manybooks.net/opds/index.php");
				//"http://www.allromanceebooks.com/epub-feed.xml",
				//"http://bookserver.archive.org/catalog/",
				add("http://m.gutenberg.org/ebooks/?format=opds"); }};
		
		if ( config.getCalibreServer().length() != 0 ) {
			names.add("Calibre server");
			addresses.add(config.getCalibreServer());
		}
				

    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.download);    	
    	
    	builder.setItems(names.toArray(new String[names.size()]),
    			new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int item) {
    			Intent intent = new Intent(LibraryActivity.this, CatalogActivity.class);
    			
    			intent.putExtra("url", addresses.get(item));
    			    					
    			startActivityIfNeeded(intent, 99);
    		}
    	});

    	builder.show();
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
				showBookDetails(selectedBook);				
				return true;
			}
		});
		
		MenuItem deleteItem = menu.add(R.string.delete);
		
		deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				libraryService.deleteBook( selectedBook.getFileName() );
				new LoadBooksTask().execute(config.getLastLibraryQuery());
				return true;					
			}
		});				
		
	}	
	
	private void startImport(File startFolder, boolean copy) {		
		ImportTask importTask = new ImportTask(this, libraryService, this, copy);
		importDialog.setOnCancelListener(importTask);
		importDialog.show();		
				
		this.oldKeepScreenOn = listView.getKeepScreenOn();
		listView.setKeepScreenOn(true);
		
		importTask.execute(startFolder);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ( this.intentCallBack != null ) {
			this.intentCallBack.onResult(resultCode, data);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {		
		
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.library_menu, menu);        
       		
		OnMenuItemClickListener toggleListener = new OnMenuItemClickListener() {
			
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
				new LoadBooksTask().execute(config.getLastLibraryQuery());
				return true;				
            }
        };
        
        MenuItem shelves = menu.findItem(R.id.shelves_view);        
        shelves.setOnMenuItemClickListener(toggleListener);
        
        MenuItem list = menu.findItem(R.id.list_view);        
        list.setOnMenuItemClickListener(toggleListener);
		
        MenuItem prefs = menu.findItem(R.id.preferences);		
		
		prefs.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(LibraryActivity.this, PageTurnerPrefsActivity.class);
				startActivity(intent);
				
				return true;
			}
		});
		
		MenuItem scan = menu.findItem(R.id.scan_books);		
		
		scan.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {	
				showImportDialog();
				return true;
			}
		});		
		
		MenuItem about = menu.findItem(R.id.about);
		about.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Dialogs.showAboutDialog(LibraryActivity.this);
				return true;
			}
		});
		
		MenuItem download = menu.findItem(R.id.download);
		download.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				showDownloadDialog();
				return true;
			}
		});
		
		return true;
	}	
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
		boolean bookCaseActive = switcher.getDisplayedChild() != 0;
		
		menu.findItem(R.id.shelves_view).setVisible(! bookCaseActive);
		menu.findItem(R.id.list_view).setVisible(bookCaseActive);
		
		return true;
	}
	
	private void showImportDialog() {
		AlertDialog.Builder builder;		
		
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View layout = inflater.inflate(R.layout.import_dialog, null);
		final RadioButton scanSpecific = (RadioButton) layout.findViewById(R.id.radioScanFolder);
		final TextView folder = (TextView) layout.findViewById(R.id.folderToScan);
		final CheckBox copyToLibrary = (CheckBox) layout.findViewById(R.id.copyToLib);		
		final Button browseButton = (Button) layout.findViewById(R.id.browseButton);
		
		folder.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				scanSpecific.setChecked(true);				
			}
		});			
		
		//Copy default setting from the prefs
		copyToLibrary.setChecked( config.isCopyToLibrayEnabled() );
		
		builder = new AlertDialog.Builder(this);
		builder.setView(layout);
		
		OnClickListener okListener = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.dismiss();
				
				if ( scanSpecific.isChecked() ) {
					startImport(new File(folder.getText().toString()), copyToLibrary.isChecked() );
				} else {
					startImport(new File("/sdcard"), copyToLibrary.isChecked());
				}				
			}
		};
		
		View.OnClickListener browseListener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				scanSpecific.setChecked(true);				
				Intent intent = new Intent(LibraryActivity.this, FileBrowseActivity.class);
				intent.setData( Uri.parse(folder.getText().toString() ));
				startActivityForResult(intent, 0);
			}
		};
		
		this.intentCallBack = new IntentCallBack() {
			
			@Override
			public void onResult(int resultCode, Intent data) {
				if ( resultCode == RESULT_OK && data != null ) {
					folder.setText(data.getData().getPath());
				}
			}
		};		
		
		browseButton.setOnClickListener(browseListener);
		
		builder.setTitle(R.string.import_books);
		builder.setPositiveButton(android.R.string.ok, okListener );
		builder.setNegativeButton(android.R.string.cancel, null );
		
		builder.show();
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
		
		LibrarySelection lastSelection = config.getLastLibraryQuery();
		
		if ( spinner.getSelectedItemPosition() != lastSelection.ordinal() ) {
			spinner.setSelection(lastSelection.ordinal());
		} else {
			new LoadBooksTask().execute(lastSelection);
		}
	}
	
	@Override
	public void importCancelled() {
		
		listView.setKeepScreenOn(oldKeepScreenOn);
		
		//Switch to the "recently added" view.
		if ( spinner.getSelectedItemPosition() == LibrarySelection.LAST_ADDED.ordinal() ) {
			new LoadBooksTask().execute(LibrarySelection.LAST_ADDED);
		} else {
			spinner.setSelection(LibrarySelection.LAST_ADDED.ordinal());
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
			if ( spinner.getSelectedItemPosition() == LibrarySelection.LAST_ADDED.ordinal() ) {
				new LoadBooksTask().execute(LibrarySelection.LAST_ADDED);
			} else {
				spinner.setSelection(LibrarySelection.LAST_ADDED.ordinal());
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
		importDialog.hide();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.import_failed);
		builder.setMessage(reason);
		builder.setNeutralButton(android.R.string.ok, null);
		builder.show();
	}
	
	@Override
	public void importStatusUpdate(String update) {
		importDialog.setMessage(update);
	}
	
	public void onAlphabetBarClick( KeyedQueryResult<LibraryBook> result, Character c ) {
		
		int index = result.getOffsetFor(c);
		
		if ( index == -1 ) {
			return;
		}
		
		if ( config.getLibraryView() == LibraryView.BOOKCASE ) {
			this.bookCaseView.setSelection(index);
		} else {			
			this.listView.setSelection(index);				
		}		
	}	
	
	
	/**
	 * Based on example found here:
	 * http://www.vogella.de/articles/AndroidListView/article.html
	 * 
	 * @author work
	 *
	 */
	private class BookListAdapter extends KeyedResultAdapter {	
		
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
			
			TextView titleView = (TextView) rowView.findViewById(R.id.bookTitle);
			TextView authorView = (TextView) rowView.findViewById(R.id.bookAuthor);
			TextView dateView = (TextView) rowView.findViewById(R.id.addedToLibrary);
			TextView progressView = (TextView) rowView.findViewById(R.id.readingProgress);
			
			final ImageView imageView = (ImageView) rowView.findViewById(R.id.bookCover);
						
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
			
			loadCover(imageView, book, index);			
			
			return rowView;
		}	
	
	}	
	
	private void loadCover( ImageView imageView, LibraryBook book, int index ) {
		Drawable draw = coverCache.get(book.getFileName());			
		
		if ( draw != null ) {
			imageView.setImageDrawable(draw);
		} else {
			
			imageView.setImageDrawable(backupCover);
			
			if ( book.getCoverImage() != null ) {				
				callbacks.add( new CoverCallback(book, index, imageView ) );	
			}
		}
	}	
	
	private class CoverScrollListener implements AbsListView.OnScrollListener {
		private Runnable lastRunnable;
		
		private int lastSection = -1;
		
		@Override
		public void onScroll(AbsListView view, final int firstVisibleItem,
				final int visibleItemCount, final int totalItemCount) {
			
			if ( visibleItemCount == 0  ) {
				return;
			}
			
			if ( this.lastRunnable != null ) {
				handler.removeCallbacks(lastRunnable);
			}	
			
			this.lastRunnable = new Runnable() {
				
				@Override
				public void run() {
					List<CoverCallback> localList = new ArrayList<CoverCallback>( callbacks );
					callbacks.clear();
					
					int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
					
					LOG.debug( "Loading items " + firstVisibleItem + " to " + lastVisibleItem + " of " + totalItemCount );
					
					for ( CoverCallback callback: localList ) {
						if ( callback.viewIndex >= firstVisibleItem && callback.viewIndex <= lastVisibleItem ) {
							callback.run();
						}
					}
					
					if ( bookAdapter.isKeyed() ) {
						
						int section = bookAdapter.getSectionForPosition(firstVisibleItem);
						
						if ( section != lastSection) {

							for ( int i=0; i < alphabetBar.getChildCount(); i++ ) {
								alphabetBar.getChildAt(i).setBackgroundColor(android.R.color.background_dark);
							}

							alphabetBar.setSelection( section );
							lastSection = section;

							if ( alphabetBar.getChildAt(section) != null ) {				
								View child = alphabetBar.getChildAt(section);				
								child.setBackgroundColor( Color.BLUE );
							}
						}
					}
						
				}
			};
				
			handler.postDelayed(lastRunnable, 550);
		}
		
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
					
		}
		
	}
	
	private class CoverCallback {
		protected LibraryBook book;
		protected int viewIndex;
		protected ImageView view;	
		
		public CoverCallback(LibraryBook book, int viewIndex, ImageView view) {
			this.book = book;
			this.view = view;
			this.viewIndex = viewIndex;			
		}
		
		public void run() {			
			try {
				FastBitmapDrawable drawable = new FastBitmapDrawable(getCover(book));			
				view.setImageDrawable(drawable);	
				coverCache.put(book.getFileName(), drawable);
			} catch (OutOfMemoryError err) {
				coverCache.clear();
			}
		}
	}
	
	private class BookCaseAdapter extends KeyedResultAdapter {
		
		private Context context;
		
		public BookCaseAdapter(Context context) {
			this.context = context;
		}	
		
		@Override
		public View getView(final int index, final LibraryBook object, View convertView,
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
			
			
			final ImageView image = (ImageView) result.findViewById(R.id.bookCover);
			image.setImageDrawable(backupCover);
			TextView text = (TextView) result.findViewById(R.id.bookLabel);
			text.setText( object.getTitle() );
			text.setBackgroundResource(R.drawable.alphabet_bar_bg_dark);			
			
			loadCover(image, object, index);		
			
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
				//startImport(new File("/sdcard"), config.isCopyToLibrayEnabled());
				showImportDialog();
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
	
	private void setAlphabetBarVisible( boolean visible ) {
		
		int vis = visible ? View.VISIBLE : View.GONE; 
		
		alphabetBar.setVisibility(vis);
		alphabetDivider.setVisibility(vis);		
		listView.setFastScrollEnabled(visible);
	}
	
	private class MenuSelectionListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int pos,
				long arg3) {
			
			LibrarySelection newSelections = LibrarySelection.values()[pos];
			
			config.setLastLibraryQuery(newSelections);
			
			bookAdapter.clear();
			new LoadBooksTask().execute(newSelections);
		}
		
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
						
		}
	}	

	private class LoadBooksTask extends AsyncTask<Configuration.LibrarySelection, Integer, QueryResult<LibraryBook>> {		
		
		private Configuration.LibrarySelection sel;
		
		@Override
		protected void onPreExecute() {
			waitDialog.setTitle(R.string.loading_library);
			waitDialog.show();
			
			coverCache.clear();
		}
		
		@Override
		protected QueryResult<LibraryBook> doInBackground(Configuration.LibrarySelection... params) {
			
			Exception storedException = null;
			
			for ( int i=0; i < 3; i++ ) {

				try {

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
				} catch (SQLiteException sql) {
					storedException = sql;
					try {
						//Sometimes the database is still locked.
						Thread.sleep(1000);
					} catch (InterruptedException in) {}
				}				
			}
			
			throw new RuntimeException( "Failed after 3 attempts", storedException ); 
		}		
		
		
		@Override
		protected void onPostExecute(QueryResult<LibraryBook> result) {
			bookAdapter.setResult(result);			
			
			if ( result instanceof KeyedQueryResult && result.getSize() >= ALPHABET_THRESHOLD ) {
				
				final KeyedQueryResult<LibraryBook> keyedResult = (KeyedQueryResult<LibraryBook>) result;
				
				ArrayAdapter<Character> adapter = new ArrayAdapter<Character>(LibraryActivity.this,
						R.layout.alphabet_line, R.id.alphabetLabel,	keyedResult.getAlphabet() );
				
				alphabetBar.setAdapter(adapter);
				
				alphabetBar.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> arg0, View arg1,
							int arg2, long arg3) {
						Character c = keyedResult.getAlphabet().get(arg2);
						onAlphabetBarClick(keyedResult, c);
					}
				});				
				
				setAlphabetBarVisible(true);
			} else {
				setAlphabetBarVisible(false);								
			}			
			
			waitDialog.hide();			
			
			if ( sel == Configuration.LibrarySelection.LAST_ADDED && result.getSize() == 0 && ! askedUserToImport ) {
				askedUserToImport = true;
				buildImportQuestionDialog();
				importQuestion.show();
			}
		}
		
	}
	
}
