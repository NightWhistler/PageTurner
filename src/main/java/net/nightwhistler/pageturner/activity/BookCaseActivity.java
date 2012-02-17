package net.nightwhistler.pageturner.activity;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.QueryResultAdapter;
import net.nightwhistler.pageturner.view.BookCaseView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.inject.Inject;

public class BookCaseActivity extends RoboActivity {
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCaseActivity.class);
	
	@InjectView(R.id.bookCaseView)
	private BookCaseView bookCaseView;	
	
	@InjectResource(R.drawable.river_diary)
	private Drawable fallBackCover;
		
	@Inject
	private LibraryService libraryService;
	
	private LibraryBook selectedBook;
	
	private ProgressDialog waitDialog;
	
	private BookViewAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
				
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookcase);		
        
        this.waitDialog = new ProgressDialog(this);
        waitDialog.setOwnerActivity(this);
        
        this.adapter = new BookViewAdapter(this);
        bookCaseView.setAdapter(adapter);
        
        adapter.setResult(libraryService.findAllByTitle());   
        
        bookCaseView.setFocusable(true);
        
    		        
    	new LoadBooksTask().execute(3);		
	}	
	
	@Override
	public void onBackPressed() {
		finish();
	}
	
	public void onBookClicked( LibraryBook book ) {
		Intent intent = new Intent(this, ReadingActivity.class);
		
		intent.setData( Uri.parse(book.getFileName()));
		this.setResult(RESULT_OK, intent);
				
		startActivityIfNeeded(intent, 99);
	}
	
	public boolean onBookLongClicked( LibraryBook book ) {
		return false;
	}	
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {		

		if ( this.selectedBook != null ) {
			MenuItem detailsItem = menu.add( "View details");

			final String fileName = this.selectedBook.getFileName();

			detailsItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					Intent intent = new Intent( BookCaseActivity.this, BookDetailsActivity.class );
					intent.putExtra("book", fileName );				
					startActivity(intent);					
					return true;
				}
			});

			MenuItem deleteItem = menu.add("Delete from library");

			deleteItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

				@Override
				public boolean onMenuItemClick(MenuItem item) {
					libraryService.deleteBook( fileName );
					new LoadBooksTask().execute(3);
					return true;					
				}
			});

			this.selectedBook = null;
		}

	}	
	
	private class BookViewAdapter extends QueryResultAdapter<LibraryBook> {
		
		private Context context;
		
		public BookViewAdapter(Context context) {
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
			
			result.setOnClickListener( new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					onBookClicked(object);					
				}
			});
			
			result.setOnLongClickListener(new OnLongClickListener() {
				
				@Override
				public boolean onLongClick(View v) {
					return onBookLongClicked(object);
				}
			});
			
			ImageView image = (ImageView) result.findViewById(R.id.bookCover);
			
			
			Bitmap bitmap = object.getCoverImage();
			
			if ( bitmap != null ) {			
				image.setImageBitmap( object.getCoverImage() );
			} else {
				image.setImageDrawable(fallBackCover);
			}		
			
			return result;
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
			adapter.setResult(result);
			
			waitDialog.hide();			
		}
		
	}	
	
}
