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

import java.util.Date;

import roboguice.inject.ContextScoped;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;

import com.google.inject.Inject;

@ContextScoped
public class LibraryDatabaseHelper extends SQLiteOpenHelper {
	
	public enum Field { 
		file_name("text primary key"),title("text"), a_first_name("text"),
		a_last_name("text"), date_added("integer"), date_last_read("integer"), 
		description("text"), cover_image("blob"), progress("integer");
		
		private String fieldDef;
	
		private Field(String fieldDef) { this.fieldDef = fieldDef; }		
	}	
	
	private SQLiteDatabase database;
	
	public enum Order { ASC, DESC };	
	
	private static final String DB_NAME = "PageTurnerLibrary";
	private static final int VERSION = 4;

	private static String getCreateTableString() {
		String create = "create table lib_books ( ";
		
		boolean first = true;
		
		for ( Field f: Field.values() ) {
			
			if ( first ) {
				first = false;
			} else {
				create += ",";
			}
			
			create += (" " + f.name() + " " + f.fieldDef);
		}
		
		create += " );";
		
		return create;
	}
	
	@Inject
	public LibraryDatabaseHelper(Context context) {
		super(context, DB_NAME, null, VERSION);		
	}
	
	private synchronized SQLiteDatabase getDataBase() {
		if ( this.database == null ) {
			this.database = getWritableDatabase();
		}
		
		return this.database;
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(getCreateTableString());		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
		
		if ( oldVersion == 3 ) {
			db.execSQL("ALTER TABLE lib_books ADD COLUMN progress integer" );
		}
	}	
	
	public void delete( String fileName ) {
		
		String[] args = { fileName };
		
		getDataBase().delete("lib_books", Field.file_name + " = ?", args );		
	}	
	
	public void close() {
		if ( this.database != null ) {
			database.close();
			this.database = null;
		}
	}
	
	public void updateLastRead( String fileName, int progress ) {
		
		String whereClause = Field.file_name.toString() + " like ?";
		String[] args = { "%" + fileName };
		
		ContentValues content = new ContentValues();
		content.put( Field.date_last_read.toString(), new Date().getTime() );
		
		if ( progress != -1 ) {
			content.put(Field.progress.toString(), progress );
		}
		
		getDataBase().update("lib_books", content, whereClause, args);		
	}	
	
	public void storeNewBook(String fileName, String authorFirstName,
			String authorLastName, String title, String description,
			byte[] coverImage, boolean setLastRead) {
		
				
		ContentValues content = new ContentValues();
				
		content.put(Field.title.toString(), title );
		content.put(Field.a_first_name.toString(), authorFirstName );
		content.put(Field.a_last_name.toString(), authorLastName );
		content.put(Field.cover_image.toString(), coverImage );
		content.put(Field.description.toString(), description );
				
		if ( setLastRead ) {
			content.put(Field.date_last_read.toString(), new Date().getTime() );
		}		
			
		content.put(Field.file_name.toString(), fileName );
		content.put(Field.date_added.toString(), new Date().getTime() );			
			
		getDataBase().insert("lib_books", null, content);
	}
	
	public boolean hasBook( String fileName ) {
		Field[] fields = { Field.file_name };
		String[] args = { "%" + fileName };
		
		String whereClause = Field.file_name.toString() + " like ?";
		
		Cursor findBook = getDataBase().query( "lib_books", fieldsAsString(fields), whereClause,
				args, null, null, null );
		
		boolean result =  findBook.getCount() != 0;
		findBook.close();
		
		return result;
	}	
	
	public QueryResult<LibraryBook> findByField( Field fieldName, String fieldValue,
			Field orderField, Order ordering) {
						
		String[] args = { fieldValue };
		String whereClause;
		
		if ( fieldValue == null ) {
			whereClause = fieldName.toString() + " is null";
			args = null;
		} else {
			whereClause = fieldName.toString() + " = ?";			
		}
		
		Cursor cursor = getDataBase().query("lib_books", fieldsAsString(Field.values()), 
				whereClause, args, null, null,
				orderField + " " + ordering  );		
		
		return new LibraryBookResult(cursor);
	}
	
	public QueryResult<LibraryBook> findAllOrderedBy( Field fieldName, Order order ) {
						
		Cursor cursor = getDataBase().query("lib_books", fieldsAsString(Field.values()), 
				fieldName != null ? fieldName.toString() + " is not null" : null,
			    new String[0], null, null,
				fieldName != null ? fieldName.toString() + " " + order.toString() : null );		
		
		return new LibraryBookResult(cursor);
	}	
	
	private class LibraryBookResult extends QueryResult<LibraryBook> {
		
		public LibraryBookResult(Cursor cursor) {
			super(cursor);
		}
		
		@Override
		public LibraryBook convertRow(Cursor cursor) {
			
			LibraryBook newBook = new LibraryBook();
			
			newBook.setAuthor(new Author( 
					cursor.getString(Field.a_first_name.ordinal()), 
					cursor.getString(Field.a_last_name.ordinal())));
			
			newBook.setTitle( cursor.getString(Field.title.ordinal()));
			
			newBook.setDescription(cursor.getString(Field.description.ordinal()));
			
			try {
				newBook.setAddedToLibrary(new Date(cursor.getLong(Field.date_added.ordinal())));
			} catch (RuntimeException r){}
			
			try {
				newBook.setLastRead(new Date(cursor.getLong(Field.date_last_read.ordinal())));
			} catch (RuntimeException r){}
			
			byte[] coverData = cursor.getBlob(Field.cover_image.ordinal());
			
			if ( coverData != null ) {			
				newBook.setCoverImage( BitmapFactory.decodeByteArray(coverData, 0, coverData.length ) );
			}
			
			newBook.setFileName( cursor.getString(Field.file_name.ordinal()));
			
			newBook.setProgress(cursor.getInt(Field.progress.ordinal()));
			
			return newBook;
		}
	}	
	
	//public void createOrUpdateBook( 
	
	private static String[] fieldsAsString(Field[] values) {		
		
		String[] fieldsAsString = new String[values.length];
		for ( int i=0; i < values.length; i++ ) {
			fieldsAsString[i] = values[i].toString();
		}
		
		return fieldsAsString;
	}
	
}
