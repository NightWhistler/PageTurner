package net.nightwhistler.pageturner.view;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.R.color;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BookCaseView extends View {
	
	private Drawable background;
	private Drawable shelf;
	
	private static final int WIDTH = 150;
	private static final int HEIGHT = 150;
	
	private QueryResult<LibraryBook> result;
	
	private static final Logger LOG = LoggerFactory.getLogger(BookCaseView.class);
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
	
	private LibraryBook selectedBook;
	
	public BookCaseView(Context context, AttributeSet attributes) {
		super(context, attributes);
		this.background = getResources().getDrawable(R.drawable.pine);
		this.shelf = getResources().getDrawable(R.drawable.shelf);
		
		this.setFocusableInTouchMode(true);
		this.setClickable(false);
		
		//setBackgroundDrawable( new BookCaseDrawable(9) );
        
	}	
	
	protected void onClick( int bookIndex ) {
		LibraryBook book = this.result.getItemAt(bookIndex);
		
		this.selectedBook = book;
		invalidate();
	}
	
	
	
	private void drawSelectedBookInfo( Canvas canvas ) {
		if ( this.selectedBook != null ) {
			LOG.debug("Drawing rect");
			
			Paint paint = new Paint();			
			paint.setARGB(200, 0, 0, 0);
			
			paint.setStyle(Style.FILL);
			paint.setAntiAlias(true);
					
			int padding = 20;
		
			canvas.drawRect(padding, padding, getWidth() - padding, getHeight() -  padding, paint);
			
			LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			View layout  = inflater.inflate(R.layout.book_details, null)
				.findViewById(R.id.bookInfoContainer);
			
			LOG.debug("Setting with to " + (getWidth() - 60) );
			layout.setLayoutParams(new ViewGroup.LayoutParams(getWidth() - 60, getHeight() - 60 ));
			fillBookData(layout); 
			
			layout.measure(getWidth(), getHeight());
			canvas.translate(30, 30);
			layout.layout(0, 0, getWidth(), getHeight() );
			
			LOG.debug(  "Width of view is now: " + layout.getWidth() );
			
			layout.draw(canvas);
			
		} else {
			LOG.debug("Not drawing rect, since selected book is null");
		}
	}
	
	private void fillBookData(View layout) {
		
		ImageView coverView = (ImageView) layout.findViewById(R.id.coverImage);
		
		if ( this.selectedBook.getCoverImage() != null ) {			
			coverView.setImageBitmap( selectedBook.getCoverImage() );
		} else {			
			coverView.setImageDrawable( getResources().getDrawable(R.drawable.river_diary));
		}				
		
		TextView titleView = (TextView) layout.findViewById(R.id.titleField);
		titleView.setText(this.selectedBook.getTitle());		
		
		TextView authorView = (TextView) layout.findViewById(R.id.authorField);
		authorView.setText( "by " + this.selectedBook.getAuthor().getFirstName() + " " + this.selectedBook.getAuthor().getLastName() );
		
		if (this.selectedBook.getLastRead() != null && ! this.selectedBook.getLastRead().equals(new Date(0))) {
			TextView lastRead = (TextView) layout.findViewById(R.id.lastRead);
			lastRead.setText("Last read: " + DATE_FORMAT.format(this.selectedBook.getLastRead()) );
		}
		
		TextView added = (TextView) layout.findViewById(R.id.addedToLibrary);
		added.setText( "Added to library: " + DATE_FORMAT.format(this.selectedBook.getAddedToLibrary()) );
		
		TextView descriptionView = (TextView) layout.findViewById(R.id.bookDescription);
		descriptionView.setText(this.selectedBook.getDescription());
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
	
	
	
	public void fireClick( float x, float y ) {
		//onClick(findIndexAt(x, y));
	}
	
	
	
	
	
	
}
