/*
 * Copyright (C) 2011 Alex Kuiper
 * 
 * This file is part of PageTurner
 *
 * PageTurner is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PageTurner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PageTurner.  If not, see <http://www.gnu.org/licenses/>.*
 */
package net.nightwhistler.pageturner.library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

public class SqlLiteLibraryService implements LibraryService {
	
	private static final int THUMBNAIL_HEIGHT = 250;
	
	LibraryDatabaseHelper helper;
	
	public SqlLiteLibraryService(Context context) {
		this.helper = new LibraryDatabaseHelper(context);
	}		
	
	
	@Override
	public void storeBook(String fileName, Book book, boolean updateLastRead) {
		
		boolean hasBook = hasBook(fileName);
		
		if ( hasBook && !updateLastRead ) {
			return;
		} else if ( hasBook ) {
			helper.updateLastRead(fileName);
			return;
		}
				
		Metadata metaData = book.getMetadata();
    	
    	String authorFirstName = "Unknown author";
    	String authorLastName = "";
    	
    	if ( metaData.getAuthors().size() > 0 ) {
    		authorFirstName = metaData.getAuthors().get(0).getFirstname();
    		authorLastName = metaData.getAuthors().get(0).getLastname();
    	}
    	
    	byte[] thumbNail = null;
    	
    	try {
    		if ( book.getCoverImage() != null ) {    			
    			thumbNail = resizeImage(book.getCoverImage().getData());
    			book.getCoverImage().close();
    		}
    	} catch (IOException io) {
    		
    	}    	
		
    	String description = "";
    	
    	if ( ! metaData.getDescriptions().isEmpty() ) {
    		description = metaData.getDescriptions().get(0);
    	}
    	
		this.helper.storeNewBook(fileName, authorFirstName, 
				authorLastName, book.getTitle(), description, 
				thumbNail, updateLastRead);    	
		
	}
	
	@Override
	public QueryResult<LibraryBook> findUnread() {
		return helper.findByField( LibraryDatabaseHelper.Field.date_last_read,
				null);
				
	}	
	
	@Override
	public QueryResult<LibraryBook> findAllByLastRead() {		
		return helper.findAllOrderedBy(
				LibraryDatabaseHelper.Field.date_last_read,
				LibraryDatabaseHelper.Order.DESC );
	}
	
	@Override
	public QueryResult<LibraryBook> findAllByAuthor() {
		return helper.findAllOrderedBy(
				LibraryDatabaseHelper.Field.a_last_name,
				LibraryDatabaseHelper.Order.ASC );
	
	}
	
	@Override
	public QueryResult<LibraryBook> findAllByLastAdded() {
		return helper.findAllOrderedBy(
				LibraryDatabaseHelper.Field.date_added,
				LibraryDatabaseHelper.Order.DESC );	
	}
	
	@Override
	public QueryResult<LibraryBook> findAllByTitle() {
		return helper.findAllOrderedBy(
				LibraryDatabaseHelper.Field.title,
				LibraryDatabaseHelper.Order.ASC );	
	}
	
	public void close() {
		helper.close();
	}
	
	@Override
	public void deleteBook(String fileName) {
		this.helper.delete( fileName );		
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
