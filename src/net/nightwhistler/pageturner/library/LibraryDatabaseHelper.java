package net.nightwhistler.pageturner.library;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LibraryDatabaseHelper extends SQLiteOpenHelper {
	
	public enum Field { file_name, title, a_first_name, a_last_name,
		date_added, date_last_read, cover_image
	}	
	
	private SQLiteDatabase database;
	
	public enum Order { ASC, DESC };
	
	private static final String CREATE_TABLE =
		"create table lib_books ( file_name text primary key, title text, " +
		"a_first_name text, a_last_name text, date_added integer, " +
		"date_last_read integer, cover_image blob );";
	
	private static final String DB_NAME = "PageTurnerLibrary";
	private static final int VERSION = 1;

	
	public LibraryDatabaseHelper(Context context) {
		super(context, DB_NAME, null, VERSION);
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TABLE);		
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		//Nothing to do yet :)		
	}
	
	private SQLiteDatabase getDataBase() {
		if ( this.database == null ) {
			this.database = getReadableDatabase();
		}
		
		return this.database;
	}
	
	public void close() {
		if ( this.database != null ) {
			database.close();
			this.database = null;
		}
	}
	
	public void store(String fileName, String authorFirstName,
			String authorLastName, String title, byte[] coverImage) {
		
		SQLiteDatabase db = getWritableDatabase();
		
		Field[] fields = { Field.file_name };
		String[] args = { fileName };
		
		String whereClause = Field.file_name.toString() + " = ?";
		
		Cursor findBook = db.query( "lib_books", fieldsAsString(fields), whereClause,
				args, null, null, null );
		
		ContentValues content = new ContentValues();
				
		content.put(Field.title.toString(), title );
		content.put(Field.a_first_name.toString(), authorFirstName );
		content.put(Field.a_last_name.toString(), authorLastName );
		content.put(Field.cover_image.toString(), coverImage );
		content.put(Field.date_last_read.toString(), new Date().getTime() );
		
		if ( findBook.getCount() == 0 ) {
			//create			
			
			content.put(Field.file_name.toString(), fileName );
			content.put(Field.date_added.toString(), new Date().getTime() );			
			
			db.insert("lib_books", null, content);
		} else {								
			//update
			db.update("lib_books", content, whereClause, args);
		}	
		
		findBook.close();
		db.close();		
	}
	
	public QueryResult<LibraryBook> findAllOrderedBy( Field fieldName, Order order ) {
		
		SQLiteDatabase db = this.getDataBase();
		
		Cursor cursor = db.query("lib_books", fieldsAsString(Field.values()), 
				null, new String[0], null, null,
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
			
			try {
				newBook.setAddedToLibrary(new Date(cursor.getLong(Field.date_added.ordinal())));
			} catch (RuntimeException r){}
			
			try {
				newBook.setLastRead(new Date(cursor.getLong(Field.date_last_read.ordinal())));
			} catch (RuntimeException r){}
			
			newBook.setCoverImage( cursor.getBlob(Field.cover_image.ordinal() ) );			
			newBook.setFileName( cursor.getString(Field.file_name.ordinal()));
			
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
