package net.nightwhistler.pageturner.view;

import roboguice.RoboGuice;
import net.nightwhistler.pageturner.Configuration;
import net.nightwhistler.pageturner.Configuration.ColourProfile;
import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.QueryResult;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.GridView;

import com.google.inject.Inject;

public class BookCaseView extends GridView {
	
	private Bitmap background;
		
	private int mShelfWidth;
	private int mShelfHeight;	
	
	private QueryResult<LibraryBook> result;	
	
	private LibraryBook selectedBook;	
	
	private Configuration config;
	
	public BookCaseView(Context context, AttributeSet attributes) {
		super(context, attributes);
				
		this.setFocusableInTouchMode(true);
		this.setClickable(false);
		
		final Bitmap shelfBackground;
		
		this.config = RoboGuice.getInjector(context).getInstance(Configuration.class);
		
		if(!Configuration.IS_EINK_DEVICE) {
			if (config.getColourProfile() == ColourProfile.DAY ) {
				shelfBackground = BitmapFactory.decodeResource(context.getResources(),
									R.drawable.shelf_single);
			} else {
				shelfBackground = BitmapFactory.decodeResource(context.getResources(),
									R.drawable.shelf_single_dark);
			}
			setBackground(shelfBackground);
		}
		this.setFocusable(true);
	}
	
	public void setBackground(Bitmap background) {
		this.background = background;
		
		mShelfWidth = background.getWidth();
        mShelfHeight = background.getHeight();
	}	
	
	protected void onClick( int bookIndex ) {
		LibraryBook book = this.result.getItemAt(bookIndex);
		
		this.selectedBook = book;
		invalidate();
	}
	
	public void scrollToChild( int index ) {
		System.out.println("Scrolling to child " + index );
		
		int y = getChildAt(index).getTop();
		
		int delta = y - getScrollY();
		
		scrollBy(0, delta);
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		final int count = getChildCount();
        final int top = count > 0 ? getChildAt(0).getTop() : 0;
        final int shelfWidth = mShelfWidth;
        final int shelfHeight = mShelfHeight;
        final int width = getWidth();
        final int height = getHeight();
        final Bitmap background = this.background;

	if(background != null) {
		for (int x = 0; x < width; x += shelfWidth) {
			for (int y = top; y < height; y += shelfHeight) {
				canvas.drawBitmap(background, x, y, null);
			}
			
			//This draws the top pixels of the shelf above the current one
			
			Rect source = new Rect(0, mShelfHeight - top, mShelfWidth, mShelfHeight);
			Rect dest = new Rect(x, 0, x + mShelfWidth, top );
			
			canvas.drawBitmap(background, source, dest, null);
		}
	}        

        super.dispatchDraw(canvas);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ( keyCode == KeyEvent.KEYCODE_BACK && this.selectedBook != null ) {
			this.selectedBook = null;
			invalidate();
			return true;
		}
		
		return false;
	}	
	
}
