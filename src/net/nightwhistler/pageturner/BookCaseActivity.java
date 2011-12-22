package net.nightwhistler.pageturner;

import java.util.ArrayList;
import java.util.List;

import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import net.nightwhistler.pageturner.view.BookCaseDrawable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ViewFlipper;
import android.widget.ViewSwitcher;

import com.globalmentor.android.widget.VerifiedFlingListener;

public class BookCaseActivity extends Activity {
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCaseActivity.class);
	
	private ViewSwitcher bookCaseView;
	
	private GestureDetector gestureDetector;
	private View.OnTouchListener touchListener;
	
	private LibraryService libraryService;
	
	private QueryResult<LibraryBook> result;
	
	private List<BookCaseDrawable> bookCaseViews = new ArrayList<BookCaseDrawable>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.bookcase);
		this.bookCaseView = (ViewSwitcher) findViewById(R.id.bookCaseViewFlipper);
		bookCaseView.setOnTouchListener(touchListener);
		
		this.gestureDetector = new GestureDetector(this, 
				new ClickListener(this));
		this.touchListener = new View.OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);				
			}
		};			
		
		this.libraryService = new SqlLiteLibraryService(this);		
		
		result = this.libraryService.findAllByTitle();
		
		for ( int i=0; i < this.bookCaseView.getChildCount(); i++ ) {
			
			ImageView imageView = (ImageView) this.bookCaseView.getChildAt(i);			
			BookCaseDrawable drawable = getBookCaseDrawable();
			imageView.setImageDrawable( drawable );			

			imageView.setOnTouchListener(touchListener);
						
			this.bookCaseViews.add( drawable );
		}
		
	}
	
	private BookCaseDrawable getBookCaseDrawable() {
		BookCaseDrawable drawable  = new BookCaseDrawable(result);
		drawable.setBackground( getResources().getDrawable(R.drawable.pine) );
		drawable.setShelf( getResources().getDrawable(R.drawable.shelf) );
		drawable.setFallBackCover( getResources().getDrawable(R.drawable.river_diary) );

		//drawable.setBounds( 0, 0, bookCaseView.getWidth(), bookCaseView.getHeight() );
		
		return drawable;
	} 

	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return this.gestureDetector.onTouchEvent(event);
	}
	
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
	
}
