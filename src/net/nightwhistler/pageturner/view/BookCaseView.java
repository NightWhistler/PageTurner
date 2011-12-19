package net.nightwhistler.pageturner.view;

import net.nightwhistler.pageturner.R;
import net.nightwhistler.pageturner.library.LibraryBook;
import net.nightwhistler.pageturner.library.LibraryService;
import net.nightwhistler.pageturner.library.QueryResult;
import net.nightwhistler.pageturner.library.SqlLiteLibraryService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class BookCaseView extends View {
	
	private Drawable background;
	private Drawable shelf;
	
	private LibraryService libraryService;
	
	private static final int WIDTH = 150;
	private static final int HEIGHT = 150;

	public BookCaseView(Context context, AttributeSet attributes) {
		super(context, attributes);
		this.background = getResources().getDrawable(R.drawable.pine);
		this.shelf = getResources().getDrawable(R.drawable.shelf);
		
		this.libraryService = new SqlLiteLibraryService(context);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {		
		super.onDraw(canvas);
		drawBackGround(canvas);
		drawShelves(canvas);
		drawBooks(canvas);
	}
	
	private void drawBooks(Canvas canvas) {
		QueryResult<LibraryBook> books = 
			this.libraryService.findAllByTitle();
		
		int booksH = (getWidth() / WIDTH);
		int combinedHeight = HEIGHT + this.shelf.getIntrinsicHeight();
		
		int rows = getHeight() / combinedHeight;
		
		int count = 0;		
		int baseOffset = 15;

		for ( int j=0; j < rows; j++ ) {
			int top = (j * combinedHeight) + (shelf.getIntrinsicHeight() / 2);
			for ( int i=0; i < booksH; i++ ) {
				int left = baseOffset + (i * WIDTH);


				if ( count < books.getSize() ) {

					LibraryBook book = books.getItemAt(count);
					if ( book.getCoverImage() != null ) {
						Bitmap bitmap = BitmapFactory.decodeByteArray(book.getCoverImage(),
								0, book.getCoverImage().length );

						double ratio = (double) bitmap.getWidth() / (double) bitmap.getHeight();

						Drawable bitMapDrawable = new BitmapDrawable(bitmap);
						bitMapDrawable.setBounds(left, top, left + (int) (WIDTH * ratio), top + HEIGHT );	

						bitMapDrawable.draw(canvas);
					}

					count++;
				}
			}
		}
		
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
