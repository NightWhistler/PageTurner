package net.nightwhistler.pageturner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.QueryResultAdapter;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.epub.EpubReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.LauncherActivity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class LibraryActivity extends ListActivity implements OnItemSelectedListener {
	
	private LibraryService libraryService;
	
	private BookAdapter bookAdapter;
	
	private ArrayAdapter<String> menuAdapter;
	
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
	
	ProgressDialog waitDialog;
	ProgressDialog importDialog;
	
	private static final String[] MENU_ITEMS = {
		"Most recently read books", "Most recently added books", "Books by title", "Books by author" 
	};	
	
	private static final Logger LOG = LoggerFactory.getLogger(LibraryActivity.class); 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		this.libraryService = new SqlLiteLibraryService(this);
		
		this.bookAdapter = new BookAdapter(this);
		this.menuAdapter = new ArrayAdapter<String>(this, R.layout.menu_row, 
				R.id.bookTitle, MENU_ITEMS);
				
		//setListAdapter(bookAdapter);
		setListAdapter(menuAdapter);
				
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		
		this.importDialog = new ProgressDialog(this);
		this.importDialog.setOwnerActivity(this);
		
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		
		//parent.getItemAtPosition(pos).toString()
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Scan for books");
		
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
		
		try {
			startActivityForResult(intent, 1);
		} catch (ActivityNotFoundException e) {
			// No compatible file manager was found.
			Toast.makeText(this, "Please install OI File Manager from the Android Market.", 
					Toast.LENGTH_SHORT).show();
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
		super.onStop();
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}	
	
	private void findEpubsInFolder( File folder, List<File> items) {
		
		if ( folder == null ) {
			return;
		}
		
		if ( folder.isDirectory() && folder.listFiles() != null) {
			for (File child: folder.listFiles() ) {
				findEpubsInFolder(child, items); 
			}
		} else {
			if ( folder.getName().endsWith(".epub") ) {
				items.add(folder);
			}
		}
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
				
		if ( getListAdapter() == this.bookAdapter
				&& event.getKeyCode() == KeyEvent.KEYCODE_BACK 
				&& event.getAction() == KeyEvent.ACTION_DOWN ) {
			
			setListAdapter(this.menuAdapter);			
			return true;
		}
		
		return super.dispatchKeyEvent(event);
		
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
			
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View rowView = inflater.inflate(R.layout.book_row, parent, false);
			
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
				
			startActivity(intent);
		}

	}
	
	private void handleMenuClick(int position) {
		this.bookAdapter.clear();
		this.setListAdapter(bookAdapter);
		new LoadBooksTask().execute(position);
	}
	
	private class ImportBooksTask extends AsyncTask<File, Integer, Void> {	
		
		private boolean hadError = false;
			
		@Override
		protected void onPreExecute() {
			importDialog.setTitle("Importing books...");
			importDialog.setMessage("Scanning for EPUB files.");
			importDialog.show();
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
						importBook( book);
					}					
				} catch (OutOfMemoryError oom ) {
					hadError = true;
					return null;
				}
				
				i++;
				publishProgress(i, total);
			}
			
			return null;
		}
		
		private void importBook(File file) {
			try {
				// read epub file
		        EpubReader epubReader = new EpubReader();
		        
				InputStream input = new FileInputStream(file);
				Book importedBook = epubReader.readEpub(input);
				input.close();
				
				Metadata metaData = importedBook.getMetadata();
	        	
	        	String authorFirstName = "Unknown author";
	        	String authorLastName = "";
	        	
	        	if ( metaData.getAuthors().size() > 0 ) {
	        		authorFirstName = metaData.getAuthors().get(0).getFirstname();
	        		authorLastName = metaData.getAuthors().get(0).getLastname();
	        	}
	        	
	        	byte[] cover = importedBook.getCoverImage() != null ? importedBook.getCoverImage().getData() : null;
	        	
	        	libraryService.storeBook(file.getAbsolutePath(), authorFirstName, authorLastName, 
	        			importedBook.getTitle(), cover );		        	
	        	
	        	cover = null;	        		        	
				
			} catch (IOException io ) {}
		}
		
		@Override
		protected void onProgressUpdate(Integer... values) {
			importDialog.setMessage("Importing " + values[0] + " / " + values[1]);			
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
				return libraryService.findAllByTitle();
			case 3:
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
