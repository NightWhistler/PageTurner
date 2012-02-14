package net.nightwhistler.pageturner.view;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.QueryResult;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.GridView;

public class BookCaseView extends GridView {
	
	private Bitmap background;
		
	private int mShelfWidth;
	private int mShelfHeight;	
	
	private QueryResult<LibraryBook> result;	
	
	private LibraryBook selectedBook;	
	
	public BookCaseView(Context context, AttributeSet attributes) {
		super(context, attributes);
				
		this.setFocusableInTouchMode(true);
		this.setClickable(false);
		
		final Bitmap shelfBackground = BitmapFactory.decodeResource(context.getResources(),
				R.drawable.shelf_single);
		setBackground(shelfBackground);
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
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		final int count = getChildCount();
        final int top = count > 0 ? getChildAt(0).getTop() : 0;
        final int shelfWidth = mShelfWidth;
        final int shelfHeight = mShelfHeight;
        final int width = getWidth();
        final int height = getHeight();
        final Bitmap background = this.background;

        for (int x = 0; x < width; x += shelfWidth) {
            for (int y = top; y < height; y += shelfHeight) {
                canvas.drawBitmap(background, x, y, null);
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
