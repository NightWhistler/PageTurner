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

import android.database.Cursor;

/**
 * A QueryResult maps a Cursor and performs
 * basic O/R mapping.
 * 
 * Subclasses should implement the actual mapping
 * from a row to an object by overriding convertRow()
 * 
 * @author Alex Kuiper
 *
 * @param <T> the type to map to.
 */
public abstract class QueryResult<T> {

	private Cursor wrappedCursor;
	
	private int limit = -1;
	
	public QueryResult(Cursor cursor) {
		this.wrappedCursor = cursor;
		cursor.moveToFirst();
	}
	
	public void setLimit(int limit) {
		this.limit = limit;
	}
	
	public int getSize() {
		
		int count = this.wrappedCursor.getCount(); 
		
		if ( limit != -1 && count > limit ) {
			return limit;
		}
		
		return count;
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
