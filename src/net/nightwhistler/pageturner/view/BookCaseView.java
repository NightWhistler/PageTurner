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
	
	private LibraryService libraryService;
	
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
		
		this.libraryService = new SqlLiteLibraryService(context);		
		
		this.result = this.libraryService.findAllByTitle();
		
		this.setFocusableInTouchMode(true);
		this.setClickable(false);
		
		setBackgroundDrawable( new BookCaseDrawable(9) );
        
	}	
	
	protected void onClick( int bookIndex ) {
		LibraryBook book = this.result.getItemAt(bookIndex);
		
		this.selectedBook = book;
		invalidate();
	}
	
	private int getBooksPerRow() {
		return (getWidth() / WIDTH);
	}
	
	private int getRows() {
		return getHeight() / getRowHeight();
	}
	
	private int getRowHeight() {
		return HEIGHT + this.shelf.getIntrinsicHeight();
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
			coverView.setImageBitmap( BitmapFactory.decodeByteArray(this.selectedBook.getCoverImage(),
					0, this.selectedBook.getCoverImage().length));
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
	
	private Drawable getCover( LibraryBook book ) {
		if ( book == null || book.getCoverImage() == null ) {
			return getContext().getResources().getDrawable(R.drawable.river_diary);
		}
		
		Bitmap bitmap = BitmapFactory.decodeByteArray(book.getCoverImage(), 0, book.getCoverImage().length );
		return new BitmapDrawable(bitmap);
	}
	
	
	private class BookCaseDrawable extends Drawable {
		
		int startOffset;
		
		public BookCaseDrawable(int offset) {
			this.startOffset = offset;
		}
		
		@Override
		public void draw(Canvas canvas) {
			drawBackGround(canvas);
			drawShelves(canvas);
			drawBooks(canvas, startOffset);
			
			drawSelectedBookInfo(canvas);
		}
		
		@Override
		public int getOpacity() {			
			return PixelFormat.OPAQUE;
		}
		
		@Override
		public void setAlpha(int alpha) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void setColorFilter(ColorFilter cf) {
			// TODO Auto-generated method stub
			
		}
	
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
	
	private void drawBooks(Canvas canvas, int startOffset) {
				
		int numberOfSlots = getBooksPerRow();
		int slotWidth = getWidth() / numberOfSlots;
		
		int combinedHeight = getRowHeight();		
		int rows = getRows();
		
		int count = startOffset;			

		for ( int j=0; j < rows; j++ ) {
			int top = (j * combinedHeight) + (shelf.getIntrinsicHeight() / 2);
			for ( int i=0; i < numberOfSlots; i++ ) {				
				
				if ( count < this.result.getSize() ) {
					
					LibraryBook book = result.getItemAt(count);
										
					Drawable bitMapDrawable = getCover(book);
					double ratio = (double) bitMapDrawable.getIntrinsicWidth() / (double) bitMapDrawable.getIntrinsicHeight();						
						
					int leftOfSlot = i * slotWidth;
					int widthOfBook = (int) (WIDTH * ratio);
					int leftOfBook = leftOfSlot + ( slotWidth / 2 ) - ( widthOfBook / 2 );
						
					bitMapDrawable.setBounds(leftOfBook, top, leftOfBook + widthOfBook, top + HEIGHT );
					bitMapDrawable.draw(canvas);					

					count++;
				}
			}
		}
		
	}
	
	public void fireClick( float x, float y ) {
		onClick(findIndexAt(x, y));
	}
	
	
	private int findIndexAt( float x, float y ) {
		
		int column = (int) x / WIDTH;
		int row = (int) y / getRowHeight();
		
		int index = row * getBooksPerRow() + column;
		
		return index;
	}
	
	private void drawShelves(Canvas canvas) {
		int shelfHeight = this.shelf.getIntrinsicHeight();
		
		int combinedHeight = HEIGHT + shelfHeight;
		
		int rows = getHeight() / combinedHeight;
		int cols = ( getWidth() / shelf.getIntrinsicWidth() ) + 1;
		
		for ( int i=0; i < cols; i++ ) {
			int left = i * shelf.getIntrinsicWidth();
			
			for ( int j=0; j < rows; j++ ) {
				int top = j * combinedHeight;
				
				shelf.setBounds(left, top + HEIGHT, 
						left + shelf.getIntrinsicWidth(),
						top + combinedHeight);
				
				shelf.draw(canvas);
			}
		}		
	}
	
	private void drawBackGround(Canvas canvas ) {
		int rows = ( getHeight() / background.getIntrinsicHeight() ) + 1;
		int cols = ( getWidth() / background.getIntrinsicWidth() ) + 1;
		
		for ( int i=0; i < cols; i++ ) {
			int left = i * background.getIntrinsicWidth();
			
			for ( int j=0; j < rows; j++ ) {
				int top = j * background.getIntrinsicHeight();
				
				background.setBounds(left, top, 
						left + background.getIntrinsicWidth(),
						top + background.getIntrinsicHeight());
				
				background.draw(canvas);				
			}
		}
	}	
}
