package net.nightwhistler.pageturner;

import java.text.DateFormat;
import java.util.Locale;

import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.QueryResultAdapter;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class LibraryActivity extends ListActivity implements OnItemSelectedListener {
	
	private LibraryService libraryService;
	
	private BookAdapter bookAdapter;
	
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
	
	ProgressDialog waitDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		this.libraryService = new SqlLiteLibraryService(this);
		
		this.bookAdapter = new BookAdapter(this);		
		setListAdapter(bookAdapter);		
		
		this.waitDialog = new ProgressDialog(this);
		this.waitDialog.setOwnerActivity(this);
		
		this.waitDialog.setTitle("Loading library...");
		this.waitDialog.show();
		
		new LoadBooksTask().execute();
	}
	
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		
		//parent.getItemAtPosition(pos).toString()
		
	}
	
	@Override
	protected void onStop() {		
		this.libraryService.close();	
		this.waitDialog.dismiss();
	}
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
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
		
		LibraryBook book = this.bookAdapter.getResultAt(position);
		
		Intent intent = this.getIntent();
		
		intent.setData( Uri.parse(book.getFileName()));
		this.setResult(RESULT_OK, intent);
				
		finish();

	}
	
	private class LoadBooksTask extends AsyncTask<Void, Integer, QueryResult<LibraryBook>> {		
		
		@Override
		protected QueryResult<LibraryBook> doInBackground(Void... params) {
			return libraryService.findAllByLastRead();
		}
		
		@Override
		protected void onPostExecute(QueryResult<LibraryBook> result) {
			bookAdapter.setResult(result);
			waitDialog.hide();
		}
		
	}
	
	
	
}
