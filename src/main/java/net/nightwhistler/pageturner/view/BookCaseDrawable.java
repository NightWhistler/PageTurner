package net.nightwhistler.pageturner.view;

import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.QueryResult;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class BookCaseDrawable extends Drawable {
	
	private QueryResult<LibraryBook> books;
	private int startOffset;
	
	private Drawable background;
	private Drawable shelf;
	private Drawable fallBackCover;
	
	private static final int WIDTH = 150;
	private static final int HEIGHT = 150;
	
	private boolean drawBooks = true;
	

	public void setBooks(QueryResult<LibraryBook> books) {
		this.books = books;
	}

	@Override
	public void draw(Canvas canvas) {
		drawBackGround(canvas);
		drawShelves(canvas);
		if ( this.drawBooks ) {
			drawBooks(canvas, startOffset);
		}
	}
	
	@Override
	public int getOpacity() {
		return PixelFormat.OPAQUE;
	}
	
	@Override
	public void setAlpha(int alpha) {
		// TODO Auto-generated method stub
		
	}
	
	public void setDrawBooks(boolean drawBooks) {
		this.drawBooks = drawBooks;
	}
	
	@Override
	public void setColorFilter(ColorFilter cf) {
		// TODO Auto-generated method stub
		
	}	
	
	public QueryResult<LibraryBook> getBooks() {
		return books;
	}
	
	public int getWidth() {
		return this.getBounds().width();
	}
	
	public int getHeight() {
		return this.getBounds().height();
	}
	
	public void setBackground(Drawable background) {
		this.background = background;
	}
	
	public void setShelf(Drawable shelf) {
		this.shelf = shelf;
	}
	
	public void setFallBackCover(Drawable fallBackCover) {
		this.fallBackCover = fallBackCover;
	}
	
	private int getBooksPerRow() {
		return (getWidth() / WIDTH);
	}
	
	private int getRows() {
		return getHeight() / getRowHeight();
	}
	
	public int getRowHeight() {
		return HEIGHT + this.shelf.getIntrinsicHeight();
	}
	
	public int getStartOffset() {
		return startOffset;
	}

	/**
	 * Returns how many books this Drawable can display.
	 * 
	 * @return
	 */
	public int getCapacity() {
		return getBooksPerRow() * getRows();
	}
	
	/**
	 * Returns how many books are actually display right now.
	 * 
	 * @return
	 */
	public int getAmountOfBooks() {
		
		if ( books == null ) {
			return 0;
		}
		
		int capacity = getCapacity();
		
		int leftInSet = (books.getSize() -1) - this.startOffset;
		
		return Math.min(capacity, leftInSet);		
	}
	
	private void drawBooks(Canvas canvas, int startOffset) {
		
		if ( this.books == null || this.books.getSize() == 0 ) {
			return;
		}
		
		int numberOfSlots = getBooksPerRow();
		int slotWidth = getWidth() / numberOfSlots;
		
		int combinedHeight = getRowHeight();		
		int rows = getRows();
		
		int count = startOffset;			

		for ( int j=0; j < rows; j++ ) {
			int top = (j * combinedHeight) + (shelf.getIntrinsicHeight() / 2);
			for ( int i=0; i < numberOfSlots; i++ ) {				
				
				if ( count < this.books.getSize() ) {
					
					LibraryBook book = books.getItemAt(count);
										
					Drawable bitMapDrawable = getCover(book);
					double ratio = (double) bitMapDrawable.getIntrinsicHeight() / (double) bitMapDrawable.getIntrinsicWidth();						
						
					int leftOfSlot = i * slotWidth;
					int widthOfBook = WIDTH;
					int heightOfBook = (int) (widthOfBook * ratio);
					
					if ( heightOfBook > HEIGHT ) {
						heightOfBook = HEIGHT;
						widthOfBook = (int) ( heightOfBook * (1/ratio));
					}
					
					int leftOfBook = leftOfSlot + ( slotWidth / 2 ) - ( widthOfBook / 2 );
						
					bitMapDrawable.setBounds(leftOfBook, top, leftOfBook + widthOfBook, top + heightOfBook );
					bitMapDrawable.draw(canvas);					

					count++;
				}
			}
		}
		
	}
	
	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
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
	
	private Drawable getCover( LibraryBook book ) {
		if ( book == null || book.getCoverImage() == null ) {
			return fallBackCover;
		}		
		
		return new BitmapDrawable(book.getCoverImage());
	}
	
	private int findIndexAt( float x, float y ) {
		
		int column = (int) x / WIDTH;
		int row = (int) y / getRowHeight();
		
		int index = row * getBooksPerRow() + column;
		
		return index;
	}
	
	public LibraryBook findBookAtLocation( float x, float y ) {
		
		if ( books == null ) {
			return null;
		}
		
		int index = findIndexAt(x, y);
		return this.books.getItemAt(index);
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
	
}
