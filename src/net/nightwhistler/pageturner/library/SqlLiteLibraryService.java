package net.nightwhistler.pageturner.library;

import java.io.ByteArrayOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class SqlLiteLibraryService implements LibraryService {
	
	private static final int THUMBNAIL_HEIGHT = 150;
	
	LibraryDatabaseHelper helper;
	
	public SqlLiteLibraryService(Context context) {
		this.helper = new LibraryDatabaseHelper(context);
	}

	@Override
	public void storeBook(String fileName, String authorFirstName,
			String authorLastName, String title, byte[] coverImage) {
		
		byte[] thumbNail = resizeImage(coverImage);
		
		this.helper.store(fileName, authorFirstName, 
				authorLastName, title, thumbNail);
	}
	
	@Override
	public QueryResult<LibraryBook> findAllByLastRead() {		
		return helper.findAllOrderedBy(
				LibraryDatabaseHelper.Field.date_last_read,
				LibraryDatabaseHelper.Order.DESC );
	}
	
	public void close() {
		helper.close();
	}
	
	@Override
	public boolean hasBook(String fileName) {
		return helper.hasBook(fileName);
	}
	
	private byte[] resizeImage( byte[] input ) {
		
		if ( input == null ) {
			return null;
		}
				
		Bitmap bitmapOrg = BitmapFactory.decodeByteArray(input, 0, input.length);

		if ( bitmapOrg == null ) {
			return null;
		}
		
		int height = bitmapOrg.getHeight();
		int width = bitmapOrg.getWidth();
		int newHeight = THUMBNAIL_HEIGHT;

		float scaleHeight = ((float) newHeight) / height;

		// createa matrix for the manipulation
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleHeight, scaleHeight);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0,
				width, height, matrix, true);

		bitmapOrg.recycle();

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		resizedBitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos);            

		resizedBitmap.recycle();

		return bos.toByteArray();            

	}
	
}
