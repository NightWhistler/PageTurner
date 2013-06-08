/*
 * Copyright (C) 2012 Alex Kuiper
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.widget.SearchView;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;
import com.google.inject.Inject;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.Configuration.ColourProfile;
import net.nightwhistler.pageturner.Configuration.LibrarySelection;
import net.nightwhistler.pageturner.Configuration.LibraryView;
import net.nightwhistler.pageturner.PlatformUtil;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.*;
import net.nightwhistler.pageturner.scheduling.QueueableAsyncTask;
import net.nightwhistler.pageturner.scheduling.TaskQueue;
import net.nightwhistler.pageturner.view.BookCaseView;
import net.nightwhistler.pageturner.view.FastBitmapDrawable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import roboguice.inject.InjectView;

import java.io.File;
import java.text.DateFormat;
import java.util.*;

import static net.nightwhistler.pageturner.PlatformUtil.isIntentAvailable;

public class LibraryFragment extends RoboSherlockFragment implements ImportCallback, OnItemClickListener,
        OnItemLongClickListener, DialogFactory.SearchCallBack, TaskQueue.TaskQueueListener {

    protected static final int REQUEST_CODE_GET_CONTENT = 2;
	
	@Inject 
	private LibraryService libraryService;

    @Inject
    private DialogFactory dialogFactory;
	
	@InjectView(R.id.libraryList)
	private ListView listView;
	
	@InjectView(R.id.bookCaseView)
	private BookCaseView bookCaseView;
		
	@InjectView(R.id.alphabetList)
	private ListView alphabetBar;
	
	private AlphabetAdapter alphabetAdapter;
	
	@InjectView(R.id.alphabetDivider)
	private ImageView alphabetDivider;
	
	@InjectView(R.id.libHolder)
	private ViewSwitcher switcher;
	
	@Inject
	private Configuration config;

    @Inject
    private TaskQueue taskQueue;

	private Drawable backupCover;
	private Handler handler;
		
	private KeyedResultAdapter bookAdapter;
		
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG);
	private static final int ALPHABET_THRESHOLD = 20;
	
	private ProgressDialog waitDialog;
	private ProgressDialog importDialog;	
	
	private AlertDialog importQuestion;
	
	private boolean askedUserToImport;
	private boolean oldKeepScreenOn;
	
	private static final Logger LOG = LoggerFactory.getLogger("LibraryActivity");
	
	private IntentCallBack intentCallBack;
	private List<CoverCallback> callbacks = new ArrayList<CoverCallback>();
	private Map<String, FastBitmapDrawable> coverCache = new HashMap<String, FastBitmapDrawable>();

    private MenuItem searchMenuItem;

	private interface IntentCallBack {
		void onResult( int resultCode, Intent data );
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {			
		super.onCreate(savedInstanceState);		
		
		Bitmap backupBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.unknown_cover );
		this.backupCover = new FastBitmapDrawable(backupBitmap);
		
		this.handler = new Handler();
				
		if ( savedInstanceState != null ) {
			this.askedUserToImport = savedInstanceState.getBoolean("import_q", false);
		}

        this.taskQueue.setTaskQueueListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_library, container, false);
	}


	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setHasOptionsMenu(true);
		this.bookCaseView.setOnScrollListener( new CoverScrollListener() );
		this.listView.setOnScrollListener( new CoverScrollListener() );
		
		if ( config.getLibraryView() == LibraryView.BOOKCASE ) {
			
			this.bookAdapter = new BookCaseAdapter();
			this.bookCaseView.setAdapter(bookAdapter);			
			
			if ( switcher.getDisplayedChild() == 0 ) {
				switcher.showNext();
			}
		} else {		
			this.bookAdapter = new BookListAdapter(getActivity());
			this.listView.setAdapter(bookAdapter);
		}

		this.waitDialog = new ProgressDialog(getActivity());
		this.waitDialog.setOwnerActivity(getActivity());
		
		this.importDialog = new ProgressDialog(getActivity());
		
		this.importDialog.setOwnerActivity(getActivity());
		importDialog.setTitle(R.string.importing_books);
		importDialog.setMessage(getString(R.string.scanning_epub));
		registerForContextMenu(this.listView);	
		this.listView.setOnItemClickListener(this);
		this.listView.setOnItemLongClickListener(this);
		
		setAlphabetBarVisible(false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(actionBar.getThemedContext(),
				android.R.layout.simple_list_item_1,
				android.R.id.text1, getResources().getStringArray(R.array.libraryQueries));

		actionBar.setListNavigationCallbacks(adapter, new MenuSelectionListener() );


        refreshView();
        executeTask(new CleanFilesTask(this, libraryService, config));
        executeTask(new ImportTask(getActivity(), libraryService, this, config, config.isCopyToLibrayEnabled(),
                true), new File(config.getLibraryFolder()) );
	}

    private <A,B,C> void executeTask( QueueableAsyncTask<A,B,C> task, A... parameters ) {
        setSupportProgressBarIndeterminateVisibility(true);
        this.taskQueue.executeTask(task, parameters);
    }

    @Override
    public void queueEmpty() {
        setSupportProgressBarIndeterminateVisibility(false);
    }

    private void clearCoverCache() {
		for ( Map.Entry<String, FastBitmapDrawable> draw: coverCache.entrySet() ) {
			draw.getValue().destroy();
		}
		
		coverCache.clear();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

        if ( config.getLongShortPressBehaviour() == Configuration.LongShortPressBehaviour.NORMAL ) {
            showBookDetails(this.bookAdapter.getResultAt(position));
        } else {
            openBook(this.bookAdapter.getResultAt(position));
        }
	}	
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {

        if ( config.getLongShortPressBehaviour() == Configuration.LongShortPressBehaviour.NORMAL ) {
            openBook(this.bookAdapter.getResultAt(position));
        } else {
            showBookDetails(this.bookAdapter.getResultAt(position));
        }

		return true;
	}
	
	private FastBitmapDrawable getCover( LibraryBook book ) {

        try {

            if ( !coverCache.containsKey(book.getFileName() ) ) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(book.getCoverImage(), 0, book.getCoverImage().length );
                FastBitmapDrawable drawable = new FastBitmapDrawable(bitmap);
                coverCache.put( book.getFileName(), drawable );
            }

            return coverCache.get( book.getFileName() );

        } catch ( OutOfMemoryError outOfMemoryError ) {
            clearCoverCache();
            return null;
        }
	}
	
	private void showBookDetails( final LibraryBook libraryBook ) {

        if ( ! isAdded() ) {
            return;
        }
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.book_details);
		LayoutInflater inflater = PlatformUtil.getLayoutInflater(getActivity());
		
		View layout = inflater.inflate(R.layout.book_details, null);
		builder.setView( layout );
		
		ImageView coverView = (ImageView) layout.findViewById(R.id.coverImage );

		if ( libraryBook.getCoverImage() != null ) {

            Drawable coverDrawable = getCover(libraryBook);

            if ( coverDrawable != null ) {
                coverView.setImageDrawable(coverDrawable);
            } else {
                coverView.setImageDrawable(getResources().getDrawable(R.drawable.unknown_cover));
            }
        }

		TextView titleView = (TextView) layout.findViewById(R.id.titleField);
		TextView authorView = (TextView) layout.findViewById(R.id.authorField);
		TextView lastRead = (TextView) layout.findViewById(R.id.lastRead);
		TextView added = (TextView) layout.findViewById(R.id.addedToLibrary);
		TextView descriptionView = (TextView) layout.findViewById(R.id.bookDescription);
		TextView fileName = (TextView) layout.findViewById(R.id.fileName);
		
		titleView.setText(libraryBook.getTitle());
		String authorText = String.format( getString(R.string.book_by),
				 libraryBook.getAuthor().getFirstName() + " " 
				 + libraryBook.getAuthor().getLastName() );
		authorView.setText( authorText );
		fileName.setText( libraryBook.getFileName() );

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

        HtmlSpanner spanner = new HtmlSpanner();
        spanner.unregisterHandler("img" ); //We don't want to render images

		descriptionView.setText(spanner.fromHtml( libraryBook.getDescription()));
		
		builder.setNeutralButton(R.string.delete, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				libraryService.deleteBook( libraryBook.getFileName() );
				refreshView();
				dialog.dismiss();			
			}
		});			
		
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setPositiveButton(R.string.read, new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				openBook(libraryBook);						
			}
		});
		
		builder.show();
	}
	
	private void openBook(LibraryBook libraryBook) {
		Intent intent = new Intent(getActivity(), ReadingActivity.class);
		
		intent.setData( Uri.parse(libraryBook.getFileName()));
		getActivity().setResult(Activity.RESULT_OK, intent);
				
		getActivity().startActivityIfNeeded(intent, 99);		
	}
	
		
	private void startImport(File startFolder, boolean copy) {		
		ImportTask importTask = new ImportTask(getActivity(), libraryService, this, config, copy, false);
		importDialog.setOnCancelListener(importTask);
		importDialog.show();		
				
		this.oldKeepScreenOn = listView.getKeepScreenOn();
		listView.setKeepScreenOn(true);

        executeTask( importTask, startFolder );
	}


    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ( this.intentCallBack != null ) {
			this.intentCallBack.onResult(resultCode, data);
		}
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {		
        inflater.inflate(R.menu.library_menu, menu);        
       		
		OnMenuItemClickListener toggleListener = new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				
				if ( switcher.getDisplayedChild() == 0 ) {
					bookAdapter = new BookCaseAdapter();
					bookCaseView.setAdapter(bookAdapter);	
					config.setLibraryView(LibraryView.BOOKCASE);					
				} else {
					bookAdapter = new BookListAdapter(getActivity());
					listView.setAdapter(bookAdapter);
					config.setLibraryView(LibraryView.LIST);					
				}
				
				switcher.showNext();
				refreshView();
				return true;				
            }
        };
        
        MenuItem shelves = menu.findItem(R.id.shelves_view);        
        shelves.setOnMenuItemClickListener(toggleListener);
        
        MenuItem list = menu.findItem(R.id.list_view);        
        list.setOnMenuItemClickListener(toggleListener);
		
        menu.findItem(R.id.preferences)		
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(getActivity(), PageTurnerPrefsActivity.class);
				startActivity(intent);
				
				return true;
			}
		});
		
		menu.findItem(R.id.scan_books)		
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {	
				showImportDialog();
				return true;
			}
		});		
		
		menu.findItem(R.id.about)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				dialogFactory.buildAboutDialog().show();
				return true;
			}
		});
		
		menu.findItem(R.id.download)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Intent intent = new Intent(getActivity(), CatalogActivity.class);    					
    			getActivity().startActivityIfNeeded(intent, 99);
    		
				return true;
			}
		});
		
		menu.findItem(R.id.profile_day)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switchToColourProfile(ColourProfile.DAY);
				return true;
			}
		});
		
		menu.findItem(R.id.profile_night)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switchToColourProfile(ColourProfile.NIGHT);
				return true;
			}
		});

        this.searchMenuItem = menu.findItem(R.id.menu_search);
        if (searchMenuItem != null) {
            final SearchView searchView = (SearchView) searchMenuItem.getActionView();

            if (searchView != null) {

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String query) {
                        performSearch(query);
                        return true;
                    }
                } );
            } else {
                searchMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        dialogFactory.showSearchDialog(R.string.search_library, R.string.enter_query, LibraryFragment.this);
                        return false;
                    }
                });
            }
        }


        // Only show open file item if we have a file manager installed
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (isIntentAvailable(getActivity(), intent)) {
            menu.findItem(R.id.open_file).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    launchFileManager();
                    return true;
                }
            });
        } else {
            menu.findItem(R.id.open_file).setVisible(false);
        }

	}

    private void launchFileManager() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        this.intentCallBack = new IntentCallBack() {

            @Override
            public void onResult(int resultCode, Intent data) {
                if ( resultCode == Activity.RESULT_OK && data != null ) {
                    Intent readingIntent = new Intent( getActivity(), ReadingActivity.class);
                    readingIntent.setData(data.getData());
                    getActivity().setResult(Activity.RESULT_OK, readingIntent);

                    getActivity().startActivityIfNeeded(readingIntent, 99);
                }
            }
        };

        try {
            startActivityForResult(intent, REQUEST_CODE_GET_CONTENT);
        } catch (ActivityNotFoundException e) {
            // No compatible file manager was found.
            Toast.makeText(getActivity(), getString(R.string.install_oi),
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void onSearchRequested() {
        if ( this.searchMenuItem != null && searchMenuItem.getActionView() != null ) {
            this.searchMenuItem.expandActionView();
            this.searchMenuItem.getActionView().requestFocus();
        } else {
            dialogFactory.showSearchDialog(R.string.search_library, R.string.enter_query, LibraryFragment.this);
        }
    }


    @Override
    public void performSearch(String query) {
        if ( query != null ) {
            executeTask(new LoadBooksTask(config.getLastLibraryQuery(), query.toString()));
        }
    }
	
	private void switchToColourProfile( ColourProfile profile ) {
		config.setColourProfile(profile);
		Intent intent = new Intent(getActivity(), LibraryActivity.class);		
		startActivity(intent);
		onStop();
		getActivity().finish();
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean bookCaseActive = config.getLibraryView() == LibraryView.BOOKCASE;
		
		menu.findItem(R.id.shelves_view).setVisible(! bookCaseActive);
		menu.findItem(R.id.list_view).setVisible(bookCaseActive);
		menu.findItem(R.id.profile_day).setVisible(config.getColourProfile() == ColourProfile.NIGHT);
		menu.findItem(R.id.profile_night).setVisible(config.getColourProfile() == ColourProfile.DAY);
	}
	
	private void showImportDialog() {
		AlertDialog.Builder builder;		
		
		LayoutInflater inflater = PlatformUtil.getLayoutInflater(getActivity());
		final View layout = inflater.inflate(R.layout.import_dialog, null);
		final RadioButton scanSpecific = (RadioButton) layout.findViewById(R.id.radioScanFolder);
		final TextView folder = (TextView) layout.findViewById(R.id.folderToScan);
		final CheckBox copyToLibrary = (CheckBox) layout.findViewById(R.id.copyToLib);		
		final Button browseButton = (Button) layout.findViewById(R.id.browseButton);
		
		folder.setText( config.getStorageBase() + "/eBooks" );
		
		folder.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				scanSpecific.setChecked(true);				
			}
		});			
		
		//Copy default setting from the prefs
		copyToLibrary.setChecked( config.isCopyToLibrayEnabled() );
		
		builder = new AlertDialog.Builder(getActivity());
		builder.setView(layout);
		
		OnClickListener okListener = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.dismiss();
				
				if ( scanSpecific.isChecked() ) {
					startImport(new File(folder.getText().toString()), copyToLibrary.isChecked() );
				} else {
					startImport(new File(config.getStorageBase()), copyToLibrary.isChecked());
				}				
			}
		};
		
		View.OnClickListener browseListener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				scanSpecific.setChecked(true);				
				Intent intent = new Intent(getActivity(), FileBrowseActivity.class);
				intent.setData( Uri.parse(folder.getText().toString() ));
				startActivityForResult(intent, 0);
			}
		};
		
		this.intentCallBack = new IntentCallBack() {
			
			@Override
			public void onResult(int resultCode, Intent data) {
				if ( resultCode == Activity.RESULT_OK && data != null ) {
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
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("import_q", askedUserToImport);
	}
	
	@Override
	public void onStop() {
		this.libraryService.close();	
		this.waitDialog.dismiss();
		this.importDialog.dismiss();
		super.onStop();
	}
	
	public void onBackPressed() {
		getActivity().finish();			
	}	
	
	@Override
	public void onPause() {
		
		this.bookAdapter.clear();
		//We clear the list to free up memory.

        this.taskQueue.clear();
		this.clearCoverCache();
		
		super.onPause();
	}
	
	
	@Override
	public void onResume() {
		super.onResume();				
		
		LibrarySelection lastSelection = config.getLastLibraryQuery();
		
		ActionBar actionBar = getSherlockActivity().getSupportActionBar();
		
		if (actionBar.getSelectedNavigationIndex() != lastSelection.ordinal() ) {
			actionBar.setSelectedNavigationItem(lastSelection.ordinal());
		} else {
            executeTask(new LoadBooksTask(lastSelection));
		}
	}
	
	@Override
	public void importComplete(int booksImported, List<String> errors, boolean emptyLibrary, boolean silent) {
		
		if ( !isAdded() || getActivity() == null ) {
			return;
		}

        if ( silent ) {
            if ( booksImported > 0 ) {
                //Schedule refresh without clearing the queue
                executeTask(new LoadBooksTask(config.getLastLibraryQuery()));
            }
            return;
        }
		
		importDialog.hide();			
		
		OnClickListener dismiss = new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();						
			}
		};
		
		//If the user cancelled the import, don't bug him/her with alerts.
		if ( (! errors.isEmpty()) ) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.import_errors);
			
			builder.setItems( errors.toArray(new String[errors.size()]), null );				
			
			builder.setNeutralButton(android.R.string.ok, dismiss );
			
			builder.show();
		}
		
		listView.setKeepScreenOn(oldKeepScreenOn);
		
		if ( booksImported > 0 ) {			
			
			//Switch to the "recently added" view.
			if (getSherlockActivity().getSupportActionBar().getSelectedNavigationIndex() == LibrarySelection.LAST_ADDED.ordinal() ) {
				loadView(LibrarySelection.LAST_ADDED, "importComplete()");
			} else {
				getSherlockActivity().getSupportActionBar().setSelectedNavigationItem(LibrarySelection.LAST_ADDED.ordinal());
			}
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.no_books_found);

            if ( emptyLibrary ) {
                builder.setMessage( getString(R.string.no_bks_fnd_text) );
            } else {
                builder.setMessage( getString(R.string.no_new_books_found));
            }

            builder.setNeutralButton(android.R.string.ok, dismiss);
			
			builder.show();
		}
	}	
	
	
	@Override
	public void importFailed(String reason, boolean silent) {
		
		if (silent || !isAdded() || getActivity() == null ) {
			return;
		}
		
		importDialog.hide();
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.import_failed);
		builder.setMessage(reason);
		builder.setNeutralButton(android.R.string.ok, null);
		builder.show();
	}
	
	@Override
	public void importStatusUpdate(String update, boolean silent) {
		
		if (silent || !isAdded() || getActivity() == null ) {
			return;
		}
		
		importDialog.setMessage(update);
	}	
	
	public void onAlphabetBarClick( KeyedQueryResult<LibraryBook> result, Character c ) {
		
		int index = result.getOffsetFor(Character.toUpperCase(c));
		
		if ( index == -1 ) {
			return;
		}
		
		if ( alphabetAdapter != null ) {		
			alphabetAdapter.setHighlightChar(c);
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
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

    private void loadView( LibrarySelection selection, String from ) {
        LOG.debug("Loading view: " + selection + " from " + from);
        this.taskQueue.clear();
        executeTask(new LoadBooksTask(selection));
    }

    private void refreshView() {
        LOG.debug("View refresh requested");
        loadView(config.getLastLibraryQuery(), "refreshView()");
    }

    @Override
    public void booksDeleted(int numberOfDeletedBooks) {

        LOG.debug("Got " + numberOfDeletedBooks + " deleted books.");

        if ( numberOfDeletedBooks > 0 ) {

            //Schedule a refresh without clearing the task queue
            executeTask(new LoadBooksTask(config.getLastLibraryQuery()));
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
		private Character lastCharacter;

        private Drawable holoDrawable;

        public  CoverScrollListener() {
            try {
                this.holoDrawable = getResources().getDrawable(R.drawable.list_activated_holo);
            } catch (IllegalStateException i) {
                //leave it null
            }
        }
		
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
					
					if ( bookAdapter.isKeyed() ) {
						
						String key = bookAdapter.getKey( firstVisibleItem );
						Character keyChar = null;
						
						if (key != null && key.length() > 0 ) {
							keyChar = Character.toUpperCase( key.charAt(0) );
						}						
						
						if (keyChar != null && ! keyChar.equals(lastCharacter)) {

							lastCharacter = keyChar;
							List<Character> alphabet = bookAdapter.getAlphabet();
							
							//If the highlight-char is already set, this means the 
							//user clicked the bar, so don't scroll it.
							if (alphabetAdapter != null && ! keyChar.equals( alphabetAdapter.getHighlightChar() ) ) {
								alphabetAdapter.setHighlightChar(keyChar);
								alphabetBar.setSelection( alphabet.indexOf(keyChar) );
							}
							
							for ( int i=0; i < alphabetBar.getChildCount(); i++ ) {
								View child = alphabetBar.getChildAt(i);
								if ( child.getTag().equals(keyChar) ) {
									child.setBackgroundDrawable( holoDrawable );
								} else {
									child.setBackgroundDrawable(null);
								}
							}							
						}						
					}
					
					List<CoverCallback> localList = new ArrayList<CoverCallback>( callbacks );
					callbacks.clear();
					
					int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
					
					LOG.debug( "Loading items " + firstVisibleItem + " to " + lastVisibleItem + " of " + totalItemCount );
					
					for ( CoverCallback callback: localList ) {
						if ( callback.viewIndex >= firstVisibleItem && callback.viewIndex <= lastVisibleItem ) {
							callback.run();
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
                FastBitmapDrawable drawable = getCover(book);

                if ( drawable != null ) {
                    view.setImageDrawable(drawable);
                }
			} catch (IllegalStateException i) {
                //Do nothing, happens when we're no longer attached.
            }
		}
	}
	
	
	private class BookCaseAdapter extends KeyedResultAdapter {
				
		@Override
		public View getView(final int index, final LibraryBook object, View convertView,
				ViewGroup parent) {
			
			View result;
		
			if ( convertView == null ) {				
				LayoutInflater inflater = PlatformUtil.getLayoutInflater(getActivity());
				result = inflater.inflate(R.layout.bookcase_row, parent, false);
				
			} else {
				result = convertView;
			}			
			
			result.setTag(index);
			
			result.setOnClickListener( new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					LibraryFragment.this.onItemClick(null, null, index, 0);
				}
			});	
			
			result.setOnLongClickListener( new View.OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					return LibraryFragment.this.onItemLongClick(null, null, index, 0);
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
	
	private void buildImportQuestionDialog() {
		
		if ( importQuestion != null ) {
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.no_books_found);
		builder.setMessage( getString(R.string.scan_bks_question) );
		builder.setPositiveButton(android.R.string.yes, new android.content.DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();				
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

    private void setSupportProgressBarIndeterminateVisibility(boolean enable) {
        SherlockFragmentActivity activity = getSherlockActivity();
        if ( activity != null) {
            LOG.debug("Setting progress bar to " + enable );
            activity.setSupportProgressBarIndeterminateVisibility(enable);
        } else {
            LOG.debug("Got null activity.");
        }
    }
	
	private class MenuSelectionListener implements OnNavigationListener {	
		
		@Override
		public boolean onNavigationItemSelected(int pos, long arg1) {

			LibrarySelection newSelections = LibrarySelection.values()[pos];

            if ( newSelections != config.getLastLibraryQuery() ) {
			    config.setLastLibraryQuery(newSelections);
			
			    bookAdapter.clear();
                loadView(newSelections, "onNavigationItemSelected()");
            }

			return false;
		}
	}	

	private class AlphabetAdapter extends ArrayAdapter<Character> {
		
		private List<Character> data;
		
		private Character highlightChar;
		
		private int savedColor = -1;
		
		public AlphabetAdapter(Context context, int layout, int view, List<Character> input ) {
			super(context, layout, view, input);
			this.data = input;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			
			Character tag = data.get(position);
			view.setTag( tag );
			
			if ( tag.equals(highlightChar) ) {
				view.setBackgroundDrawable( getResources().getDrawable(R.drawable.list_activated_holo));
			} else {
				view.setBackgroundDrawable(null);
			}
			
			return view;
		}
		
		public void setHighlightChar(Character highlightChar) {
			this.highlightChar = highlightChar;
		}
		
		public Character getHighlightChar() {
			return highlightChar;
		}
	}

    private void loadQueryData(QueryResult<LibraryBook> result ) {
        if ( !isAdded() || getActivity() == null ) {
            return;
        }

        bookAdapter.setResult(result);

        if ( result instanceof KeyedQueryResult && result.getSize() >= ALPHABET_THRESHOLD ) {

            final KeyedQueryResult<LibraryBook> keyedResult = (KeyedQueryResult<LibraryBook>) result;

            alphabetAdapter = new AlphabetAdapter(getActivity(),
                    R.layout.alphabet_line, R.id.alphabetLabel,	keyedResult.getAlphabet() );

            alphabetBar.setAdapter(alphabetAdapter);

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
            alphabetAdapter = null;
            setAlphabetBarVisible(false);
        }


    }

	private class LoadBooksTask extends QueueableAsyncTask<String, Integer, QueryResult<LibraryBook>> {
		
		private Configuration.LibrarySelection sel;
        private String filter;

        public LoadBooksTask(LibrarySelection selection) {
            this.sel = selection;
        }

        public LoadBooksTask(LibrarySelection selection, String filter ) {
            this(selection);
            this.filter = filter;
        }
		
		@Override
		protected void onPreExecute() {

            if ( this.filter == null )  {
			    coverCache.clear();
            }
		}
		
		@Override
		protected QueryResult<LibraryBook> doInBackground(String... params) {
			
			Exception storedException = null;

            String query = this.filter;
			
			for ( int i=0; i < 3; i++ ) {

				try {

					switch ( sel ) {			
					case LAST_ADDED:
						return libraryService.findAllByLastAdded(query);
					case UNREAD:
						return libraryService.findUnread(query);
					case BY_TITLE:
						return libraryService.findAllByTitle(query);
					case BY_AUTHOR:
						return libraryService.findAllByAuthor(query);
					default:
						return libraryService.findAllByLastRead(query);
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
        protected void doOnPostExecute(QueryResult<LibraryBook> result) {
			 loadQueryData(result);

            if (filter == null && sel == Configuration.LibrarySelection.LAST_ADDED && result.getSize() == 0 && ! askedUserToImport ) {
                askedUserToImport = true;
                buildImportQuestionDialog();
                importQuestion.show();
            }
		}
		
	}
}
