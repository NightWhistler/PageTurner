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

import roboguice.RoboGuice;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.R;
import android.os.Bundle;

import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;

public class LibraryActivity extends RoboSherlockFragmentActivity {
	private LibraryFragment libraryFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme( RoboGuice.getInjector(this).getInstance(Configuration.class).getTheme() );
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_library);
		libraryFragment = (LibraryFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_library);
	}

<<<<<<< HEAD
				Intent intent = new Intent(LibraryActivity.this, ReadingActivity.class);
				
				intent.setData( Uri.parse(libraryBook.getFileName()));
				setResult(RESULT_OK, intent);
						
				startActivityIfNeeded(intent, 99);				
			}
		});
		
		builder.show();
	}	
	
	private void startImport(File startFolder, boolean copy) {		
		ImportTask importTask = new ImportTask(this, libraryService, this, config, copy);
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
		
		MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.library_menu, menu);        
       		
		OnMenuItemClickListener toggleListener = new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				
				if ( switcher.getDisplayedChild() == 0 ) {
					bookAdapter = new BookCaseAdapter();
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
				Intent intent = new Intent(LibraryActivity.this, CatalogActivity.class);    					
    			startActivityIfNeeded(intent, 99);
    		
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
		
		LayoutInflater inflater = PlatformUtil.getLayoutInflater(this);
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
		
		builder = new AlertDialog.Builder(this);
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
	
=======
>>>>>>> master
	@Override
	public void onBackPressed() {
		libraryFragment.onBackPressed();
	}
}
