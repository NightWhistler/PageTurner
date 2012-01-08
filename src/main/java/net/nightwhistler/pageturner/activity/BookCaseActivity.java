package net.nightwhistler.pageturner.activity;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResultAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;

import com.google.inject.Inject;

public class BookCaseActivity extends RoboActivity {
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCaseActivity.class);
	
	@InjectView(R.id.bookCaseGrid)
	private GridView bookCaseView;
	
	//private GestureDetector gestureDetector;
	private View.OnTouchListener touchListener;
	
	@Inject
	private LibraryService libraryService;
	
	//private QueryResult<LibraryBook> result;	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
				
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookcase);
		
		this.bookCaseView.setOnTouchListener(touchListener);
		
		//Drawable background = getBookCaseDrawable();
		//bookCaseView.setBackgroundDrawable(background);
						
		//bookCaseView.setNumColumns(3);
		
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth();

		
		//int columns = (width / 150) + 1;
		//bookCaseView.setNumColumns(columns);
		//bookCaseView.setPadding(0, 0, 0, 0);
		//bookCaseView.setStretchMode(GridView.NO_STRETCH);
		
		BookGridAdapter adapter = new BookGridAdapter();
		bookCaseView.setAdapter( adapter );		
		
		adapter.setResult( this.libraryService.findAllByTitle() );
	}
	
	
	
	/*
	private BookCaseDrawable getBookCaseDrawable() {
		BookCaseDrawable drawable  = new BookCaseDrawable(result);
		drawable.setBackground( getResources().getDrawable(R.drawable.pine) );
		drawable.setShelf( getResources().getDrawable(R.drawable.shelf) );
		drawable.setFallBackCover( getResources().getDrawable(R.drawable.river_diary) );

		//drawable.setBounds( 0, 0, bookCaseView.getWidth(), bookCaseView.getHeight() );
		
		return drawable;
	}
	*/	
	
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
				drawable = new BitmapDrawable( Bitmap.createScaledBitmap(object.getCoverImage(), 150, 200, false));
				//drawable = getResources().getDrawable(R.drawable.river_diary);
			} else {
				imageView.setImageBitmap(null);
				return imageView;
				//drawable = getResources().getDrawable(R.drawable.river_diary);
			}
			
			
			float ratio = (float) drawable.getIntrinsicHeight() / (float) drawable.getIntrinsicWidth();
			
			int width = 150;
			int height = (int) (width * ratio);
			
			drawable.setBounds( 0, 0, width, height );
			imageView.setImageDrawable(drawable);
			
			
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
