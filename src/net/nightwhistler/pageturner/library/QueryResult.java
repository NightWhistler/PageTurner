package net.nightwhistler.pageturner.library;

import android.database.Cursor;

public abstract class QueryResult<T> {

	private Cursor wrappedCursor;
	
	public QueryResult(Cursor cursor) {
		this.wrappedCursor = cursor;
		cursor.moveToFirst();
	}
	
	public int getSize() {
		return this.wrappedCursor.getCount();
	}
	
	public T getItemAt(int index) {		
		this.wrappedCursor.moveToPosition(index);		
		return convertRow(this.wrappedCursor);		
	}
	
	public boolean hasNext() {
		return ! this.wrappedCursor.isAfterLast();
	}
	
	public void close() {
		this.wrappedCursor.close();		
	}
	
	public abstract T convertRow( Cursor cursor );
}
