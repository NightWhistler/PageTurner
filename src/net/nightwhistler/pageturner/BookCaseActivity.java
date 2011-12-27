package net.nightwhistler.pageturner;

import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.QueryResultAdapter;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import net.nightwhistler.pageturner.view.BookCaseDrawable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.ListActivity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

public class BookCaseActivity extends Activity {
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCaseActivity.class);
	
	private GridView bookCaseView;
	
	private GestureDetector gestureDetector;
	private View.OnTouchListener touchListener;
	
	private LibraryService libraryService;
	
	private QueryResult<LibraryBook> result;	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
				
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookcase);
		this.bookCaseView = (GridView) findViewById(R.id.bookCaseGrid);
		bookCaseView.setOnTouchListener(touchListener);
		
		//Drawable background = getBookCaseDrawable();
		//bookCaseView.setBackgroundDrawable(background);
						
		bookCaseView.setNumColumns(3);
		BookGridAdapter adapter = new BookGridAdapter();
		bookCaseView.setAdapter( adapter );
		
		this.libraryService = new SqlLiteLibraryService(this);
		adapter.setResult( this.libraryService.findAllByTitle() );
	}
	
	private BookCaseDrawable getBookCaseDrawable() {
		BookCaseDrawable drawable  = new BookCaseDrawable(result);
		drawable.setBackground( getResources().getDrawable(R.drawable.pine) );
		drawable.setShelf( getResources().getDrawable(R.drawable.shelf) );
		drawable.setFallBackCover( getResources().getDrawable(R.drawable.river_diary) );

		//drawable.setBounds( 0, 0, bookCaseView.getWidth(), bookCaseView.getHeight() );
		
		return drawable;
	}	
	
	private class BookGridAdapter extends QueryResultAdapter<LibraryBook> {
		@Override
		public View getView(int index, LibraryBook object, View convertView,
				ViewGroup parent) {
			
			ImageView imageView;
			
			if ( convertView != null ) {
				imageView = (ImageView) convertView;
			} else {
				imageView = new ImageView(BookCaseActivity.this);
			}			
			
			Drawable drawable;
			if ( object.getCoverImage() != null ) {
				drawable = new BitmapDrawable(object.getCoverImage());
			} else {
				drawable = getResources().getDrawable(R.drawable.river_diary);
			}
			
			drawable.setBounds( 0, 0, 150, 150 );
			imageView.setImageDrawable(drawable);
			
			//imageView.setBackgroundResource(R.drawable.pine);
			
			return imageView;
		}
	}
	
	/*
	private class ClickListener extends VerifiedFlingListener {
		
		public ClickListener(Context context) {
			super(context);
		}
		
		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			
			//LOG.debug("Single tap confirmed.");
			//bookCaseView.fireClick(e.getX(), e.getY() );
			return true;
		}
		
		@Override
		public boolean onVerifiedFling(MotionEvent e1, MotionEvent e2,
				float velocityX, float velocityY) {
			
			int displayed = bookCaseView.getDisplayedChild();
			int otherIndex = Math.abs( displayed - 1 );
			
			if ( velocityX < 0 ) {	
				bookCaseView.setInAnimation( Animations.inFromRightAnimation() );
				bookCaseView.setOutAnimation(Animations.outToLeftAnimation() );				
				
				int newOffset = bookCaseViews.get(displayed).getStartOffset()
					+ bookCaseViews.get(displayed).getAmountOfBooks();
				
				if ( newOffset >= ( result.getSize() -1) ) {
					newOffset = 0;
				}
				 
				bookCaseViews.get(otherIndex).setStartOffset(newOffset);
				
				bookCaseView.showNext();
				return true;
			} else {
				bookCaseView.setInAnimation(Animations.inFromLeftAnimation());
				bookCaseView.setOutAnimation(Animations.outToRightAnimation() );
				
				int newOffset = bookCaseViews.get(displayed).getStartOffset()
					- bookCaseViews.get(displayed).getCapacity();
				
				if ( newOffset < 0 ) {
					newOffset = (result.getSize() -1) - newOffset;
				}
				
				bookCaseViews.get(otherIndex).setStartOffset(newOffset);
				
				bookCaseView.showPrevious();
				return true;
			}			
						
		}
		
	}
	*/
	
}
