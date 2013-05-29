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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.google.inject.Inject;
import roboguice.inject.ContextSingleton;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@ContextSingleton
public class LibraryDatabaseHelper extends SQLiteOpenHelper {

    private static final String LIB_BOOKS_TABLE = "lib_books";
	
	public enum Field { 
		file_name("text primary key"), title("text"), a_first_name("text"),
		a_last_name("text"), date_added("integer"), date_last_read("integer"), 
		description("text"), cover_image("blob"), progress("integer");
		
		private String fieldDef;
	
		private Field(String fieldDef) { this.fieldDef = fieldDef; }		
	}

	public enum Order { ASC , DESC }
	
	private static final String DB_NAME = "PageTurnerLibrary";
	private static final int VERSION = 4;

	private static String getCreateTableString() {
		String create = "create table " + LIB_BOOKS_TABLE + " ( ";
		
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
		return getWritableDatabase();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(getCreateTableString());		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {		
		
		if ( oldVersion == 3 ) {
			db.execSQL("ALTER TABLE " + LIB_BOOKS_TABLE + " ADD COLUMN progress integer" );
		}
	}	
	
	public void delete( String fileName ) {
		
		String[] args = { fileName };
		
		getDataBase().delete(LIB_BOOKS_TABLE, Field.file_name + " = ?", args );
	}

	public void updateLastRead( String fileName, int progress ) {
		
		String whereClause = Field.file_name.toString() + " like ?";
		String[] args = { "%" + fileName };
		
		ContentValues content = new ContentValues();
		content.put( Field.date_last_read.toString(), new Date().getTime() );
		
		if ( progress != -1 ) {
			content.put(Field.progress.toString(), progress );
		}
		
		getDataBase().update(LIB_BOOKS_TABLE, content, whereClause, args);
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
			
		getDataBase().insert(LIB_BOOKS_TABLE, null, content);
	}
	
	public boolean hasBook( String fileName ) {
		Field[] fields = { Field.file_name };
		String[] args = { "%" + fileName };
		
		String whereClause = Field.file_name.toString() + " like ?";
		
		Cursor findBook = getDataBase().query( LIB_BOOKS_TABLE, fieldsAsString(fields), whereClause,
				args, null, null, null );
		
		boolean result =  findBook.getCount() != 0;
		findBook.close();
		
		return result;
	}

    private static String getFilterClause(String filter, String existingClause ) {

        if ( filter == null || filter.length() == 0 ) {
            return  existingClause;
        }

        String whereClause = "(" + Field.a_first_name + " like ? "
                + " or " + Field.a_last_name + " like ? "
                + " or " + Field.title + " like ? )";

        if ( existingClause != null ) {
            whereClause = whereClause + " and " + existingClause;
        }

        return whereClause;
    }

    private static String[] getFilterArgs( String[] existingArgs, String filter ) {
        if ( filter == null || filter.length() == 0 ) {
            return existingArgs;
        }
        
        String searchString =  "%" + filter + "%";

        String[] newArgs;

        if ( existingArgs != null ) {
            newArgs = new String[existingArgs.length + 3];

            //Move original arguments 3 positions to the right
            System.arraycopy(existingArgs, 0,newArgs, 3, existingArgs.length );
        } else  {
            newArgs = new String[3];
        }

        //And fill the first 3 with the filter-string.
        for ( int i=0; i < 3; i++ ) {
            newArgs[i] = searchString;
        }

        return newArgs;
    }
	
	public synchronized  KeyedQueryResult<LibraryBook> findByField( Field fieldName, String fieldValue,
			Field orderField, Order ordering, String filter) {

        String[] args = { fieldValue };
		String whereClause;
		
		if ( fieldValue == null ) {
			whereClause = fieldName.toString() + " is null";
			args = null;
		} else {
			whereClause = fieldName.toString() + " = ?";			
		}

        if ( filter != null ) {
            whereClause = getFilterClause(filter, whereClause);
            args = getFilterArgs(args, filter);
        }
		
		Cursor cursor = getDataBase().query(LIB_BOOKS_TABLE,
                fieldsAsString(Field.values()),
				whereClause, args, null, null,
				"LOWER(" + orderField + ") " + ordering  );		
		
		List<String> keys = getKeys(orderField, ordering);
		
		return new KeyedBookResult( cursor, keys );
	}

	public synchronized QueryResult<LibraryBook> findAllOrderedBy( Field fieldName, Order order, String filter ) {

        String whereClause = fieldName != null ? fieldName.toString() + " is not null" : null;
        String[] args = new String[0];

        if ( filter != null ) {
            whereClause = getFilterClause(filter, whereClause);
            args = getFilterArgs(args, filter);
        }
						
		Cursor cursor = getDataBase().query(LIB_BOOKS_TABLE,
                fieldsAsString(Field.values()),
				whereClause,args, null, null,
				fieldName != null ? "LOWER(" + fieldName.toString() + ") " + order.toString() : null );		
		
		return new LibraryBookResult(cursor);
	}	
	
	private List<String> getKeys( Field fieldName, Order order ) {
		String[] keyField = { fieldName.toString() };
		Cursor fieldCursor = getDataBase().query(LIB_BOOKS_TABLE,
                keyField, null,	new String[0], null, null,
				fieldName != null ? "LOWER(" + fieldName.toString() + ") " + order.toString() : null);
				
		List<String> keys = new ArrayList<String>();
		fieldCursor.moveToFirst();
		
		fieldCursor.moveToFirst();
		
		while( !fieldCursor.isAfterLast()) {
		     keys.add(fieldCursor.getString(0));
		     fieldCursor.moveToNext();
		}
		
		fieldCursor.close();
		
		return keys;
	}
	
	public synchronized KeyedQueryResult<LibraryBook> findAllKeyedBy(Field fieldName, Order order, String filter ) {
		
		List<String> keys = getKeys(fieldName, order);

        String whereClause = fieldName != null ? fieldName.toString() + " is not null" : null;
        String[] args = new String[0];

        if ( filter != null ) {
            whereClause = getFilterClause(filter, whereClause);
            args = getFilterArgs(args, filter);
        }


		Cursor cursor = getDataBase().query(LIB_BOOKS_TABLE,
                fieldsAsString(Field.values()),
			    whereClause, args, null, null,
				fieldName != null ? "LOWER(" + fieldName.toString() + ") " 
						+ order.toString() : null );		
		
		return new KeyedBookResult(cursor, keys);
	}	
	
	private class KeyedBookResult extends KeyedQueryResult<LibraryBook> {
		
		public KeyedBookResult(Cursor cursor, List<String> keys) {
			super(cursor, keys);
		}
		
		@Override
		public LibraryBook convertRow(Cursor cursor) {
			return doConvertRow(cursor);
		}
	}
	
	private class LibraryBookResult extends QueryResult<LibraryBook> {
		
		public LibraryBookResult(Cursor cursor) {
			super(cursor);
		}
		
		@Override
		public LibraryBook convertRow(Cursor cursor) {
			return doConvertRow(cursor);
		}
	}
	
	private static LibraryBook doConvertRow(Cursor cursor) {
		
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
		newBook.setCoverImage(coverData);			
		
		newBook.setFileName( cursor.getString(Field.file_name.ordinal()));
		
		newBook.setProgress(cursor.getInt(Field.progress.ordinal()));
		
		return newBook;
	}

	
	private static String[] fieldsAsString(Field[] values) {		
		
		String[] fieldsAsString = new String[values.length];
		for ( int i=0; i < values.length; i++ ) {
			fieldsAsString[i] = values[i].toString();
		}
		
		return fieldsAsString;
	}
	
}
